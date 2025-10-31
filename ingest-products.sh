#!/bin/bash

# Script to ingest 100 products for each tenant

BASE_URL="http://localhost:8080/api/v1/documents"
TENANT1="tenant1"
TENANT2="tenant2"

# Product categories and tags for variety
declare -a categories=("electronics" "computers" "gadgets" "accessories" "wearables")
declare -a tags=("electronics" "computers" "laptops" "phones" "headphones" "keyboards" "mice" "displays" "storage" "networking")

echo "Starting product ingestion..."
echo ""

# Ingest 100 products for tenant1
echo "Ingesting 10000 products for $TENANT1..."
for i in {1..10000}; do
    product_id="product-$TENANT1-$(printf "%03d" $i)"
    category=${categories[$((RANDOM % ${#categories[@]}))]}
    tag1=${tags[$((RANDOM % ${#tags[@]}))]}
    tag2=${tags[$((RANDOM % ${#tags[@]}))]}
    price=$(awk "BEGIN {printf \"%.2f\", ($RANDOM % 2000) + 10}")
    
    response=$(curl -s -X POST "$BASE_URL" \
        -H "Content-Type: application/json" \
        -H "X-Tenant-ID: $TENANT1" \
        -d "{
            \"documentId\": \"$product_id\",
            \"title\": \"Product $i - $category\",
            \"content\": \"High-quality $category product $i with excellent features and performance. Perfect for modern users.\",
            \"category\": \"product\",
            \"metadata\": {
                \"name\": \"Product $i - $category\",
                \"description\": \"High-quality $category product $i with excellent features and performance. Perfect for modern users.\",
                \"price\": \"$price\",
                \"tag\": \"$tag1,$tag2,$category\"
            }
        }")
    
    if [ $((i % 10)) -eq 0 ]; then
        echo "  Processed $i products for $TENANT1..."
    fi
done

echo "Completed 100 products for $TENANT1"
echo ""

# Ingest 100 products for tenant2
echo "Ingesting 100 products for $TENANT2..."
for i in {1..10000}; do
    product_id="product-$TENANT2-$(printf "%03d" $i)"
    category=${categories[$((RANDOM % ${#categories[@]}))]}
    tag1=${tags[$((RANDOM % ${#tags[@]}))]}
    tag2=${tags[$((RANDOM % ${#tags[@]}))]}
    price=$(awk "BEGIN {printf \"%.2f\", ($RANDOM % 2000) + 10}")
    
    response=$(curl -s -X POST "$BASE_URL" \
        -H "Content-Type: application/json" \
        -H "X-Tenant-ID: $TENANT2" \
        -d "{
            \"documentId\": \"$product_id\",
            \"title\": \"Product $i - $category\",
            \"content\": \"Premium $category product $i with advanced features and cutting-edge technology.\",
            \"category\": \"product\",
            \"metadata\": {
                \"name\": \"Product $i - $category\",
                \"description\": \"Premium $category product $i with advanced features and cutting-edge technology.\",
                \"price\": \"$price\",
                \"tag\": \"$tag1,$tag2,$category\"
            }
        }")
    
    if [ $((i % 10)) -eq 0 ]; then
        echo "  Processed $i products for $TENANT2..."
    fi
done

echo "Completed 100 products for $TENANT2"
echo ""
echo "=========================================="
echo "Successfully ingested 200 products total:"
echo "  - 100 products for $TENANT1"
echo "  - 100 products for $TENANT2"
echo "=========================================="


