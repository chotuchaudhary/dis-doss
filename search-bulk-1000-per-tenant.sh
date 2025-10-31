#!/bin/bash

# Script to send 1000 search queries per tenant for full text search testing
# Tests search functionality across all tenants (tenant1 to tenant6)

BASE_URL="http://localhost:8080/api/v1/search"

# Tenants to search
declare -a tenants=("tenant1" "tenant2")

# Document types
declare -a doc_types=("product" "article" "manual" "review" "faq" "tutorial")

# Search query keywords pool
declare -a keywords=(
    "sample" "document" "product" "article" "manual" "review" "faq" "tutorial"
    "premium" "quality" "technology" "insights" "guide" "evaluation" "questions" "step"
    "electronics" "gadgets" "tech" "office" "software" "hardware" "gaming" "business"
    "comprehensive" "detailed" "expert" "analysis" "instructions" "tips" "answers" "guide"
    "advanced" "modern" "professional" "personal" "excellence" "design" "trends" "developments"
    "high-quality" "latest" "current" "industry" "troubleshooting" "best" "practices" "skills"
    "information" "content" "data" "knowledge" "experience" "performance" "quality" "value"
    "perfect" "designed" "features" "analysis" "solutions" "examples" "techniques" "methods"
)

# Generate random search query
generate_search_query() {
    local num_terms=$((RANDOM % 3 + 1))  # 1-3 keywords
    local query=""
    
    for ((i=0; i<num_terms; i++)); do
        local keyword_idx=$((RANDOM % ${#keywords[@]}))
        if [ -z "$query" ]; then
            query="${keywords[$keyword_idx]}"
        else
            query="$query ${keywords[$keyword_idx]}"
        fi
    done
    
    echo "$query"
}

echo "=========================================="
echo "Bulk Search Query Test - 1000 queries per tenant"
echo "=========================================="
echo "Tenants: ${tenants[*]}"
echo "Document Types: ${doc_types[*]}"
echo "Queries per tenant: 1000"
echo "Total queries: $(( ${#tenants[@]} * 1000 ))"
echo "=========================================="
echo ""

# Configuration
queries_per_tenant=1000
queries_per_type=$((queries_per_tenant / ${#doc_types[@]}))
remainder=$((queries_per_tenant % ${#doc_types[@]}))

echo "Configuration:"
echo "  Queries per tenant: $queries_per_tenant"
echo "  Queries per document type: $queries_per_type (base)"
echo "  Remainder queries: $remainder (will be distributed)"
echo "  Base URL: $BASE_URL"
echo ""
echo "Starting bulk search queries..."
echo ""

start_time=$(date +%s)
total_success=0
total_failed=0
total_results=0
query_count=0

# Track performance
declare -a response_times=()

for tenant in "${tenants[@]}"; do
    tenant_queries=0
    tenant_success=0
    tenant_failed=0
    tenant_results=0
    tenant_start_time=$(date +%s)
    
    echo "=========================================="
    echo "Processing Tenant: $tenant"
    echo "Target: $queries_per_tenant queries"
    echo "=========================================="
    
    # Process each document type
    doc_type_index=0
    for doc_type in "${doc_types[@]}"; do
        type_queries=0
        type_start_time=$(date +%s)
        
        # Distribute remainder queries across first few document types
        if [ $doc_type_index -lt $remainder ]; then
            type_query_count=$((queries_per_type + 1))
        else
            type_query_count=$queries_per_type
        fi
        
        for ((i=1; i<=type_query_count; i++)); do
            query=$(generate_search_query)
            
            # Build search URL (proper URL encoding)
            if command -v python3 &> /dev/null; then
                encoded_query=$(python3 -c "import urllib.parse; print(urllib.parse.quote('$query'))")
            elif command -v node &> /dev/null; then
                encoded_query=$(node -e "console.log(encodeURIComponent('$query'))")
            else
                # Fallback to basic encoding
                encoded_query=$(echo "$query" | sed 's/ /%20/g' | sed 's/&/%26/g' | sed 's/+/%2B/g')
            fi
            search_url="${BASE_URL}?query=${encoded_query}&documentType=${doc_type}&page=0&size=10"
            
            # Execute search query with timeout
            # Use curl's built-in timing (works on both Linux and macOS)
            response=$(curl -s -w "\n%{http_code}\n%{time_total}" -X GET "$search_url" \
                -H "Content-Type: application/json" \
                -H "X-Tenant-ID: ${tenant}" \
                --max-time 30 \
                --connect-timeout 5 2>&1)
            
            # Extract components: response body, http_code, time_total
            # Last line is time_total, second to last is http_code
            time_total=$(echo "$response" | tail -n1)
            http_code=$(echo "$response" | tail -n2 | head -n1)
            response_body=$(echo "$response" | sed '$d' | sed '$d')
            
            # Convert time_total (seconds) to milliseconds
            if [[ "$time_total" =~ ^[0-9]+\.?[0-9]*$ ]]; then
                query_time_ms=$(awk "BEGIN {printf \"%.0f\", $time_total * 1000}")
            else
                query_time_ms=0
            fi
            
            # Check if curl failed (no HTTP code or error message)
            if [[ ! "$http_code" =~ ^[0-9]{3}$ ]] || echo "$response" | grep -q "curl:"; then
                http_code="000"
                response_body=""
            fi
            
            response_times+=($query_time_ms)
            
            query_count=$((query_count + 1))
            tenant_queries=$((tenant_queries + 1))
            type_queries=$((type_queries + 1))
            
            if [ "$http_code" = "200" ]; then
                # Parse results count from JSON response
                results_count=$(echo "$response_body" | grep -o '"total":[0-9]*' | grep -o '[0-9]*' | head -1)
                if [ -z "$results_count" ]; then
                    results_count=0
                fi
                
                total_success=$((total_success + 1))
                tenant_success=$((tenant_success + 1))
                total_results=$((total_results + results_count))
                tenant_results=$((tenant_results + results_count))
            else
                total_failed=$((total_failed + 1))
                tenant_failed=$((tenant_failed + 1))
                
                # Log first few failures for debugging
                if [ $total_failed -le 5 ]; then
                    echo "    ⚠ Query failed (HTTP $http_code): $query" >&2
                fi
            fi
            
            # Progress update every 100 queries
            if (( query_count % 100 == 0 )); then
                echo "  Progress: $query_count/$(( ${#tenants[@]} * queries_per_tenant )) queries total (${total_success} success, ${total_failed} failed)"
            fi
            
            # Small delay to avoid overwhelming the server
            sleep 0.01
        done
        
        type_end_time=$(date +%s)
        type_duration=$((type_end_time - type_start_time))
        echo "  ✓ Completed $type_queries queries for $doc_type (took ${type_duration}s)"
        
        doc_type_index=$((doc_type_index + 1))
    done
    
    tenant_end_time=$(date +%s)
    tenant_duration=$((tenant_end_time - tenant_start_time))
    
    echo ""
    echo "✓ Completed $tenant_queries queries for $tenant (took ${tenant_duration}s)"
    echo "  - Success: $tenant_success"
    echo "  - Failed: $tenant_failed"
    echo "  - Total results found: $tenant_results"
    if [ $tenant_success -gt 0 ]; then
        avg_tenant_results=$(awk "BEGIN {printf \"%.2f\", $tenant_results / $tenant_success}")
        echo "  - Average results per query: $avg_tenant_results"
    fi
    echo ""
    
    # Small delay between tenants
    sleep 0.5
done

end_time=$(date +%s)
duration=$((end_time - start_time))

# Calculate statistics
if [ $total_success -gt 0 ]; then
    avg_results=$((total_results / total_success))
else
    avg_results=0
fi

# Calculate average response time
if [ ${#response_times[@]} -gt 0 ]; then
    total_response_time=0
    for time in "${response_times[@]}"; do
        total_response_time=$((total_response_time + time))
    done
    avg_response_time=$((total_response_time / ${#response_times[@]}))
else
    avg_response_time=0
fi

# Find min and max response times
if [ ${#response_times[@]} -gt 0 ]; then
    min_time=${response_times[0]}
    max_time=${response_times[0]}
    for time in "${response_times[@]}"; do
        if [ $time -lt $min_time ]; then
            min_time=$time
        fi
        if [ $time -gt $max_time ]; then
            max_time=$time
        fi
    done
else
    min_time=0
    max_time=0
fi

echo "=========================================="
echo "Search Query Test Complete!"
echo "=========================================="
echo "Total queries executed: $query_count"
echo "  - Successful: $total_success"
echo "  - Failed: $total_failed"
if [ $query_count -gt 0 ]; then
    success_rate=$(awk "BEGIN {printf \"%.2f\", ($total_success * 100) / $query_count}")
    echo "  - Success rate: ${success_rate}%"
fi
echo ""
echo "Results:"
echo "  - Total documents found: $total_results"
if [ $total_success -gt 0 ] && [ $total_results -gt 0 ]; then
    avg_results_per_query=$(awk "BEGIN {printf \"%.2f\", $total_results / $total_success}")
    echo "  - Average results per query: $avg_results_per_query"
fi
echo ""
echo "Performance:"
echo "  - Total time: ${duration} seconds"
if [ $duration -gt 0 ]; then
    qps=$(awk "BEGIN {printf \"%.2f\", $query_count / $duration}")
    echo "  - Queries per second: $qps"
fi
echo "  - Average response time: ${avg_response_time}ms"
echo "  - Min response time: ${min_time}ms"
echo "  - Max response time: ${max_time}ms"
echo ""
echo "Per Tenant Summary:"
echo "  - Tenants tested: ${#tenants[@]} (${tenants[*]})"
echo "  - Queries per tenant: $queries_per_tenant"
echo "  - Document types: ${doc_types[*]}"
echo "=========================================="

