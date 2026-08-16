# Daily aggregation schedule
resource "aws_cloudwatch_event_rule" "aggregate_daily" {
  name                = "${var.project_name}-aggregate-daily"
  description         = "Run daily stats aggregation at 00:00 UTC"
  schedule_expression = "cron(0 0 * * ? *)"
}

resource "aws_cloudwatch_event_target" "aggregate_daily" {
  rule      = aws_cloudwatch_event_rule.aggregate_daily.name
  target_id = "AggregateStatsFunction"
  arn       = aws_lambda_function.aggregate_stats.arn
}

resource "aws_lambda_permission" "aggregate_stats_events" {
  statement_id  = "AllowEventBridgeInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.aggregate_stats.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.aggregate_daily.arn
}
