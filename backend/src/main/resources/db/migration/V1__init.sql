-- All monetary values are stored as integer cents to guarantee exact arithmetic.

CREATE TABLE expense_group (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL CHECK (length(trim(name)) > 0),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE member (
    id         BIGSERIAL PRIMARY KEY,
    group_id   BIGINT       NOT NULL REFERENCES expense_group (id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL CHECK (length(trim(name)) > 0),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_member_group_name ON member (group_id, lower(name));

CREATE TABLE expense (
    id           BIGSERIAL PRIMARY KEY,
    group_id     BIGINT       NOT NULL REFERENCES expense_group (id) ON DELETE CASCADE,
    description  VARCHAR(255) NOT NULL CHECK (length(trim(description)) > 0),
    amount_cents BIGINT       NOT NULL CHECK (amount_cents > 0),
    paid_by      BIGINT       NOT NULL REFERENCES member (id),
    split_type   VARCHAR(10)  NOT NULL CHECK (split_type IN ('EQUAL', 'EXACT')),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_expense_group ON expense (group_id);

CREATE TABLE expense_split (
    expense_id  BIGINT NOT NULL REFERENCES expense (id) ON DELETE CASCADE,
    member_id   BIGINT NOT NULL REFERENCES member (id),
    share_cents BIGINT NOT NULL CHECK (share_cents >= 0),
    PRIMARY KEY (expense_id, member_id)
);

CREATE TABLE settlement (
    id           BIGSERIAL PRIMARY KEY,
    group_id     BIGINT      NOT NULL REFERENCES expense_group (id) ON DELETE CASCADE,
    from_member  BIGINT      NOT NULL REFERENCES member (id),
    to_member    BIGINT      NOT NULL REFERENCES member (id),
    amount_cents BIGINT      NOT NULL CHECK (amount_cents > 0),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (from_member <> to_member)
);

CREATE INDEX idx_settlement_group ON settlement (group_id);
