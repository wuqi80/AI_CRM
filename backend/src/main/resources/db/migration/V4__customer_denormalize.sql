-- ============================================
-- V4: 客户表冗余字段 (主联系人信息、联系人数、标签)
-- 优化列表查询性能，消除N+1子查询
-- ============================================

-- 1. 添加冗余字段
ALTER TABLE crm_customer ADD COLUMN IF NOT EXISTS primary_contact_name VARCHAR(100);
ALTER TABLE crm_customer ADD COLUMN IF NOT EXISTS primary_contact_phone VARCHAR(50);
ALTER TABLE crm_customer ADD COLUMN IF NOT EXISTS primary_contact_position VARCHAR(100);
ALTER TABLE crm_customer ADD COLUMN IF NOT EXISTS contact_count INTEGER DEFAULT 0;
ALTER TABLE crm_customer ADD COLUMN IF NOT EXISTS tag_names TEXT DEFAULT '';

COMMENT ON COLUMN crm_customer.primary_contact_name IS '主联系人姓名(冗余)';
COMMENT ON COLUMN crm_customer.primary_contact_phone IS '主联系人电话(冗余)';
COMMENT ON COLUMN crm_customer.primary_contact_position IS '主联系人职位(冗余)';
COMMENT ON COLUMN crm_customer.contact_count IS '联系人数量(冗余)';
COMMENT ON COLUMN crm_customer.tag_names IS '标签名称逗号分隔(冗余)';

-- 2. 从现有数据回填
UPDATE crm_customer c SET
    primary_contact_name = sub.name,
    primary_contact_phone = sub.phone,
    primary_contact_position = sub.position
FROM (
    SELECT DISTINCT ON (customer_id) customer_id, name, phone, position
    FROM crm_contact
    WHERE is_primary = 1 AND status = 1
    ORDER BY customer_id, create_time DESC
) sub
WHERE c.customer_id = sub.customer_id;

UPDATE crm_customer c SET
    contact_count = sub.cnt
FROM (
    SELECT customer_id, COUNT(*) AS cnt
    FROM crm_contact
    WHERE status = 1
    GROUP BY customer_id
) sub
WHERE c.customer_id = sub.customer_id;

UPDATE crm_customer c SET
    tag_names = sub.names
FROM (
    SELECT customer_id, STRING_AGG(tag_name, ',' ORDER BY create_time) AS names
    FROM crm_customer_tag
    GROUP BY customer_id
) sub
WHERE c.customer_id = sub.customer_id;
