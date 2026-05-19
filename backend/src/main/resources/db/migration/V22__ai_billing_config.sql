CREATE TABLE IF NOT EXISTS crm_ai_billing_config (
    config_key VARCHAR(64) PRIMARY KEY,
    tokens_per_credit INTEGER NOT NULL DEFAULT 800,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_ai_billing_tokens_per_credit_positive CHECK (tokens_per_credit > 0)
);

COMMENT ON TABLE crm_ai_billing_config IS 'AI billing conversion config';
COMMENT ON COLUMN crm_ai_billing_config.config_key IS 'Config key';
COMMENT ON COLUMN crm_ai_billing_config.tokens_per_credit IS 'Tokens covered by one credit at 1x model multiplier';

INSERT INTO crm_ai_billing_config (config_key, tokens_per_credit)
VALUES ('default', 800)
ON CONFLICT (config_key) DO NOTHING;
