ALTER TABLE crm_custom_field
    ADD COLUMN IF NOT EXISTS is_unique SMALLINT DEFAULT 0;

UPDATE crm_custom_field
SET is_unique = 0
WHERE is_unique IS NULL;

COMMENT ON COLUMN crm_custom_field.is_unique IS 'Unique field flag: 0 no, 1 yes';
