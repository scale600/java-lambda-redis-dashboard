resource "aws_api_gateway_rest_api" "api" {
  name        = "${var.project_name}-api"
  description = "Real-time traffic monitoring dashboard API"
}

# ---------------------------------------------------------------------------
# POST /visit -> RecordVisitFunction
# ---------------------------------------------------------------------------
resource "aws_api_gateway_resource" "visit" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = "visit"
}

resource "aws_api_gateway_method" "visit_post" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.visit.id
  http_method   = "POST"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "visit_post" {
  rest_api_id             = aws_api_gateway_rest_api.api.id
  resource_id             = aws_api_gateway_resource.visit.id
  http_method             = aws_api_gateway_method.visit_post.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_alias.record_visit.invoke_arn
}

# ---------------------------------------------------------------------------
# GET /health -> GetStatsFunction
# ---------------------------------------------------------------------------
resource "aws_api_gateway_resource" "health" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = "health"
}

resource "aws_api_gateway_method" "health_get" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.health.id
  http_method   = "GET"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "health_get" {
  rest_api_id             = aws_api_gateway_rest_api.api.id
  resource_id             = aws_api_gateway_resource.health.id
  http_method             = aws_api_gateway_method.health_get.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_alias.get_stats.invoke_arn
}

# ---------------------------------------------------------------------------
# /stats/{proxy+} -> GetStatsFunction
# (covers /stats/overview, /stats/timeseries, /stats/paths, /stats/recent)
# ---------------------------------------------------------------------------
resource "aws_api_gateway_resource" "stats" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = "stats"
}

resource "aws_api_gateway_resource" "stats_proxy" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_resource.stats.id
  path_part   = "{proxy+}"
}

resource "aws_api_gateway_method" "stats_any" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.stats_proxy.id
  http_method   = "ANY"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "stats_any" {
  rest_api_id             = aws_api_gateway_rest_api.api.id
  resource_id             = aws_api_gateway_resource.stats_proxy.id
  http_method             = aws_api_gateway_method.stats_any.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_alias.get_stats.invoke_arn
}

# ---------------------------------------------------------------------------
# Permissions: allow API Gateway to invoke the Lambda aliases
# ---------------------------------------------------------------------------
resource "aws_lambda_permission" "record_visit_api" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.record_visit.function_name
  qualifier     = aws_lambda_alias.record_visit.name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "get_stats_api" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.get_stats.function_name
  qualifier     = aws_lambda_alias.get_stats.name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/*/*"
}

# ---------------------------------------------------------------------------
# Deployment + stage
# ---------------------------------------------------------------------------
resource "aws_api_gateway_deployment" "api" {
  rest_api_id = aws_api_gateway_rest_api.api.id

  triggers = {
    redeployment = sha1(jsonencode([
      aws_api_gateway_integration.visit_post,
      aws_api_gateway_integration.health_get,
      aws_api_gateway_integration.stats_any,
    ]))
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_api_gateway_stage" "prod" {
  deployment_id = aws_api_gateway_deployment.api.id
  rest_api_id   = aws_api_gateway_rest_api.api.id
  stage_name    = "prod"
}
