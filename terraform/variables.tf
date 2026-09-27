variable "aws_region" {
  description = "AWS region for deploying infrastructure"
  type        = string
  default     = "us-east-1"
}

variable "db_username" {
  description = "Master username for PostgreSQL"
  type        = string
  default     = "sahastra_user"
}

variable "db_password" {
  description = "Master password for PostgreSQL"
  type        = string
  sensitive   = true
}

variable "redis_password" {
  description = "Redis auth token"
  type        = string
  sensitive   = true
}

variable "opensearch_admin" {
  description = "OpenSearch admin username"
  type        = string
  default     = "admin"
}

variable "opensearch_password" {
  description = "OpenSearch admin password"
  type        = string
  sensitive   = true
}

variable "alert_email" {
  description = "Email address for operational alerts"
  type        = string
  default     = "ops@example.com"
}
