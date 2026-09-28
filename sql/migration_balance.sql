-- Миграция: баланс покупателей и история пополнений
ALTER TABLE buyers ADD COLUMN IF NOT EXISTS balance NUMERIC(12, 2) NOT NULL DEFAULT 0;
ALTER TABLE buyers DROP CONSTRAINT IF EXISTS buyers_balance_non_negative;
ALTER TABLE buyers ADD CONSTRAINT buyers_balance_non_negative CHECK (balance >= 0);

CREATE TABLE IF NOT EXISTS balance_topups (
    id            SERIAL PRIMARY KEY,
    buyer_id      INTEGER NOT NULL REFERENCES buyers(id) ON DELETE RESTRICT,
    amount        NUMERIC(12, 2) NOT NULL,
    topup_time    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    balance_after NUMERIC(12, 2) NOT NULL,
    CONSTRAINT balance_topups_amount_positive CHECK (amount > 0),
    CONSTRAINT balance_topups_balance_after_non_negative CHECK (balance_after >= 0)
);

CREATE INDEX IF NOT EXISTS idx_balance_topups_buyer ON balance_topups(buyer_id);

UPDATE buyers SET balance = 500000 WHERE balance = 0;
