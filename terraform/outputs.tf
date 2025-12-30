output "discord_interactions_url" {
  value = "${aws_apigatewayv2_api.discord_http_api.api_endpoint}/discord-interactions"
}
