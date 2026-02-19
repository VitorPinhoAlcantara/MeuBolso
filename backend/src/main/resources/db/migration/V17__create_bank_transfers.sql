CREATE TABLE bank_transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    from_account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    to_account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    amount NUMERIC(14,2) NOT NULL,
    date DATE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_bank_transfers_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_bank_transfers_different_accounts CHECK (from_account_id <> to_account_id)
);

CREATE INDEX idx_bank_transfers_user_id ON bank_transfers(user_id);
CREATE INDEX idx_bank_transfers_from_account_id ON bank_transfers(from_account_id);
CREATE INDEX idx_bank_transfers_to_account_id ON bank_transfers(to_account_id);
