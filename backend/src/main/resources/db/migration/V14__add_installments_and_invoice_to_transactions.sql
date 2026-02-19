ALTER TABLE transactions
    ADD COLUMN purchase_date DATE,
    ADD COLUMN installment_group_id UUID,
    ADD COLUMN installment_number INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN installment_total INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN invoice_id UUID REFERENCES card_invoices(id) ON DELETE SET NULL;

UPDATE transactions
SET purchase_date = date
WHERE purchase_date IS NULL;

ALTER TABLE transactions
    ALTER COLUMN purchase_date SET NOT NULL;

ALTER TABLE transactions
    ADD CONSTRAINT chk_transactions_installment_number_positive
        CHECK (installment_number >= 1);

ALTER TABLE transactions
    ADD CONSTRAINT chk_transactions_installment_total_positive
        CHECK (installment_total >= 1);

ALTER TABLE transactions
    ADD CONSTRAINT chk_transactions_installment_number_total
        CHECK (installment_number <= installment_total);

CREATE INDEX idx_transactions_installment_group_id ON transactions(installment_group_id);
CREATE INDEX idx_transactions_invoice_id ON transactions(invoice_id);
