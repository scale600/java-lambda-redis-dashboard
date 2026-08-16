variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Prefix used for resource names"
  type        = string
  default     = "traffic-dashboard"
}

variable "frontend_domain" {
  description = "Custom domain for the dashboard (DNS managed on Cloudflare)"
  type        = string
  default     = "java-redis.techcloudup.com"
}

variable "lambda_runtime" {
  description = "Lambda runtime identifier"
  type        = string
  default     = "java17"
}

variable "lambda_memory_size" {
  description = "Lambda memory size in MB"
  type        = number
  default     = 512
}

variable "lambda_jar_path" {
  description = "Path to the backend uber-JAR. Build first: mvn clean package"
  type        = string
  default     = "../backend/target/backend-1.0.jar"
}

variable "upstash_redis_rest_url" {
  description = "Upstash Redis REST endpoint URL"
  type        = string
  sensitive   = true
}

variable "upstash_redis_rest_token" {
  description = "Upstash Redis REST auth token"
  type        = string
  sensitive   = true
}
