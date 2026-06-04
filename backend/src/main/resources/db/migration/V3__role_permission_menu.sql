-- 1. manager_role_menu 加 data_scope 列（存储数据范围 1-5）
ALTER TABLE manager_role_menu ADD COLUMN IF NOT EXISTS data_scope INTEGER;

-- 2. 清空旧数据
DELETE FROM manager_role_menu;
DELETE FROM manager_menu;

-- 3. 按系统功能插入完整菜单数据
--    模块: type=3, parentId=0
--    操作: type=5, parentId=模块ID

-- ========== 客户管理 (1000) ==========
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1000, 0, 'customer', '客户管理', 3);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1001, 1000, 'customer:create', '新建', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1002, 1000, 'customer:view', '查看', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1003, 1000, 'customer:edit', '编辑', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1004, 1000, 'customer:delete', '删除', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1005, 1000, 'customer:change_stage', '变更阶段', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1006, 1000, 'customer:transfer', '转移', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1007, 1000, 'customer:import', '导入', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1008, 1000, 'customer:export', '导出', 5);

-- ========== 联系人管理 (1100) ==========
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1100, 0, 'contact', '联系人管理', 3);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1101, 1100, 'contact:create', '新建', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1102, 1100, 'contact:view', '查看', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1103, 1100, 'contact:edit', '编辑', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1104, 1100, 'contact:delete', '删除', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1105, 1100, 'contact:set_primary', '设为主要联系人', 5);

-- ========== 任务管理 (1200) ==========
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1200, 0, 'task', '任务管理', 3);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1201, 1200, 'task:create', '新建', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1202, 1200, 'task:view', '查看', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1203, 1200, 'task:edit', '编辑', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1204, 1200, 'task:delete', '删除', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1205, 1200, 'task:update_status', '变更状态', 5);

-- ========== 跟进记录 (1300) ==========
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1300, 0, 'followup', '跟进记录', 3);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1301, 1300, 'followup:create', '新建', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1302, 1300, 'followup:view', '查看', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1303, 1300, 'followup:delete', '删除', 5);

-- ========== 知识库 (1400) ==========
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1400, 0, 'knowledge', '知识库', 3);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1401, 1400, 'knowledge:upload', '上传', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1402, 1400, 'knowledge:view', '查看', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1403, 1400, 'knowledge:delete', '删除', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1404, 1400, 'knowledge:download', '下载', 5);

-- ========== AI 对话 (1500) ==========
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1500, 0, 'chat', 'AI 对话', 3);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1501, 1500, 'chat:session', '会话管理', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1502, 1500, 'chat:send', '发送消息', 5);

-- ========== AI Agent (1600) ==========
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1600, 0, 'agent', 'AI Agent', 3);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1601, 1600, 'agent:create', '新建', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1602, 1600, 'agent:edit', '编辑', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1603, 1600, 'agent:delete', '删除', 5);

-- ========== 用户管理 (1700) ==========
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1700, 0, 'user', '用户管理', 3);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1701, 1700, 'user:create', '新建', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1702, 1700, 'user:edit', '编辑', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1703, 1700, 'user:delete', '删除', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1704, 1700, 'user:status', '启用/禁用', 5);

-- ========== 部门管理 (1800) ==========
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1800, 0, 'dept', '部门管理', 3);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1801, 1800, 'dept:create', '新建', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1802, 1800, 'dept:edit', '编辑', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1803, 1800, 'dept:delete', '删除', 5);

-- ========== 角色管理 (1900) ==========
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1900, 0, 'role', '角色管理', 3);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1901, 1900, 'role:create', '新建', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1902, 1900, 'role:edit', '编辑', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1903, 1900, 'role:delete', '删除', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1904, 1900, 'role:permission', '分配权限', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (1905, 1900, 'role:user', '分配用户', 5);

-- ========== 系统配置 (2000) ==========
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (2000, 0, 'config', '系统配置', 3);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (2001, 2000, 'config:ai', 'AI 配置', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (2002, 2000, 'config:storage', '存储配置', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (2003, 2000, 'config:knowledge', '知识库配置', 5);

-- ========== 自定义字段 (2100) ==========
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (2100, 0, 'customField', '自定义字段', 3);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (2101, 2100, 'customField:create', '新建', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (2102, 2100, 'customField:edit', '编辑', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (2103, 2100, 'customField:delete', '删除', 5);
