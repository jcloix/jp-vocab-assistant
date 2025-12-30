resource "aws_lambda_function" "task_detector" {
  function_name = "${var.project_name}-task-detector"
  runtime       = "java17"
  handler       = "io.github.jcloix.jpvocab.lambda.ingestion.TaskDetectorHandler::handleRequest"
  role          = aws_iam_role.lambda_role.arn

  filename         = var.lambda_task_detector_jar
  source_code_hash = filebase64sha256(var.lambda_task_detector_jar)

  memory_size = 1024
  timeout     = 60

  environment {
    variables = {
      # Google Docs
      GOOGLE_DOC_ID         = var.google_doc_id
      MY_NAME              = var.my_name
      GOOGLE_CLIENT_ID     = var.google_client_id
      GOOGLE_CLIENT_SECRET = var.google_client_secret
      GOOGLE_REFRESH_TOKEN = var.google_refresh_token

      # AI
      GEMINI_API_KEY = var.gemini_api_key

      # Discord
      DISCORD_BOT_TOKEN  = var.discord_bot_token
      DISCORD_CHANNEL_ID = var.discord_channel_id
      DISCORD_PUBLIC_KEY = var.discord_public_key

      # Database
      TASKS_TABLE_NAME = var.tasks_table_name
      AI_USAGE_TABLE  = var.ai_usage_table_name
    }
  }

  tags = {
    Project = var.project_name
  }
}
