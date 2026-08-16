# Shared environment for all functions
locals {
  lambda_env = {
    UPSTASH_REDIS_REST_URL   = var.upstash_redis_rest_url
    UPSTASH_REDIS_REST_TOKEN = var.upstash_redis_rest_token
  }
}

# ---------------------------------------------------------------------------
# RecordVisitFunction — handles POST /visit (4-command write path)
# ---------------------------------------------------------------------------
resource "aws_lambda_function" "record_visit" {
  function_name    = "${var.project_name}-record-visit"
  role             = aws_iam_role.lambda_exec.arn
  handler          = "com.example.dashboard.RecordVisitHandler::handleRequest"
  runtime          = var.lambda_runtime
  architectures    = ["arm64"]
  memory_size      = var.lambda_memory_size
  timeout          = 30
  filename         = var.lambda_jar_path
  source_code_hash = filebase64sha256(var.lambda_jar_path)
  publish          = true

  environment {
    variables = local.lambda_env
  }

  snap_start {
    apply_on = "PublishedVersions"
  }
}

# ---------------------------------------------------------------------------
# GetStatsFunction — handles GET /health and GET /stats/*
# ---------------------------------------------------------------------------
resource "aws_lambda_function" "get_stats" {
  function_name    = "${var.project_name}-get-stats"
  role             = aws_iam_role.lambda_exec.arn
  handler          = "com.example.dashboard.GetStatsHandler::handleRequest"
  runtime          = var.lambda_runtime
  architectures    = ["arm64"]
  memory_size      = var.lambda_memory_size
  timeout          = 30
  filename         = var.lambda_jar_path
  source_code_hash = filebase64sha256(var.lambda_jar_path)
  publish          = true

  environment {
    variables = local.lambda_env
  }

  snap_start {
    apply_on = "PublishedVersions"
  }
}

# ---------------------------------------------------------------------------
# AggregateStatsFunction — daily rollup (scheduled)
# ---------------------------------------------------------------------------
resource "aws_lambda_function" "aggregate_stats" {
  function_name    = "${var.project_name}-aggregate-stats"
  role             = aws_iam_role.lambda_exec.arn
  handler          = "com.example.dashboard.AggregateStatsHandler::handleRequest"
  runtime          = var.lambda_runtime
  architectures    = ["arm64"]
  memory_size      = var.lambda_memory_size
  timeout          = 60
  filename         = var.lambda_jar_path
  source_code_hash = filebase64sha256(var.lambda_jar_path)
  publish          = true

  environment {
    variables = local.lambda_env
  }

  snap_start {
    apply_on = "PublishedVersions"
  }
}

# SnapStart only applies to published versions, so API Gateway must invoke the
# function through an alias.
resource "aws_lambda_alias" "record_visit" {
  name             = "live"
  function_name    = aws_lambda_function.record_visit.function_name
  function_version = aws_lambda_function.record_visit.version
}

resource "aws_lambda_alias" "get_stats" {
  name             = "live"
  function_name    = aws_lambda_function.get_stats.function_name
  function_version = aws_lambda_function.get_stats.version
}

resource "aws_lambda_alias" "aggregate_stats" {
  name             = "live"
  function_name    = aws_lambda_function.aggregate_stats.function_name
  function_version = aws_lambda_function.aggregate_stats.version
}
