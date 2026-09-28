-- Схема БД аукционной площадки (3НФ)
-- categories: справочник категорий (устраняет зависимость category -> lot)
-- sellers, buyers: независимые сущности участников
-- auction_lots: основная сущность, связана с seller и category
-- Все TIMESTAMP трактуются как московское время (МСК, UTC+3).

CREATE TABLE IF NOT EXISTS categories (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS sellers (
    id              SERIAL PRIMARY KEY,
    full_name       VARCHAR(150) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    phone           VARCHAR(30),
    registered_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT sellers_full_name_not_blank CHECK (LENGTH(TRIM(full_name)) > 0)
);

CREATE TABLE IF NOT EXISTS buyers (
    id              SERIAL PRIMARY KEY,
    full_name       VARCHAR(150) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    phone           VARCHAR(30),
    balance         NUMERIC(12, 2) NOT NULL DEFAULT 0,
    registered_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT buyers_full_name_not_blank CHECK (LENGTH(TRIM(full_name)) > 0),
    CONSTRAINT buyers_balance_non_negative CHECK (balance >= 0)
);

CREATE TABLE IF NOT EXISTS auction_lots (
    id              SERIAL PRIMARY KEY,
    seller_id       INTEGER NOT NULL REFERENCES sellers(id) ON DELETE RESTRICT,
    category_id     INTEGER NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    starting_price  NUMERIC(12, 2) NOT NULL,
    current_price   NUMERIC(12, 2) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    criminal_record VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ends_at         TIMESTAMP NOT NULL,
    CONSTRAINT lots_title_not_blank CHECK (LENGTH(TRIM(title)) > 0),
    CONSTRAINT lots_starting_price_positive CHECK (starting_price > 0),
    CONSTRAINT lots_current_price_valid CHECK (current_price >= starting_price),
    CONSTRAINT lots_status_valid CHECK (status IN ('DRAFT', 'ACTIVE', 'SOLD', 'CANCELLED')),
    CONSTRAINT lots_ends_after_created CHECK (ends_at > created_at)
);

CREATE TABLE IF NOT EXISTS balance_topups (
    id          SERIAL PRIMARY KEY,
    buyer_id    INTEGER NOT NULL REFERENCES buyers(id) ON DELETE RESTRICT,
    amount      NUMERIC(12, 2) NOT NULL,
    topup_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    balance_after NUMERIC(12, 2) NOT NULL,
    CONSTRAINT balance_topups_amount_positive CHECK (amount > 0),
    CONSTRAINT balance_topups_balance_after_non_negative CHECK (balance_after >= 0)
);

CREATE TABLE IF NOT EXISTS bids (
    id          SERIAL PRIMARY KEY,
    lot_id      INTEGER NOT NULL REFERENCES auction_lots(id) ON DELETE CASCADE,
    buyer_id    INTEGER NOT NULL REFERENCES buyers(id) ON DELETE RESTRICT,
    amount      NUMERIC(12, 2) NOT NULL,
    bid_time    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT bids_amount_positive CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_lots_seller ON auction_lots(seller_id);
CREATE INDEX IF NOT EXISTS idx_lots_category ON auction_lots(category_id);
CREATE INDEX IF NOT EXISTS idx_lots_status ON auction_lots(status);
CREATE INDEX IF NOT EXISTS idx_bids_lot ON bids(lot_id);
CREATE INDEX IF NOT EXISTS idx_balance_topups_buyer ON balance_topups(buyer_id);
