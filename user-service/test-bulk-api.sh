#!/bin/bash

# Test script cho Bulk User API
BASE_URL="http://localhost:8080"

echo "=== Testing Bulk User Creation API ==="

# Test 1: Tạo 100 users
echo "Test 1: Creating 100 users..."
curl -X POST "$BASE_URL/api/users/bulk-create" \
  -H "Content-Type: application/json" \
  -d '{
    "count": 100,
    "batchSize": 10,
    "userTemplate": {
      "emailPattern": "testuser{index}@example.com",
      "usernamePattern": "testuser{index}",
      "namePattern": "Test User {index}",
      "password": "password123"
    }
  }' | jq '.'

echo -e "\n"

# Test 2: Tạo 1000 users
echo "Test 2: Creating 1000 users..."
curl -X POST "$BASE_URL/api/users/bulk-create" \
  -H "Content-Type: application/json" \
  -d '{
    "count": 1000,
    "batchSize": 100,
    "userTemplate": {
      "emailPattern": "bulkuser{index}@example.com",
      "usernamePattern": "bulkuser{index}",
      "namePattern": "Bulk User {index}",
      "password": "securePassword123"
    }
  }' | jq '.'

echo -e "\n"

# Test 3: Kiểm tra trạng thái service
echo "Test 3: Checking service status..."
curl -X GET "$BASE_URL/api/users/bulk-status" | jq '.'

echo -e "\n"

# Test 4: Test với invalid parameters
echo "Test 4: Testing with invalid count..."
curl -X POST "$BASE_URL/api/users/bulk-create" \
  -H "Content-Type: application/json" \
  -d '{
    "count": 0,
    "batchSize": 10
  }' | jq '.'

echo -e "\n"

echo "=== Test completed ===" 