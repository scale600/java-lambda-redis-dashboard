terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.34"
    }
  }

  # Remote state (optional): create the S3 bucket + DynamoDB table first,
  # then run `terraform init -migrate-state`.
  #
  # backend "s3" {
  #   bucket         = "tfstate-753523452116-us-east-1"
  #   key            = "java-lambda-redis-dashboard/terraform.tfstate"
  #   region         = "us-east-1"
  #   encrypt        = true
  #   dynamodb_table = "tfstate-lock-753523452116"
  # }
}

provider "aws" {
  region = var.aws_region
}

# CloudFront certificates must be created in us-east-1
provider "aws" {
  alias  = "us_east_1"
  region = "us-east-1"
}
