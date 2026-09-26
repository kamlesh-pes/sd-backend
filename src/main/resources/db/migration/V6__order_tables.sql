CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    tax DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    shipping DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    shipping_address VARCHAR(255) NOT NULL,
    city VARCHAR(120) NOT NULL,
    state VARCHAR(120),
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(120) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_order_user ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_order_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_order_created_at ON orders(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_idempotency ON orders(user_id, idempotency_key);

CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10,2) NOT NULL,
    line_total DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_order_item_order ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_item_product ON order_items(product_id);

CREATE TABLE IF NOT EXISTS order_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    actor_id VARCHAR(64),
    actor_type VARCHAR(30),
    reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_order_history_order ON order_status_history(order_id);

CREATE TABLE IF NOT EXISTS idempotency_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    key_value VARCHAR(128) NOT NULL,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_idempotency_user_key UNIQUE (user_id, key_value)
);

CREATE INDEX IF NOT EXISTS idx_idempotency_user ON idempotency_keys(user_id);
CREATE INDEX IF NOT EXISTS idx_idempotency_key ON idempotency_keys(key_value);
