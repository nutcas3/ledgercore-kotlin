CREATE TABLE accounts (
    id            UUID PRIMARY KEY,
    currency      CHAR(3)   NOT NULL,
    balance_minor BIGINT    NOT NULL DEFAULT 0 CHECK (balance_minor >= 0),
    version       INT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE journal_entries (
    id              UUID PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    status          VARCHAR(16)  NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE postings (
    id           UUID PRIMARY KEY,
    entry_id     UUID       NOT NULL REFERENCES journal_entries (id),
    account_id   UUID       NOT NULL REFERENCES accounts (id),
    direction    VARCHAR(6) NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    amount_minor BIGINT     NOT NULL CHECK (amount_minor > 0),
    currency     CHAR(3)    NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_postings_account ON postings (account_id);
CREATE INDEX idx_postings_entry ON postings (entry_id);
