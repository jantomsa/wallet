CREATE TABLE IF NOT EXISTS wallet
(
    id       UUID           NOT NULL primary key,
    currency VARCHAR(3)     NOT NULL,
    amount   DECIMAL(19, 4) NOT NULL,
    name     VARCHAR(50)    NOT NULL,
    user_id  UUID           NOT NULL
);
