########################
# VocabTasks table
########################

resource "aws_dynamodb_table" "vocab_tasks" {
  name         = var.tasks_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "PK"
  range_key    = "SK"

  attribute {
    name = "PK"
    type = "S"
  }

  attribute {
    name = "SK"
    type = "S"
  }

  tags = {
    Project = var.project_name
  }
}

########################
# AIModelUsage table
########################

resource "aws_dynamodb_table" "ai_usage" {
  name         = var.ai_usage_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "modelName"
  range_key    = "date"

  attribute {
    name = "modelName"
    type = "S"
  }

  attribute {
    name = "date"
    type = "S"
  }

  tags = {
    Project = var.project_name
  }
}
