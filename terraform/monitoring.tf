resource "aws_cloudwatch_log_group" "backend" {
  name              = "/aws/ecs/sahastra-backend"
  retention_in_days = 30
}

resource "aws_sns_topic" "ops_alerts" {
  name = "sahastra-ops-alerts"
}

resource "aws_sns_topic_subscription" "ops_email" {
  topic_arn = aws_sns_topic.ops_alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

resource "aws_cloudwatch_dashboard" "backend" {
  dashboard_name = "SahastraBackendOperations"
  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6
        properties = {
          metrics = [
            ["SahastraBackend", "RequestLatency", "Service", "sahastra-backend", { "stat": "p95" }]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "API latency (p95)"
          period  = 300
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 0
        width  = 12
        height = 6
        properties = {
          metrics = [
            ["SahastraBackend", "HTTP5xxErrors", "Service", "sahastra-backend"]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "HTTP 5xx error rate"
          period  = 300
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 6
        width  = 12
        height = 6
        properties = {
          metrics = [
            ["AWS/RDS", "DatabaseConnections", "DBInstanceIdentifier", aws_db_instance.postgres.identifier]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "RDS connection pool"
          period  = 300
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 6
        width  = 12
        height = 6
        properties = {
          metrics = [
            ["AWS/ElastiCache", "DatabaseMemoryUsagePercentage", "CacheClusterId", "sahastra-redis-001"]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "Redis memory usage"
          period  = 300
        }
      }
    ]
  })
}

resource "aws_cloudwatch_metric_alarm" "api_latency_p95" {
  alarm_name          = "sahastra-api-p95-latency"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 3
  threshold           = 1000
  period              = 300
  namespace           = "SahastraBackend"
  metric_name         = "RequestLatency"
  extended_statistic  = "p95"
  alarm_description   = "Alerts when API p95 latency exceeds the 1s target"
  dimensions = {
    Service = "sahastra-backend"
  }
  alarm_actions = [aws_sns_topic.ops_alerts.arn]
  ok_actions    = [aws_sns_topic.ops_alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "error_rate" {
  alarm_name          = "sahastra-error-rate"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 3
  threshold           = 1
  period              = 300
  namespace           = "SahastraBackend"
  metric_name         = "HTTP5xxErrors"
  statistic           = "Sum"
  alarm_description   = "Alerts when requests fail above the 1% target error rate"
  dimensions = {
    Service = "sahastra-backend"
  }
  alarm_actions = [aws_sns_topic.ops_alerts.arn]
  ok_actions    = [aws_sns_topic.ops_alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "database_connections" {
  alarm_name          = "sahastra-rds-connection-pool"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 3
  threshold           = 18
  period              = 300
  namespace           = "AWS/RDS"
  metric_name         = "DatabaseConnections"
  statistic           = "Average"
  alarm_description   = "Alerts when the PostgreSQL pool is approaching exhaustion"
  dimensions = {
    DBInstanceIdentifier = aws_db_instance.postgres.identifier
  }
  alarm_actions = [aws_sns_topic.ops_alerts.arn]
  ok_actions    = [aws_sns_topic.ops_alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "redis_memory" {
  alarm_name          = "sahastra-redis-memory"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 3
  threshold           = 80
  period              = 300
  namespace           = "AWS/ElastiCache"
  metric_name         = "DatabaseMemoryUsagePercentage"
  statistic           = "Average"
  alarm_description   = "Alerts when Redis memory usage exceeds the safe operating threshold"
  dimensions = {
    CacheClusterId = "sahastra-redis-001"
  }
  alarm_actions = [aws_sns_topic.ops_alerts.arn]
  ok_actions    = [aws_sns_topic.ops_alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "opensearch_index_lag" {
  alarm_name          = "sahastra-opensearch-index-lag"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 3
  threshold           = 300
  period              = 300
  namespace           = "AWS/OpenSearchService"
  metric_name         = "IndexingLag"
  statistic           = "Average"
  alarm_description   = "Alerts when OpenSearch indexing is delayed more than 5 minutes"
  dimensions = {
    DomainName = aws_opensearch_domain.search.domain_name
  }
  alarm_actions = [aws_sns_topic.ops_alerts.arn]
  ok_actions    = [aws_sns_topic.ops_alerts.arn]
}
