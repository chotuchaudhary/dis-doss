#!/bin/bash

# Script to read CSV file and ingest data into Elasticsearch using multi-threading
# Each row is randomly assigned to one of the six tenants (tenant1-tenant6)

# Configuration
CSV_FILE="${1:-/Users/user/Downloads/data.csv}"
BASE_URL="${2:-http://localhost:8080}"
THREAD_COUNT="${3:-10}"
API_ENDPOINT="${BASE_URL}/api/v1/documents/orders/"

# Tenants array
declare -a tenants=("tenant1" "tenant2" "tenant3" "tenant4" "tenant5" "tenant6")

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if CSV file exists
if [ ! -f "$CSV_FILE" ]; then
    echo -e "${RED}Error: CSV file not found: $CSV_FILE${NC}"
    echo "Usage: $0 <csv_file_path> [base_url] [thread_count]"
    exit 1
fi

echo "=========================================="
echo "CSV Data Ingestion Script (Multi-threaded)"
echo "=========================================="
echo "CSV File: $CSV_FILE"
echo "API Endpoint: $API_ENDPOINT"
echo "Thread Count: $THREAD_COUNT"
echo "Tenants: ${tenants[*]}"
echo "=========================================="
echo ""

# Check if file is readable
if [ ! -r "$CSV_FILE" ]; then
    echo -e "${RED}Error: Cannot read file: $CSV_FILE${NC}"
    exit 1
fi

# Count total lines (excluding header)
total_lines=$(tail -n +2 "$CSV_FILE" | wc -l | tr -d ' ')
echo "Total rows to ingest: $total_lines"
echo ""

# Create temporary directory for counters
TEMP_DIR=$(mktemp -d)
SUCCESS_FILE="${TEMP_DIR}/success"
FAILED_FILE="${TEMP_DIR}/failed"
PROCESSED_FILE="${TEMP_DIR}/processed"
LOG_FILE="${TEMP_DIR}/log"

# Initialize counter files
echo "0" > "$SUCCESS_FILE"
echo "0" > "$FAILED_FILE"
echo "0" > "$PROCESSED_FILE"
> "$LOG_FILE"

# Create a lock file for thread-safe counter updates
LOCK_FILE="${TEMP_DIR}/lock"

# Thread-safe counter increment function
increment_counter() {
    local counter_file="$1"
    while ! ln "$counter_file" "${counter_file}.lock" 2>/dev/null; do
        sleep 0.001
    done
    local value=$(cat "$counter_file")
    echo $((value + 1)) > "$counter_file"
    rm "${counter_file}.lock"
}

# Thread-safe log function
log_message() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" >> "$LOG_FILE"
}

start_time=$(date +%s)

# Create a Python helper script for CSV parsing
PYTHON_SCRIPT=$(cat <<'PYEOF'
import csv
import sys
import json

for line in sys.stdin:
    line = line.rstrip('\n\r')
    if not line:
        continue
    try:
        reader = csv.reader([line])
        row = next(reader)
        # Pad with empty strings if less than 8 fields
        while len(row) < 8:
            row.append('')
        # Output as tab-separated values for safe shell parsing
        print('\t'.join(row[:8]))
    except Exception as e:
        # Output error indicator
        print(f"ERROR: {str(e)}", file=sys.stderr)
        sys.exit(1)
PYEOF
)

# Function to process a single row
process_row() {
    local line="$1"
    local line_number="$2"
    
    # Skip empty lines
    if [ -z "$line" ]; then
        return 0
    fi
    
    # Parse CSV line using Python (handles quoted fields properly)
    local csv_fields=""
    if command -v python3 &> /dev/null; then
        csv_fields=$(echo "$line" | python3 -c "$PYTHON_SCRIPT" 2>/dev/null)
        local parse_status=$?
        if [ $parse_status -ne 0 ] || [ -z "$csv_fields" ]; then
            log_message "Warning: Skipping row $line_number - parse error"
            increment_counter "$FAILED_FILE"
            return 1
        fi
        
        # Split by tab (handle empty fields)
        IFS=$'\t' read -r InvoiceNo StockCode Description Quantity InvoiceDate UnitPrice CustomerID Count <<< "$csv_fields"
    else
        # Fallback: simple CSV parsing (assumes no commas in quoted fields)
        IFS=',' read -r InvoiceNo StockCode Description Quantity InvoiceDate UnitPrice CustomerID Count <<< "$line"
    fi
    
    # Ensure all fields are set (even if empty)
    InvoiceNo="${InvoiceNo:-}"
    StockCode="${StockCode:-}"
    Description="${Description:-}"
    Quantity="${Quantity:-0}"
    InvoiceDate="${InvoiceDate:-}"
    UnitPrice="${UnitPrice:-0}"
    CustomerID="${CustomerID:-}"
    Count="${Count:-0}"
    
    # Skip if InvoiceNo is empty
    if [ -z "$InvoiceNo" ]; then
        return 0
    fi
    
    # Randomly select a tenant
    local tenant_index=$((RANDOM % ${#tenants[@]}))
    local selected_tenant="${tenants[$tenant_index]}"
    
    # Clean up fields (remove quotes, trim whitespace)
    InvoiceNo=$(echo "$InvoiceNo" | sed 's/^"//;s/"$//' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
    StockCode=$(echo "$StockCode" | sed 's/^"//;s/"$//' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
    Description=$(echo "$Description" | sed 's/^"//;s/"$//' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
    Quantity=$(echo "$Quantity" | sed 's/^"//;s/"$//' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
    InvoiceDate=$(echo "$InvoiceDate" | sed 's/^"//;s/"$//' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
    UnitPrice=$(echo "$UnitPrice" | sed 's/^"//;s/"$//' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
    CustomerID=$(echo "$CustomerID" | sed 's/^"//;s/"$//' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
    Count=$(echo "$Count" | sed 's/^"//;s/"$//' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
    
    # Generate UUID for each document
    local document_uuid=""
    
    # Try uuidgen first
    if command -v uuidgen &> /dev/null; then
        document_uuid=$(uuidgen 2>/dev/null | tr -d '\n' | tr -d '\r')
    fi
    
    # Try Python uuid if uuidgen didn't work
    if [ -z "$document_uuid" ] && command -v python3 &> /dev/null; then
        document_uuid=$(python3 -c "import uuid; print(uuid.uuid4())" 2>/dev/null | tr -d '\n' | tr -d '\r')
    fi
    
    # Fallback: generate pseudo-UUID
    if [ -z "$document_uuid" ]; then
        local timestamp=$(date +%s 2>/dev/null || echo "0")
        local random1=${RANDOM}
        local random2=${RANDOM}
        local random3=${RANDOM}
        
        # Try to use hash command (available on macOS)
        if command -v shasum &> /dev/null; then
            local hash_input="${timestamp}-${random1}-${random2}-${random3}-${line_number}"
            local hash_output=$(echo -n "$hash_input" | shasum -a 256 | cut -d' ' -f1 | head -c 32)
            # Format as UUID: 8-4-4-4-12
            document_uuid="${hash_output:0:8}-${hash_output:8:4}-${hash_output:12:4}-${hash_output:16:4}-${hash_output:20:12}"
        elif command -v sha1sum &> /dev/null; then
            local hash_input="${timestamp}-${random1}-${random2}-${random3}-${line_number}"
            local hash_output=$(echo -n "$hash_input" | sha1sum | cut -d' ' -f1 | head -c 32)
            document_uuid="${hash_output:0:8}-${hash_output:8:4}-${hash_output:12:4}-${hash_output:16:4}-${hash_output:20:12}"
        else
            # Last resort: simple format
            document_uuid="doc-${timestamp}-${random1}-${random2}-${line_number}"
        fi
    fi
    
    # Ensure UUID is not empty
    if [ -z "$document_uuid" ]; then
        document_uuid="doc-$(date +%s)-${line_number}-${RANDOM}"
    fi
    
    # Build document endpoint with UUID
    local document_endpoint="${API_ENDPOINT}${document_uuid}"
    
    # Build JSON document using jq if available, otherwise manual escaping
    local json_document=""
    if command -v jq &> /dev/null; then
        json_document=$(jq -n \
            --arg inv "$InvoiceNo" \
            --arg stock "$StockCode" \
            --arg desc "$Description" \
            --arg qty "$Quantity" \
            --arg date "$InvoiceDate" \
            --arg price "$UnitPrice" \
            --arg cust "$CustomerID" \
            --arg cnt "$Count" \
            '{invoiceNo: $inv, stockCode: $stock, description: $desc, quantity: ($qty | tonumber? // 0), invoiceDate: $date, unitPrice: ($price | tonumber? // 0), customerID: $cust, count: ($cnt | tonumber? // 0)}')
    else
        # Manual JSON escaping (escape quotes, backslashes, newlines, etc.)
        local Description_escaped=$(echo "$Description" | sed 's/\\/\\\\/g' | sed 's/"/\\"/g' | sed 's/\n/\\n/g' | sed 's/\r//g' | sed 's/\t/\\t/g')
        local InvoiceNo_escaped=$(echo "$InvoiceNo" | sed 's/\\/\\\\/g' | sed 's/"/\\"/g')
        local StockCode_escaped=$(echo "$StockCode" | sed 's/\\/\\\\/g' | sed 's/"/\\"/g')
        local InvoiceDate_escaped=$(echo "$InvoiceDate" | sed 's/\\/\\\\/g' | sed 's/"/\\"/g')
        local CustomerID_escaped=$(echo "$CustomerID" | sed 's/\\/\\\\/g' | sed 's/"/\\"/g')
        
        # Ensure numeric fields are valid (handle empty or non-numeric)
        Quantity=${Quantity:-0}
        UnitPrice=${UnitPrice:-0}
        Count=${Count:-0}
        
        # Convert to numeric if possible, otherwise use 0
        if ! [[ "$Quantity" =~ ^-?[0-9]+\.?[0-9]*$ ]]; then
            Quantity=0
        fi
        if ! [[ "$UnitPrice" =~ ^-?[0-9]+\.?[0-9]*$ ]]; then
            UnitPrice=0
        fi
        if ! [[ "$Count" =~ ^-?[0-9]+\.?[0-9]*$ ]]; then
            Count=0
        fi
        
        json_document=$(cat <<EOF
{
  "invoiceNo": "$InvoiceNo_escaped",
  "stockCode": "$StockCode_escaped",
  "description": "$Description_escaped",
  "quantity": $Quantity,
  "invoiceDate": "$InvoiceDate_escaped",
  "unitPrice": $UnitPrice,
  "customerID": "$CustomerID_escaped",
  "count": $Count
}
EOF
)
    fi
    
    # Validate document_uuid is set
    if [ -z "$document_uuid" ]; then
        log_message "Failed to generate UUID for row $line_number"
        increment_counter "$FAILED_FILE"
        return 1
    fi
    
    # Ingest document via API with UUID in endpoint
    local response=$(curl -s -w "\n%{http_code}" -X POST "${document_endpoint}" \
        -H "Content-Type: application/json" \
        -H "X-Tenant-ID: ${selected_tenant}" \
        -d "$json_document" \
        --max-time 10 \
        --connect-timeout 5 \
        2>&1)
    
    # Extract HTTP status code (last line)
    local http_code=$(echo "$response" | tail -n1 | tr -d '\n' | tr -d '\r')
    
    # Check if successful
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        increment_counter "$SUCCESS_FILE"
        increment_counter "$PROCESSED_FILE"
    else
        increment_counter "$FAILED_FILE"
        increment_counter "$PROCESSED_FILE"
        local processed=$(cat "$PROCESSED_FILE")
        local failed=$(cat "$FAILED_FILE")
        # Only log first few failures
        if [ $failed -le 5 ]; then
            local response_body=$(echo "$response" | sed '$d' | head -c 200)
            log_message "Failed to ingest row $line_number (HTTP ${http_code:-000}): UUID=${document_uuid}, InvoiceNo=${InvoiceNo}"
        fi
    fi
    
    return 0
}

# Function to update progress (runs in background)
update_progress() {
    while true; do
        sleep 2
        local processed=$(cat "$PROCESSED_FILE")
        local success=$(cat "$SUCCESS_FILE")
        local failed=$(cat "$FAILED_FILE")
        if [ $processed -gt 0 ]; then
            echo -e "\r${GREEN}✓${NC} Processed: $processed/$total_lines (Success: $success, Failed: $failed)" | tr -d '\n'
        fi
    done
}

# Start progress updater in background
update_progress &
PROGRESS_PID=$!

# Cleanup function
cleanup() {
    kill $PROGRESS_PID 2>/dev/null
    wait $PROGRESS_PID 2>/dev/null
    rm -rf "$TEMP_DIR"
}
trap cleanup EXIT INT TERM

# Read CSV file and queue rows for processing
current_line=0
active_jobs=0
line_number=0

while IFS= read -r line || [ -n "$line" ]; do
    current_line=$((current_line + 1))
    
    # Skip header
    if [ $current_line -eq 1 ]; then
        continue
    fi
    
    line_number=$((line_number + 1))
    
    # Wait if we've reached the thread limit
    while [ $active_jobs -ge $THREAD_COUNT ]; do
        wait -n
        active_jobs=$((active_jobs - 1))
    done
    
    # Process row in background
    (process_row "$line" "$line_number") &
    active_jobs=$((active_jobs + 1))
    
done < "$CSV_FILE"

# Wait for all remaining background jobs to complete
wait

# Stop progress updater
kill $PROGRESS_PID 2>/dev/null
wait $PROGRESS_PID 2>/dev/null
echo "" # New line after progress

end_time=$(date +%s)
duration=$((end_time - start_time))

success_count=$(cat "$SUCCESS_FILE")
failed_count=$(cat "$FAILED_FILE")
processed_count=$(cat "$PROCESSED_FILE")

echo ""
echo "=========================================="
echo "Ingestion Complete!"
echo "=========================================="
echo "Total rows processed: $processed_count"
echo "Successful: $success_count"
echo "Failed: $failed_count"
echo "Duration: ${duration} seconds"
if [ $duration -gt 0 ]; then
    throughput=$(awk "BEGIN {printf \"%.2f\", $processed_count / $duration}")
    echo "Throughput: $throughput rows/second"
fi
echo "Threads used: $THREAD_COUNT"
echo "=========================================="

# Cleanup
rm -rf "$TEMP_DIR"
