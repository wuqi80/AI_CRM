ALTER TABLE manager_user
    ADD COLUMN IF NOT EXISTS employee_status VARCHAR(32);

UPDATE manager_user
SET employee_status = CASE
    WHEN status = 0 THEN 'disabled'
    WHEN status = 2 THEN 'resigned'
    ELSE 'active'
END
WHERE employee_status IS NULL OR employee_status = '';

ALTER TABLE manager_user
    ALTER COLUMN employee_status SET DEFAULT 'active';

ALTER TABLE manager_user
    ALTER COLUMN employee_status SET NOT NULL;

ALTER TABLE manager_user
    ADD COLUMN IF NOT EXISTS ui_preferences TEXT;

COMMENT ON COLUMN manager_user.employee_status IS 'Employee status: active, resigned, disabled';
COMMENT ON COLUMN manager_user.ui_preferences IS 'User UI preferences JSON';
