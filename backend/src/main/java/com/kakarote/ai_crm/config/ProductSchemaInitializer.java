package com.kakarote.ai_crm.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureProductTables();
            ensureProductMenus();
            removeProductSettingsPermission();
            ensureProductRoleGrants();
            log.info("Ensured product module schema and permissions");
        } catch (Exception exception) {
            log.warn("Failed to ensure product module schema and permissions", exception);
        }
    }

    private void ensureProductTables() {
        jdbcTemplate.execute("""
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
            )
            """);
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_product_category_parent
                ON crm_product_category (tenant_id, parent_id, sort_order, category_id)
                WHERE del_flag = 0
            """);
        jdbcTemplate.execute("""
            CREATE UNIQUE INDEX IF NOT EXISTS uk_product_category_parent_name
                ON crm_product_category (tenant_id, parent_id, category_name)
                WHERE del_flag = 0
            """);
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_product_category_update_time ON crm_product_category");
        jdbcTemplate.execute("""
            CREATE TRIGGER trg_product_category_update_time
                BEFORE UPDATE ON crm_product_category
                FOR EACH ROW EXECUTE FUNCTION update_timestamp()
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS crm_product (
                product_id BIGINT NOT NULL,
                product_name VARCHAR(255) NOT NULL,
                product_code VARCHAR(100),
                main_image VARCHAR(500),
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
            )
            """);
        jdbcTemplate.execute("""
            ALTER TABLE crm_product
                ADD COLUMN IF NOT EXISTS main_image VARCHAR(500)
            """);
        jdbcTemplate.execute("""
            CREATE UNIQUE INDEX IF NOT EXISTS uk_product_tenant_code
                ON crm_product (tenant_id, product_code)
                WHERE del_flag = 0 AND product_code IS NOT NULL AND product_code <> ''
            """);
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_product_owner
                ON crm_product (tenant_id, owner_id, status, update_time DESC)
                WHERE del_flag = 0
            """);
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_product_category
                ON crm_product (tenant_id, category_id)
                WHERE del_flag = 0
            """);
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_product_status
                ON crm_product (tenant_id, status)
                WHERE del_flag = 0
            """);
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_product_update_time ON crm_product");
        jdbcTemplate.execute("""
            CREATE TRIGGER trg_product_update_time
                BEFORE UPDATE ON crm_product
                FOR EACH ROW EXECUTE FUNCTION update_timestamp()
            """);
        jdbcTemplate.execute("""
            ALTER TABLE crm_chat_session
                ADD COLUMN IF NOT EXISTS product_id BIGINT
            """);
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_chat_session_product_id
                ON crm_chat_session (product_id)
            """);
    }

    private void ensureProductMenus() {
        jdbcTemplate.update("""
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
                type = EXCLUDED.type
            """);
    }

    private void ensureProductRoleGrants() {
        jdbcTemplate.update("""
            WITH product_menus AS (
                SELECT menu_id, realm
                FROM manager_menu
                WHERE parent_id = 2900
                  AND type = 5
            ),
            target_roles AS (
                SELECT role_id, realm
                FROM manager_role
                WHERE realm IN ('super_admin', 'default_user')
            ),
            missing_grants AS (
                SELECT
                    r.role_id,
                    r.realm AS role_realm,
                    m.menu_id,
                    m.realm AS menu_realm
                FROM target_roles r
                CROSS JOIN product_menus m
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM manager_role_menu rm
                    WHERE rm.role_id = r.role_id
                      AND rm.menu_id = m.menu_id
                )
            ),
            negative_id_seed AS (
                SELECT COALESCE(MIN(id), 0) - 1 AS start_id
                FROM manager_role_menu
                WHERE id < 0
            ),
            numbered_grants AS (
                SELECT
                    negative_id_seed.start_id - ROW_NUMBER() OVER (ORDER BY role_id, menu_id) + 1 AS id,
                    role_id,
                    menu_id,
                    CASE
                        WHEN role_realm = 'super_admin' AND menu_realm <> 'product:create' THEN 5
                        WHEN menu_realm = 'product:create' THEN NULL
                        ELSE 1
                    END AS data_scope
                FROM missing_grants
                CROSS JOIN negative_id_seed
            )
            INSERT INTO manager_role_menu (id, role_id, menu_id, data_scope, create_time)
            SELECT id, role_id, menu_id, data_scope, CURRENT_TIMESTAMP
            FROM numbered_grants
            """);
    }

    private void removeProductSettingsPermission() {
        jdbcTemplate.update("""
            DELETE FROM manager_role_menu
            WHERE menu_id IN (
                SELECT menu_id
                FROM manager_menu
                WHERE realm = 'product:settings'
            )
            """);
        jdbcTemplate.update("""
            DELETE FROM manager_menu
            WHERE realm = 'product:settings'
               OR menu_id = 2910
            """);
    }
}
