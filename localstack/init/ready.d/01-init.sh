#!/bin/bash
set -euo pipefail

REGION="us-east-1"
ACCOUNT_ID="000000000000"
TABLE_NAME="tech-challenge-4-feedbacks"
CRITICAL_TOPIC="tech-challenge-4-critical-feedbacks"
ADMIN_TOPIC="tech-challenge-4-admin-notifications"
CRITICAL_QUEUE="tech-challenge-4-critical-events"
ADMIN_QUEUE="tech-challenge-4-admin-messages"

echo "[tc4-local] Criando recursos locais..."

if ! awslocal dynamodb describe-table --table-name "$TABLE_NAME" >/dev/null 2>&1; then
  awslocal dynamodb create-table \
    --table-name "$TABLE_NAME" \
    --attribute-definitions AttributeName=id,AttributeType=S \
    --key-schema AttributeName=id,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST >/dev/null
fi

CRITICAL_TOPIC_ARN=$(awslocal sns create-topic --name "$CRITICAL_TOPIC" --query TopicArn --output text)
ADMIN_TOPIC_ARN=$(awslocal sns create-topic --name "$ADMIN_TOPIC" --query TopicArn --output text)

CRITICAL_QUEUE_URL=$(awslocal sqs create-queue --queue-name "$CRITICAL_QUEUE" --query QueueUrl --output text)
ADMIN_QUEUE_URL=$(awslocal sqs create-queue --queue-name "$ADMIN_QUEUE" --query QueueUrl --output text)

CRITICAL_QUEUE_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$CRITICAL_QUEUE_URL" \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' \
  --output text)

ADMIN_QUEUE_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$ADMIN_QUEUE_URL" \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' \
  --output text)

subscribe_if_needed() {
  local topic_arn="$1"
  local queue_arn="$2"
  local existing
  existing=$(awslocal sns list-subscriptions-by-topic \
    --topic-arn "$topic_arn" \
    --query "Subscriptions[?Endpoint=='${queue_arn}'].SubscriptionArn | [0]" \
    --output text)

  if [ "$existing" = "None" ] || [ -z "$existing" ]; then
    awslocal sns subscribe \
      --topic-arn "$topic_arn" \
      --protocol sqs \
      --notification-endpoint "$queue_arn" >/dev/null
  fi
}

subscribe_if_needed "$CRITICAL_TOPIC_ARN" "$CRITICAL_QUEUE_ARN"
subscribe_if_needed "$ADMIN_TOPIC_ARN" "$ADMIN_QUEUE_ARN"

echo "[tc4-local] Recursos prontos:"
echo "  DynamoDB: $TABLE_NAME"
echo "  SNS crítico: $CRITICAL_TOPIC_ARN"
echo "  SNS admin: $ADMIN_TOPIC_ARN"
echo "  SQS observação crítico: $CRITICAL_QUEUE_URL"
echo "  SQS observação admin: $ADMIN_QUEUE_URL"
