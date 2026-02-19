ALTER TABLE accounts
    ADD COLUMN billing_closing_day INTEGER,
    ADD COLUMN billing_due_day INTEGER,
    ADD COLUMN credit_limit NUMERIC(14,2);

ALTER TABLE accounts
    ADD CONSTRAINT chk_accounts_billing_closing_day
        CHECK (billing_closing_day IS NULL OR billing_closing_day BETWEEN 1 AND 31);

ALTER TABLE accounts
    ADD CONSTRAINT chk_accounts_billing_due_day
        CHECK (billing_due_day IS NULL OR billing_due_day BETWEEN 1 AND 31);

ALTER TABLE accounts
    ADD CONSTRAINT chk_accounts_credit_limit_non_negative
        CHECK (credit_limit IS NULL OR credit_limit >= 0);

ALTER TABLE accounts
    ADD CONSTRAINT chk_accounts_card_billing_required
        CHECK (
            (type = 'CARD' AND billing_closing_day IS NOT NULL AND billing_due_day IS NOT NULL)
            OR (type <> 'CARD' AND billing_closing_day IS NULL AND billing_due_day IS NULL)
        );
