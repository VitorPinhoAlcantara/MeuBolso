CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    account_id UUID NOT NULL,
    category_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(14, 2) NOT NULL,
    date DATE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_transactions_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_transactions_account
        FOREIGN KEY (account_id) REFERENCES accounts(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_transactions_category
        FOREIGN KEY (category_id) REFERENCES categories(id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_transactions_type
        CHECK (type IN ('INCOME', 'EXPENSE')),

    CONSTRAINT ck_transactions_amount_positive
        CHECK (amount > 0)
);

CREATE INDEX idx_transactions_user_date
    ON transactions (user_id, date);

CREATE INDEX idx_transactions_user_category
    ON transactions (user_id, category_id);

CREATE INDEX idx_transactions_user_account
    ON transactions (user_id, account_id);

CREATE INDEX idx_transactions_user_type
    ON transactions (user_id, type);
