#!/bin/bash

# Test preview-code endpoint
# Usage: ./test-preview-code.sh <access_token>

TOKEN="${1:-YOUR_TOKEN_HERE}"

echo "Testing GET /api/v1/classes/preview-code"
echo "=========================================="
echo ""

# Test 1: Valid request
echo "Test 1: Valid request"
curl -X GET "http://localhost:8080/api/v1/classes/preview-code?branchId=1&courseId=1&startDate=2025-01-15" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json" \
  -s | jq .

echo ""
echo "----------------------------------------"
echo ""

# Test 2: Missing parameters
echo "Test 2: Missing branchId"
curl -X GET "http://localhost:8080/api/v1/classes/preview-code?courseId=1&startDate=2025-01-15" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json" \
  -s | jq .

echo ""
echo "----------------------------------------"
echo ""

# Test 3: Invalid branchId
echo "Test 3: Invalid branchId (999)"
curl -X GET "http://localhost:8080/api/v1/classes/preview-code?branchId=999&courseId=1&startDate=2025-01-15" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json" \
  -s | jq .

echo ""
echo "=========================================="
echo "Tests completed"
