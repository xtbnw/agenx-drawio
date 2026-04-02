#!/usr/bin/env bash

BASE_URL="http://localhost:8091/api/v1"
AGENT_ID="balanced"
USER_ID="demo-user"

echo "1) Query available agents"
curl -s "${BASE_URL}/query_ai_agent_config_list"
echo
echo

echo "2) Create a session"
curl -s -X POST "${BASE_URL}/create_session" \
  -H "Content-Type: application/json" \
  -d "{
    \"agentId\": \"${AGENT_ID}\",
    \"userId\": \"${USER_ID}\"
  }"
echo
echo

echo "3) Ask for draw.io XML"
curl -s -X POST "${BASE_URL}/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": "balanced",
    "userId": "demo-user",
    "message": "Draw an e-commerce flowchart for order creation, inventory check, payment, shipment, and notification. Return importable draw.io XML only."
  }'
echo
