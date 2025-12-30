########################
# Discord task queue
########################

resource "aws_sqs_queue" "discord_task_queue" {
  name = "${var.project_name}-discord-task-queue"

  visibility_timeout_seconds = 60
  message_retention_seconds  = 1209600 # 14 days

  tags = {
    Project = var.project_name
  }
}
