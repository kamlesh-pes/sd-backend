-- V4__search_indexing_schema.sql
-- OpenSearch support for product search and indexing

-- Outbox table for async product change events
-- Used for deferred indexing to OpenSearch via CDC or outbox pattern
CREATE TABLE IF NOT EXISTS outbox (
    id UUID PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL CHECK (event_type IN ('PRODUCT_CREATED', 'PRODUCT_UPDATED', 'PRODUCT_DELETED')),
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL DEFAULT 'Product' CHECK (aggregate_type IN ('Product')),
    payload JSONB NOT NULL,
    indexed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    indexed_at TIMESTAMP WITH TIME ZONE
);

-- Index for efficient polling by indexer service
CREATE INDEX IF NOT EXISTS idx_outbox_indexed_created ON outbox(indexed, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate ON outbox(aggregate_type, aggregate_id);

-- Search index metadata table for tracking indexing health
-- Stores lag and last sync timestamps for monitoring
CREATE TABLE IF NOT EXISTS search_index_metadata (
    id UUID PRIMARY KEY,
    index_name VARCHAR(100) NOT NULL UNIQUE,
    last_indexed_product_id UUID,
    last_indexed_at TIMESTAMP WITH TIME ZONE,
    document_count BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Insert initial metadata for products index
INSERT INTO search_index_metadata (id, index_name) 
VALUES (gen_random_uuid(), 'products') 
ON CONFLICT (index_name) DO NOTHING;

-- Create search audit log for monitoring indexing operations
CREATE TABLE IF NOT EXISTS search_audit_log (
    id UUID PRIMARY KEY,
    operation VARCHAR(50) NOT NULL CHECK (operation IN ('INDEX', 'DELETE', 'BULK_INDEX', 'ERROR')),
    document_id UUID,
    index_name VARCHAR(100) NOT NULL,
    document_count INT,
    duration_ms INT,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_search_audit_created ON search_audit_log(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_search_audit_operation ON search_audit_log(operation, created_at DESC);
