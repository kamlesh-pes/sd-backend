CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(140) NOT NULL UNIQUE,
    parent_id UUID REFERENCES categories(id),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_category_slug ON categories(slug);
CREATE INDEX idx_category_parent ON categories(parent_id);

CREATE TABLE brands (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(140) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX idx_brand_slug ON brands(slug);

CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category_id UUID NOT NULL REFERENCES categories(id),
    brand_id UUID NOT NULL REFERENCES brands(id),
    base_price DECIMAL(10,2) NOT NULL CHECK (base_price > 0),
    discount_type VARCHAR(20),
    discount_value DECIMAL(10,2),
    discount_starts_at TIMESTAMP WITH TIME ZONE,
    discount_ends_at TIMESTAMP WITH TIME ZONE,
    stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    attributes JSONB,
    images JSONB,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT product_discount_type_check CHECK (discount_type IS NULL OR discount_type IN ('PERCENTAGE', 'FIXED')),
    CONSTRAINT product_discount_value_check CHECK (discount_value IS NULL OR discount_value >= 0),
    CONSTRAINT product_discount_pair_check CHECK ((discount_type IS NULL) = (discount_value IS NULL)),
    CONSTRAINT product_discount_window_check CHECK (discount_ends_at IS NULL OR discount_starts_at IS NULL OR discount_ends_at > discount_starts_at),
    CONSTRAINT product_percentage_check CHECK (discount_type <> 'PERCENTAGE' OR discount_value <= 100),
    CONSTRAINT product_fixed_discount_check CHECK (discount_type <> 'FIXED' OR discount_value <= base_price)
);

CREATE UNIQUE INDEX idx_product_sku ON products(sku);
CREATE INDEX idx_product_category ON products(category_id);
CREATE INDEX idx_product_brand ON products(brand_id);
CREATE INDEX idx_product_active ON products(active);
CREATE INDEX idx_product_created_at ON products(created_at);
