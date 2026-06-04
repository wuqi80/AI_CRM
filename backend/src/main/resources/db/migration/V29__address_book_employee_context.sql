-- 通讯录员工对象、员工状态、员工会话与员工附件关联

ALTER TABLE manager_user
    ADD COLUMN IF NOT EXISTS employee_status VARCHAR(32) NOT NULL DEFAULT 'active';

UPDATE manager_user
SET employee_status = CASE WHEN status = 0 THEN 'disabled' ELSE 'active' END
WHERE employee_status IS NULL OR employee_status = '';

ALTER TABLE crm_chat_session
    ADD COLUMN IF NOT EXISTS employee_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_chat_session_employee_id
    ON crm_chat_session (employee_id);

ALTER TABLE crm_knowledge
    ADD COLUMN IF NOT EXISTS employee_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_knowledge_employee_id
    ON crm_knowledge (employee_id);

-- ========== 通讯录 (2400) ==========
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type)
VALUES (2400, 0, 'addressBook', '通讯录', 3)
ON CONFLICT (menu_id) DO UPDATE
SET parent_id = EXCLUDED.parent_id,
    realm = EXCLUDED.realm,
    realm_name = EXCLUDED.realm_name,
    type = EXCLUDED.type;

INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type)
VALUES (2401, 2400, 'addressBook:list', '查看列表', 5)
ON CONFLICT (menu_id) DO UPDATE
SET parent_id = EXCLUDED.parent_id,
    realm = EXCLUDED.realm,
    realm_name = EXCLUDED.realm_name,
    type = EXCLUDED.type;

INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type)
VALUES (2402, 2400, 'addressBook:detail', '查看详情', 5)
ON CONFLICT (menu_id) DO UPDATE
SET parent_id = EXCLUDED.parent_id,
    realm = EXCLUDED.realm,
    realm_name = EXCLUDED.realm_name,
    type = EXCLUDED.type;
