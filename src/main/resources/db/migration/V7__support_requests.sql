CREATE TABLE IF NOT EXISTS support_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_support_request_order ON support_requests(order_id);
CREATE INDEX IF NOT EXISTS idx_support_request_user ON support_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_support_request_status ON support_requests(status);
CREATE UNIQUE INDEX IF NOT EXISTS uk_support_request_order_user ON support_requests(order_id, user_id);
