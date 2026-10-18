#!/bin/bash
set -euo pipefail

CONTAINER="tech-challenge-4-localstack"

echo "=== DynamoDB ==="
docker exec "$CONTAINER" awslocal dynamodb scan --table-name tech-challenge-4-feedbacks

CRITICAL_QUEUE_URL=$(docker exec "$CONTAINER" awslocal sqs get-queue-url \
  --queue-name tech-challenge-4-critical-events \
  --query QueueUrl --output text | tr -d '\r')

ADMIN_QUEUE_URL=$(docker exec "$CONTAINER" awslocal sqs get-queue-url \
  --queue-name tech-challenge-4-admin-messages \
  --query QueueUrl --output text | tr -d '\r')

echo
echo "=== Eventos críticos publicados no SNS ==="
docker exec "$CONTAINER" awslocal sqs receive-message \
  --queue-url "$CRITICAL_QUEUE_URL" \
  --max-number-of-messages 10 \
  --wait-time-seconds 1

echo
echo "=== Mensagens para administradores ==="
docker exec "$CONTAINER" awslocal sqs receive-message \
  --queue-url "$ADMIN_QUEUE_URL" \
  --max-number-of-messages 10 \
  --wait-time-seconds 1
