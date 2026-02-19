ALTER TABLE card_invoices
    ADD COLUMN payment_method_id UUID;

UPDATE card_invoices ci
SET payment_method_id = (
    SELECT pm.id
    FROM payment_methods pm
    WHERE pm.user_id = ci.user_id
      AND pm.account_id = ci.account_id
      AND pm.type = 'CARD'
    ORDER BY pm.is_default DESC, pm.created_at ASC
    LIMIT 1
);

UPDATE card_invoices ci
SET payment_method_id = (
    SELECT pm.id
    FROM payment_methods pm
    WHERE pm.user_id = ci.user_id
      AND pm.account_id = ci.account_id
    ORDER BY pm.is_default DESC, pm.created_at ASC
    LIMIT 1
)
WHERE ci.payment_method_id IS NULL;

ALTER TABLE card_invoices
    ALTER COLUMN payment_method_id SET NOT NULL;

ALTER TABLE card_invoices
    ADD CONSTRAINT fk_card_invoices_payment_method
        FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id) ON DELETE CASCADE;

ALTER TABLE card_invoices
    DROP CONSTRAINT IF EXISTS uk_card_invoices_period;

ALTER TABLE card_invoices
    ADD CONSTRAINT uk_card_invoices_period
        UNIQUE (user_id, payment_method_id, period_year, period_month);

CREATE INDEX idx_card_invoices_payment_method ON card_invoices(payment_method_id);

ALTER TABLE transactions
    ADD COLUMN payment_method_id UUID;

UPDATE transactions t
SET payment_method_id = (
    SELECT ci.payment_method_id
    FROM card_invoices ci
    WHERE ci.id = t.invoice_id
)
WHERE t.invoice_id IS NOT NULL;

UPDATE transactions t
SET payment_method_id = (
    SELECT pm.id
    FROM payment_methods pm
    WHERE pm.user_id = t.user_id
      AND pm.account_id = t.account_id
    ORDER BY pm.is_default DESC, pm.created_at ASC
    LIMIT 1
)
WHERE t.payment_method_id IS NULL;

ALTER TABLE transactions
    ALTER COLUMN payment_method_id SET NOT NULL;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_payment_method
        FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id) ON DELETE RESTRICT;

CREATE INDEX idx_transactions_payment_method_id ON transactions(payment_method_id);
