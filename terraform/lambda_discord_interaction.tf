########################
# Discord Interaction Lambda
########################

resource "aws_lambda_function" "discord_interaction" {
  function_name = "${var.project_name}-discord-interaction"
  role          = aws_iam_role.lambda_role.arn
  handler       = "io.github.jcloix.jpvocab.lambda.discord.DiscordInteractionHandler::handleRequest"
  runtime       = "java17"

  filename         = "../lambda-discord-interaction/target/lambda-discord-interaction-0.1.0.jar"
  source_code_hash = filebase64sha256("../lambda-discord-interaction/target/lambda-discord-interaction-0.1.0.jar")

  memory_size = 512
  timeout     = 10

  publish = true

  snap_start {
    apply_on = "PublishedVersions"
  }

  environment {
    variables = {
      DISCORD_PUBLIC_KEY     = var.discord_public_key
      DISCORD_TASK_QUEUE_URL = aws_sqs_queue.discord_task_queue.url
    }
  }

  tags = {
    Project = var.project_name
  }
}

########################
# Lambda Alias (USED BY API GATEWAY)
########################

resource "aws_lambda_alias" "discord_live" {
  name             = "live"
  description      = "Stable alias for Discord interactions"
  function_name    = aws_lambda_function.discord_interaction.function_name
  function_version = aws_lambda_function.discord_interaction.version
}
