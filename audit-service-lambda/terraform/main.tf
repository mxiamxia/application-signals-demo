data "aws_region" "current" {}
provider "aws" {
  region = "us-east-1"
}

#### SQS

resource "aws_sqs_queue" "audit-jobs" {
  delay_seconds              = 90
  max_message_size           = 2048
  message_retention_seconds  = 86400
  receive_wait_time_seconds  = 10
  visibility_timeout_seconds = 60

  policy = <<POLICY
{
  "Version": "2012-10-17",
  "Id": "arn:aws:sqs:*:*:*/SQSPolicy",
  "Statement": [
    {
      "Sid": "QueuesQA_AllActions",
      "Effect": "Allow",
      "Action": [
        "sqs:ChangeMessageVisibility",
        "sqs:ChangeMessageVisibilityBatch",
        "sqs:GetQueueAttributes",
        "sqs:GetQueueUrl",
        "sqs:ListDeadLetterSourceQueues",
        "sqs:ListQueues",
        "sqs:ReceiveMessage",
        "sqs:SendMessage",
        "sqs:SendMessageBatch",
        "sqs:SetQueueAttributes"
      ],
      "Resource": "arn:aws:sqs:*:*:*/SQSPolicy"
    }
  ]
}
POLICY
}

### IAM role

resource "aws_iam_role" "lambda_exec_role_audit_service" {

  name = "lambda_exec_role_audit_service"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Action    = "sts:AssumeRole",
      Effect    = "Allow",
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_policy" "lambda_policy_audit_service" {
  name        = local.policy_name
  description = "IAM policy for Lambda to write logs to CloudWatch"

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Action   = [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      Effect   = "Allow",
      Resource = "arn:aws:logs:*:*:*"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "admin_policy_audit_service" {
  role       = local.role_name
  policy_arn = "arn:aws:iam::aws:policy/AdministratorAccess"
}

resource "aws_iam_role_policy_attachment" "lambda_policy_attach_audit_service" {
  role       = local.role_name
  policy_arn = local.policy_arn
}

locals {
  role_arn = aws_iam_role.lambda_exec_role_audit_service.arn
  role_name = "lambda_exec_role_audit_service"
  policy_arn = aws_iam_policy.lambda_policy_audit_service.arn
  policy_name = "lambda_policy_audit_service"
  layer_arns = {
    "af-south-1"     = "arn:aws:lambda:af-south-1:904233096616:layer:AWSOpenTelemetryDistroPython:4"
    "ap-east-1"      = "arn:aws:lambda:ap-east-1:888577020596:layer:AWSOpenTelemetryDistroPython:4"
    "ap-northeast-1" = "arn:aws:lambda:ap-northeast-1:615299751070:layer:AWSOpenTelemetryDistroPython:4"
    "ap-northeast-2" = "arn:aws:lambda:ap-northeast-2:615299751070:layer:AWSOpenTelemetryDistroPython:4"
    "ap-northeast-3" = "arn:aws:lambda:ap-northeast-3:615299751070:layer:AWSOpenTelemetryDistroPython:4"
    "ap-south-1"     = "arn:aws:lambda:ap-south-1:615299751070:layer:AWSOpenTelemetryDistroPython:4"
    "ap-south-2"     = "arn:aws:lambda:ap-south-2:796973505492:layer:AWSOpenTelemetryDistroPython:4"
    "ap-southeast-1" = "arn:aws:lambda:ap-southeast-1:615299751070:layer:AWSOpenTelemetryDistroPython:4"
    "ap-southeast-2" = "arn:aws:lambda:ap-southeast-2:615299751070:layer:AWSOpenTelemetryDistroPython:4"
    "ap-southeast-3" = "arn:aws:lambda:ap-southeast-3:039612877180:layer:AWSOpenTelemetryDistroPython:4"
    "ap-southeast-4" = "arn:aws:lambda:ap-southeast-4:713881805771:layer:AWSOpenTelemetryDistroPython:4"
    "ca-central-1"   = "arn:aws:lambda:ca-central-1:615299751070:layer:AWSOpenTelemetryDistroPython:4"
    "eu-central-1"   = "arn:aws:lambda:eu-central-1:615299751070:layer:AWSOpenTelemetryDistroPython:4"
    "eu-central-2"   = "arn:aws:lambda:eu-central-2:156041407956:layer:AWSOpenTelemetryDistroPython:4"
    "eu-north-1"     = "arn:aws:lambda:eu-north-1:615299751070:layer:AWSOpenTelemetryDistroPython:4"
    "eu-south-1"     = "arn:aws:lambda:eu-south-1:257394471194:layer:AWSOpenTelemetryDistroPython:4"
    "eu-south-2"     = "arn:aws:lambda:eu-south-2:490004653786:layer:AWSOpenTelemetryDistroPython:4"
    "eu-west-1"      = "arn:aws:lambda:eu-west-1:615299751070:layer:AWSOpenTelemetryDistroPython:4"
    "eu-west-2"      = "arn:aws:lambda:eu-west-2:615299751070:layer:AWSOpenTelemetryDistroPython:4"
    "eu-west-3"      = "arn:aws:lambda:eu-west-3:615299751070:layer:AWSOpenTelemetryDistroPython:4"
    "il-central-1"   = "arn:aws:lambda:il-central-1:746669239226:layer:AWSOpenTelemetryDistroPython:4"
    "me-central-1"   = "arn:aws:lambda:me-central-1:739275441131:layer:AWSOpenTelemetryDistroPython:4"
    "me-south-1"     = "arn:aws:lambda:me-south-1:980921751758:layer:AWSOpenTelemetryDistroPython:4"
    "sa-east-1"      = "arn:aws:lambda:sa-east-1:615299751070:layer:AWSOpenTelemetryDistroPython:4"
    "us-east-1"      = "arn:aws:lambda:us-east-1:615299751070:layer:AWSOpenTelemetryDistroPython:4"
    "us-east-2"      = "arn:aws:lambda:us-east-2:615299751070:layer:AWSOpenTelemetryDistroPython:4"
    "us-west-1"      = "arn:aws:lambda:us-west-1:615299751070:layer:AWSOpenTelemetryDistroPython:11"
    "us-west-2"      = "arn:aws:lambda:us-west-2:615299751070:layer:AWSOpenTelemetryDistroPython:11"
  }
}


###### lambda functions
resource "aws_lambda_function" "audit-service" {
  function_name = "audit-service"

  handler = "lambda_function.lambda_handler"
  runtime = "python3.12"
  timeout = 30

  role = local.role_arn

  filename         = "${path.module}/../sample-app/build/function.zip"
  tracing_config {
    mode = "Active"
  }
  layers = [lookup(local.layer_arns, data.aws_region.current.name, "")]

  environment {
    variables = {
      AWS_LAMBDA_EXEC_WRAPPER = "/opt/otel-instrument",
#      OTEL_TRACES_EXPORTER = "console,otlp"
    }
  }
}

resource "aws_lambda_event_source_mapping" "example" {
  event_source_arn = aws_sqs_queue.audit-jobs.arn
  function_name    = aws_lambda_function.audit-service.arn
}