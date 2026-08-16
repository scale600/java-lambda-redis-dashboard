output "api_base_url" {
  description = "API Gateway invoke URL (prod stage)"
  value       = aws_api_gateway_stage.prod.invoke_url
}

output "frontend_bucket_name" {
  description = "S3 bucket for the frontend"
  value       = aws_s3_bucket.frontend.id
}

output "cloudfront_domain" {
  description = "CloudFront distribution domain (CNAME target in Cloudflare)"
  value       = aws_cloudfront_distribution.frontend.domain_name
}

output "frontend_domain" {
  description = "Custom dashboard domain"
  value       = var.frontend_domain
}

output "acm_validation_records" {
  description = "DNS records to add to Cloudflare to validate the ACM certificate"
  value = {
    for dvo in aws_acm_certificate.frontend.domain_validation_options :
    dvo.domain_name => {
      name  = dvo.resource_record_name
      type  = dvo.resource_record_type
      value = dvo.resource_record_value
    }
  }
}
