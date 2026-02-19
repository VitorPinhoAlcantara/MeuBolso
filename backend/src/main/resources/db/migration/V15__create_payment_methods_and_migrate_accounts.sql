ALTER TABLE accounts
    DROP CONSTRAINT IF EXISTS chk_accounts_card_billing_required;

CREATE TABLE payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    billing_closing_day INTEGER,
    billing_due_day INTEGER,
    credit_limit NUMERIC(14,2),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_payment_methods_type
        CHECK (type IN ('CARD', 'PIX', 'CASH')),
    CONSTRAINT chk_payment_methods_billing_closing_day
        CHECK (billing_closing_day IS NULL OR billing_closing_day BETWEEN 1 AND 31),
    CONSTRAINT chk_payment_methods_billing_due_day
        CHECK (billing_due_day IS NULL OR billing_due_day BETWEEN 1 AND 31),
    CONSTRAINT chk_payment_methods_credit_limit_non_negative
        CHECK (credit_limit IS NULL OR credit_limit >= 0),
    CONSTRAINT chk_payment_methods_card_billing_required
        CHECK (
            (type = 'CARD' AND billing_closing_day IS NOT NULL AND billing_due_day IS NOT NULL)
            OR (type <> 'CARD' AND billing_closing_day IS NULL AND billing_due_day IS NULL)
        )
);

CREATE INDEX idx_payment_methods_user_id ON payment_methods(user_id);
CREATE INDEX idx_payment_methods_account_id ON payment_methods(account_id);
CREATE UNIQUE INDEX uq_payment_methods_account_name ON payment_methods(account_id, lower(name));

INSERT INTO payment_methods (
    id, user_id, account_id, name, type, billing_closing_day, billing_due_day, credit_limit, is_default
)
SELECT
    gen_random_uuid(),
    a.user_id,
    a.id,
    CASE
        WHEN a.type = 'CARD' THEN 'Cartão'
        WHEN a.type = 'CASH' THEN 'Dinheiro'
        WHEN a.type = 'BANK' THEN 'PIX'
        ELSE 'Método padrão'
    END,
    CASE
        WHEN a.type = 'CARD' THEN 'CARD'
        WHEN a.type = 'CASH' THEN 'CASH'
        ELSE 'PIX'
    END,
    CASE WHEN a.type = 'CARD' THEN a.billing_closing_day ELSE NULL END,
    CASE WHEN a.type = 'CARD' THEN a.billing_due_day ELSE NULL END,
    CASE WHEN a.type = 'CARD' THEN a.credit_limit ELSE NULL END,
    TRUE
FROM accounts a;

UPDATE accounts
SET
    type = 'BANK',
    billing_closing_day = NULL,
    billing_due_day = NULL,
    credit_limit = NULL;
