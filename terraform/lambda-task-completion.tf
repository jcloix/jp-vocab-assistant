########################
# Task completion Lambda
########################

resource "aws_lambda_function" "task_completion" {
  function_name = "${var.project_name}-task-completion"
  runtime       = "java17"
  handler       = "io.github.jcloix.jpvocab.lambda.completion.DiscordTaskWorker::handleRequest"
  role          = aws_iam_role.lambda_role.arn

  filename         = "../lambda-task-completion/target/lambda-task-completion-0.1.0.jar"
  source_code_hash = filebase64sha256("../lambda-task-completion/target/lambda-task-completion-0.1.0.jar")

  memory_size = 512
  timeout     = 30

  environment {
    variables = {
      GOOGLE_DOC_ID         = var.google_doc_id
      MY_NAME               = var.my_name
      GOOGLE_CLIENT_ID      = var.google_client_id
      GOOGLE_CLIENT_SECRET  = var.google_client_secret
      GOOGLE_REFRESH_TOKEN  = var.google_refresh_token

      DISCORD_BOT_TOKEN     = var.discord_bot_token
      DISCORD_CHANNEL_ID    = var.discord_channel_id
      DISCORD_PUBLIC_KEY    = var.discord_public_key

      DISCORD_TASK_QUEUE_URL = aws_sqs_queue.discord_task_queue.url

      TASKS_TABLE_NAME = aws_dynamodb_table.vocab_tasks.name
    }
  }

  tags = {
    Project = var.project_name
  }
}

########################
# SQS → Lambda trigger
########################

resource "aws_lambda_event_source_mapping" "task_completion_sqs" {
  event_source_arn = aws_sqs_queue.discord_task_queue.arn
  function_name    = aws_lambda_function.task_completion.arn

  batch_size = 1
  enabled    = true
}
