INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type)
VALUES
    (2400, 0, 'product', '产品管理', 3),
    (2401, 2400, 'product:create', '新建', 5),
    (2402, 2400, 'product:view', '查看', 5),
    (2403, 2400, 'product:edit', '编辑', 5),
    (2404, 2400, 'product:delete', '删除', 5),
    (2405, 2400, 'product:update_status', '变更状态', 5),
    (2406, 2400, 'product:transfer', '转移', 5),
    (2407, 2400, 'product:import', '导入', 5),
    (2408, 2400, 'product:export', '导出', 5),
    (2409, 2400, 'product:settings', '产品设置', 5),
    (2410, 2400, 'product:category_manage', '分类管理', 5),
    (2500, 0, 'addressBook', '通讯录', 3),
    (2501, 2500, 'addressBook:list', '查看', 5)
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO manager_role_menu (id, role_id, menu_id, data_scope, create_time)
SELECT 2400000000000 + m.menu_id,
       r.role_id,
       m.menu_id,
       5,
       CURRENT_TIMESTAMP
FROM manager_role r
JOIN manager_menu m ON m.menu_id IN (
    2400, 2401, 2402, 2403, 2404, 2405, 2406, 2407, 2408, 2409, 2410,
    2500, 2501
)
WHERE r.realm = 'super_admin'
  AND NOT EXISTS (
      SELECT 1
      FROM manager_role_menu rm
      WHERE rm.role_id = r.role_id
        AND rm.menu_id = m.menu_id
  );
