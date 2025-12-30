########################
# Global
########################

variable "project_name" {
  type        = string
  description = "Project name prefix for all resources"
  default     = "jpvocab"
}

########################
# Lambda JAR paths
########################

variable "lambda_task_detector_jar" {
  type        = string
  description = "Path to task detector lambda JAR"
}

variable "lambda_discord_interaction_jar" {
  type        = string
  description = "Path to discord interaction lambda JAR"
}

variable "lambda_task_completion_jar" {
  type        = string
  description = "Path to task completion lambda JAR"
}

########################
# Discord
########################

variable "discord_bot_token" {
  type        = string
  sensitive   = true
}

variable "discord_channel_id" {
  type        = string
}

variable "discord_public_key" {
  type        = string
  sensitive = true
}

########################
# Google Docs
########################

variable "google_doc_id" {
  type        = string
}

variable "google_client_id" {
  type        = string
  sensitive   = true
}

variable "google_client_secret" {
  type        = string
  sensitive   = true
}

variable "google_refresh_token" {
  type        = string
  sensitive   = true
}

variable "my_name" {
  type        = string
}

########################
# AI
########################

variable "gemini_api_key" {
  type        = string
  sensitive   = true
}

########################
# DynamoDB
########################

variable "tasks_table_name" {
  type        = string
}

variable "ai_usage_table_name" {
  type        = string
}
