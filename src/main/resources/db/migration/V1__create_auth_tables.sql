CREATE TYPE role_name AS ENUM ('USER', 'ADMIN');
CREATE TYPE auth_provider AS ENUM ('GOOGLE', 'STEAM');

CREATE TABLE users (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email varchar(120) CONSTRAINT uk_users_email UNIQUE,
    password varchar(80),
    nickname varchar(50) NOT NULL,
    age int NOT NULL,
    is_fake boolean NOT NULL DEFAULT (false),
    created_at timestamptz NOT NULL DEFAULT (now())
);

CREATE TABLE roles (
    id int GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name role_name NOT NULL CONSTRAINT uk_roles_name UNIQUE
);

CREATE TABLE users_roles (
    user_id bigint REFERENCES users (id) ON DELETE CASCADE,
    role_id int REFERENCES roles (id) ON DELETE RESTRICT ,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE user_identities (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider auth_provider NOT NULL,
    provider_user_id varchar(255) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT (now()),
    CONSTRAINT uk_user_identities_provider UNIQUE (user_id, provider),
    CONSTRAINT uk_user_identities_account UNIQUE (provider, provider_user_id)
);

CREATE TABLE refresh_tokens (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token_hash varchar(64) NOT NULL UNIQUE,
    user_id bigint NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expire_at timestamptz NOT NULL,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT(now())
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);