CREATE TABLE IF NOT EXISTS crm_external_auth_identity (
    id BIGINT PRIMARY KEY,
    provider VARCHAR(32) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    email VARCHAR(255),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    display_name VARCHAR(100),
    avatar_url VARCHAR(500),
    raw_profile TEXT,
    status INTEGER NOT NULL DEFAULT 1,
    bind_time TIMESTAMP,
    last_login_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_external_auth_provider_subject UNIQUE (provider, subject),
    CONSTRAINT uk_external_auth_provider_user UNIQUE (provider, user_id)
);

CREATE INDEX IF NOT EXISTS idx_external_auth_user_id ON crm_external_auth_identity (user_id);

DROP TRIGGER IF EXISTS trg_external_auth_identity_update_time ON crm_external_auth_identity;
CREATE TRIGGER trg_external_auth_identity_update_time
    BEFORE UPDATE ON crm_external_auth_identity
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();
