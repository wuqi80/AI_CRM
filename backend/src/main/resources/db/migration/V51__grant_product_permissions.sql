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
FROM numbered_grants;
