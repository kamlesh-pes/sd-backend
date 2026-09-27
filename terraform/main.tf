terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

resource "aws_db_instance" "postgres" {
  identifier             = "sahastra-postgres"
  engine                 = "postgres"
  engine_version         = "16"
  instance_class         = "db.t4g.micro"
  allocated_storage      = 20
  db_name                = "sahastra_db"
  username               = var.db_username
  password               = var.db_password
  skip_final_snapshot    = true
  publicly_accessible    = false
  backup_retention_period = 7
  storage_encrypted      = true

  vpc_security_group_ids = [aws_security_group.database.id]
  db_subnet_group_name   = aws_db_subnet_group.main.name
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id       = "sahastra-redis"
  description                = "Redis for cache, rate limiting, and session state"
  engine                     = "redis"
  node_type                  = "cache.t4g.micro"
  num_cache_clusters         = 1
  automatic_failover_enabled = false
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  auth_token                 = var.redis_password
  security_group_ids         = [aws_security_group.redis.id]
  subnet_group_name          = aws_elasticache_subnet_group.main.name
}

resource "aws_opensearch_domain" "search" {
  domain_name    = "sahastra-search"
  engine_version = "OpenSearch_2.11"

  cluster_config {
    instance_type = "r6g.large.search"
    instance_count = 1
  }

  ebs_options {
    ebs_enabled = true
    volume_size = 10
  }

  advanced_security_options {
    enabled                        = true
    internal_user_database_enabled = true
    master_user_options {
      master_user_name     = var.opensearch_admin
      master_user_password = var.opensearch_password
    }
  }

  node_to_node_encryption {
    enabled = true
  }

  encrypt_at_rest {
    enabled = true
  }

  domain_endpoint_options {
    enforce_https       = true
    tls_security_policy = "Policy-Min-TLS-1-2-2019-06"
  }

  vpc_options {
    subnet_ids = [aws_subnet.main.id]
    security_group_ids = [aws_security_group.search.id]
  }
}

resource "aws_ecs_cluster" "backend" {
  name = "sahastra-backend"
}

resource "aws_security_group" "database" {
  name   = "sahastra-db-sg"
  vpc_id = aws_vpc.main.id

  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }
}

resource "aws_security_group" "redis" {
  name   = "sahastra-redis-sg"
  vpc_id = aws_vpc.main.id

  ingress {
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }
}

resource "aws_security_group" "search" {
  name   = "sahastra-search-sg"
  vpc_id = aws_vpc.main.id

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }
}

resource "aws_vpc" "main" {
  cidr_block = "10.0.0.0/16"
  enable_dns_support = true
  enable_dns_hostnames = true
}

resource "aws_subnet" "main" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "${var.aws_region}a"
  map_public_ip_on_launch = false
}

resource "aws_db_subnet_group" "main" {
  name       = "sahastra-db-subnet-group"
  subnet_ids = [aws_subnet.main.id]
}

resource "aws_elasticache_subnet_group" "main" {
  name       = "sahastra-redis-subnet-group"
  subnet_ids = [aws_subnet.main.id]
}
