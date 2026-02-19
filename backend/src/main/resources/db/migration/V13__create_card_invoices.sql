CREATE TABLE card_invoices (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    period_year INTEGER NOT NULL,
    period_month INTEGER NOT NULL,
    closing_date DATE NOT NULL,
    due_date DATE NOT NULL,
    total_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE card_invoices
    ADD CONSTRAINT uk_card_invoices_period
        UNIQUE (user_id, account_id, period_year, period_month);

ALTER TABLE card_invoices
    ADD CONSTRAINT chk_card_invoices_period_month
        CHECK (period_month BETWEEN 1 AND 12);

CREATE INDEX idx_card_invoices_user ON card_invoices(user_id);
CREATE INDEX idx_card_invoices_account ON card_invoices(account_id);
