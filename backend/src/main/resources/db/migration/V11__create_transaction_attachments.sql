CREATE TABLE transaction_attachments (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_transaction_attachments_transaction_id
    ON transaction_attachments(transaction_id);
