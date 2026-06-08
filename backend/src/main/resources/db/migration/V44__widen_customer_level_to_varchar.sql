-- 放开"客户级别(level)"选项可编辑：把物理列从 CHAR(1) 改宽为 VARCHAR(10)，
-- 以支持用户自定义更长的级别取值（如 VIP / D 级）。
-- 同步更新系统字段元数据的列类型，否则后端选项长度校验仍会按 CHAR(1) 限制为 1 个字符。

ALTER TABLE crm_customer ALTER COLUMN level TYPE VARCHAR(10);

UPDATE crm_custom_field
SET column_type = 'VARCHAR(10)'
WHERE entity_type = 'customer'
  AND field_name = 'level'
  AND (column_type IS NULL OR upper(column_type) = 'CHAR(1)');
