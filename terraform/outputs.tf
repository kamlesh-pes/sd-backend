output "postgres_endpoint" {
  description = "PostgreSQL RDS endpoint"
  value       = aws_db_instance.postgres.address
}

output "redis_endpoint" {
  description = "Redis endpoint"
  value       = aws_elasticache_replication_group.redis.primary_endpoint_address
}

output "opensearch_endpoint" {
  description = "OpenSearch endpoint"
  value       = aws_opensearch_domain.search.endpoint
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.backend.name
}
