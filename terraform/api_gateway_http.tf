############################
# HTTP API (API Gateway v2)
############################

resource "aws_apigatewayv2_api" "discord_http_api" {
  name          = "${var.project_name}-discord-http-api"
  protocol_type = "HTTP"
}

############################
# Lambda integration (ALIAS)
############################

resource "aws_apigatewayv2_integration" "discord_lambda_integration" {
  api_id           = aws_apigatewayv2_api.discord_http_api.id
  integration_type = "AWS_PROXY"

  # IMPORTANT: invoke ARN of the ALIAS, not the function
  integration_uri  = aws_lambda_alias.discord_live.invoke_arn

  integration_method = "POST"
}

############################
# Route
############################

resource "aws_apigatewayv2_route" "discord_route" {
  api_id    = aws_apigatewayv2_api.discord_http_api.id
  route_key = "POST /discord-interactions"
  target    = "integrations/${aws_apigatewayv2_integration.discord_lambda_integration.id}"
}

############################
# Stage
############################

resource "aws_apigatewayv2_stage" "prod" {
  api_id      = aws_apigatewayv2_api.discord_http_api.id
  name        = "$default"
  auto_deploy = true
}

############################
# Permission for API Gateway (ALIAS)
############################

resource "aws_lambda_permission" "allow_apigw" {
  statement_id  = "AllowAPIGatewayInvokeDiscord"
  action        = "lambda:InvokeFunction"

  function_name = aws_lambda_function.discord_interaction.function_name
  qualifier     = aws_lambda_alias.discord_live.name

  principal  = "apigateway.amazonaws.com"
  source_arn = "${aws_apigatewayv2_api.discord_http_api.execution_arn}/*/*"
}
