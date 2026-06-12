CREATE TABLE IF NOT EXISTS crm_product_category (
    category_id BIGINT NOT NULL,
    parent_id BIGINT NOT NULL DEFAULT 0,
    category_name VARCHAR(100) NOT NULL,
    category_path VARCHAR(500) NOT NULL,
    level SMALLINT NOT NULL DEFAULT 1,
    sort_order INTEGER NOT NULL DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1,
    del_flag SMALLINT NOT NULL DEFAULT 0,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT,
    PRIMARY KEY (category_id)
);

CREATE INDEX IF NOT EXISTS idx_product_category_parent
    ON crm_product_category (tenant_id, parent_id, sort_order, category_id)
    WHERE del_flag = 0;

CREATE UNIQUE INDEX IF NOT EXISTS uk_product_category_parent_name
    ON crm_product_category (tenant_id, parent_id, category_name)
    WHERE del_flag = 0;

DROP TRIGGER IF EXISTS trg_product_category_update_time ON crm_product_category;
CREATE TRIGGER trg_product_category_update_time
    BEFORE UPDATE ON crm_product_category
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_product (
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_code VARCHAR(100),
    category_id BIGINT,
    product_type VARCHAR(50) NOT NULL DEFAULT 'goods',
    unit VARCHAR(50),
    standard_price NUMERIC(18, 2),
    cost_price NUMERIC(18, 2),
    owner_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    description TEXT,
    del_flag SMALLINT NOT NULL DEFAULT 0,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT,
    PRIMARY KEY (product_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_product_tenant_code
    ON crm_product (tenant_id, product_code)
    WHERE del_flag = 0 AND product_code IS NOT NULL AND product_code <> '';

CREATE INDEX IF NOT EXISTS idx_product_owner
    ON crm_product (tenant_id, owner_id, status, update_time DESC)
    WHERE del_flag = 0;

CREATE INDEX IF NOT EXISTS idx_product_category
    ON crm_product (tenant_id, category_id)
    WHERE del_flag = 0;

CREATE INDEX IF NOT EXISTS idx_product_status
    ON crm_product (tenant_id, status)
    WHERE del_flag = 0;

DROP TRIGGER IF EXISTS trg_product_update_time ON crm_product;
CREATE TRIGGER trg_product_update_time
    BEFORE UPDATE ON crm_product
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

ALTER TABLE crm_chat_session
    ADD COLUMN IF NOT EXISTS product_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_chat_session_product_id
    ON crm_chat_session (product_id);

INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type)
VALUES
    (2900, 0, 'product', '产品管理', 3),
    (2901, 2900, 'product:view', '查看', 5),
    (2902, 2900, 'product:create', '新建', 5),
    (2903, 2900, 'product:edit', '编辑', 5),
    (2904, 2900, 'product:delete', '删除', 5),
    (2905, 2900, 'product:update_status', '启用/停用', 5),
    (2906, 2900, 'product:transfer', '转移', 5),
    (2907, 2900, 'product:import', '导入', 5),
    (2908, 2900, 'product:export', '导出', 5),
    (2909, 2900, 'product:category_manage', '类目管理', 5)
ON CONFLICT (menu_id) DO UPDATE
SET parent_id = EXCLUDED.parent_id,
    realm = EXCLUDED.realm,
    realm_name = EXCLUDED.realm_name,
    type = EXCLUDED.type;

COMMENT ON TABLE crm_product IS '产品资料库';
COMMENT ON TABLE crm_product_category IS '产品类目';
COMMENT ON COLUMN crm_product.status IS 'active=启用, inactive=停用';
COMMENT ON COLUMN crm_chat_session.product_id IS '产品对象会话关联的产品ID';
