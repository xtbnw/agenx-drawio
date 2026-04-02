curl -X POST "url" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer api-key" \
    -d '{
        "model": "model-name",
        "messages": [
        {
            "role": "user",
            "content": "1+1"
        }
        ],
        "stream": true,
        "max_tokens": 65536,
        "temperature": 0.0
    }'