$ErrorActionPreference = "Stop"
$container = "tech-challenge-4-localstack"

Write-Host "=== DynamoDB ==="
docker exec $container awslocal dynamodb scan --table-name tech-challenge-4-feedbacks

$criticalQueueUrl = docker exec $container awslocal sqs get-queue-url `
  --queue-name tech-challenge-4-critical-events `
  --query QueueUrl --output text

$adminQueueUrl = docker exec $container awslocal sqs get-queue-url `
  --queue-name tech-challenge-4-admin-messages `
  --query QueueUrl --output text

Write-Host "`n=== Eventos críticos publicados no SNS ==="
docker exec $container awslocal sqs receive-message `
  --queue-url $criticalQueueUrl.Trim() `
  --max-number-of-messages 10 `
  --wait-time-seconds 1

Write-Host "`n=== Mensagens para administradores ==="
docker exec $container awslocal sqs receive-message `
  --queue-url $adminQueueUrl.Trim() `
  --max-number-of-messages 10 `
  --wait-time-seconds 1
