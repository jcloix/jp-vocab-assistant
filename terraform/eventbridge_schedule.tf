########################
# EventBridge Scheduler
########################

# Schedule 1: Every hour from 7-9 AM JST (22:00-00:00 UTC)
resource "aws_cloudwatch_event_rule" "task_detector_morning" {
  name                = "${var.project_name}-task-detector-morning"
  description         = "Runs every hour 7-9 AM JST"
  schedule_expression = "cron(0 22,23,0 * * ? *)"

  tags = {
    Project = var.project_name
  }
}

resource "aws_cloudwatch_event_target" "task_detector_morning_target" {
  rule      = aws_cloudwatch_event_rule.task_detector_morning.name
  target_id = "TaskDetectorLambdaMorning"
  arn       = aws_lambda_function.task_detector.arn
}

resource "aws_lambda_permission" "allow_eventbridge_morning" {
  statement_id  = "AllowExecutionFromEventBridgeMorning"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.task_detector.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.task_detector_morning.arn
}

# Schedule 2: Every 30 min from 9 AM to 1 PM JST (00:00-04:00 UTC)
resource "aws_cloudwatch_event_rule" "task_detector_midday" {
  name                = "${var.project_name}-task-detector-midday"
  description         = "Runs every 30 min 9 AM-1 PM JST"
  schedule_expression = "cron(0,30 0,1,2,3,4 * * ? *)"

  tags = {
    Project = var.project_name
  }
}

resource "aws_cloudwatch_event_target" "task_detector_midday_target" {
  rule      = aws_cloudwatch_event_rule.task_detector_midday.name
  target_id = "TaskDetectorLambdaMidday"
  arn       = aws_lambda_function.task_detector.arn
}

resource "aws_lambda_permission" "allow_eventbridge_midday" {
  statement_id  = "AllowExecutionFromEventBridgeMidday"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.task_detector.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.task_detector_midday.arn
}

# Schedule 3: Every 2 hours from 1 PM to 8 PM JST (04:00-11:00 UTC)
resource "aws_cloudwatch_event_rule" "task_detector_afternoon" {
  name                = "${var.project_name}-task-detector-afternoon"
  description         = "Runs every 2 hours 1-8 PM JST"
  schedule_expression = "cron(0 4,6,8,10 * * ? *)"

  tags = {
    Project = var.project_name
  }
}

resource "aws_cloudwatch_event_target" "task_detector_afternoon_target" {
  rule      = aws_cloudwatch_event_rule.task_detector_afternoon.name
  target_id = "TaskDetectorLambdaAfternoon"
  arn       = aws_lambda_function.task_detector.arn
}

resource "aws_lambda_permission" "allow_eventbridge_afternoon" {
  statement_id  = "AllowExecutionFromEventBridgeAfternoon"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.task_detector.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.task_detector_afternoon.arn
}

########################
# Summary of execution times (JST)
########################

# Morning (7-9 AM):   3 executions (7 AM, 8 AM, 9 AM)
# Midday (9 AM-1 PM): 9 executions (every 30 min: 9:00, 9:30, 10:00, 10:30, 11:00, 11:30, 12:00, 12:30, 1:00 PM)
# Afternoon (1-8 PM): 4 executions (1 PM, 3 PM, 5 PM, 7 PM)
# Total: 16 executions per day