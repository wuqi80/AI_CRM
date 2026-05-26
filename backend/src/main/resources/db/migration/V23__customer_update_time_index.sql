CREATE INDEX IF NOT EXISTS idx_customer_update_time
    ON crm_customer (tenant_id, update_time DESC, create_time DESC);
