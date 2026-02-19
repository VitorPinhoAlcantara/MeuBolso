ALTER TABLE card_invoices
    ADD COLUMN paid_from_account_id UUID,
    ADD COLUMN paid_at DATE;

ALTER TABLE card_invoices
    ADD CONSTRAINT fk_card_invoices_paid_from_account
        FOREIGN KEY (paid_from_account_id) REFERENCES accounts(id) ON DELETE SET NULL;

CREATE INDEX idx_card_invoices_paid_from_account ON card_invoices(paid_from_account_id);
