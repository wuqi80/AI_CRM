-- 部门表
CREATE TABLE IF NOT EXISTS manager_dept (
    dept_id    BIGINT NOT NULL,
    dept_name  VARCHAR(100) NOT NULL,
    parent_id  BIGINT DEFAULT 0,
    sort_order INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (dept_id)
);

CREATE INDEX IF NOT EXISTS idx_dept_parent_id ON manager_dept (parent_id);

COMMENT ON TABLE manager_dept IS '部门表';
COMMENT ON COLUMN manager_dept.dept_id IS '部门ID';
COMMENT ON COLUMN manager_dept.dept_name IS '部门名称';
COMMENT ON COLUMN manager_dept.parent_id IS '上级部门ID，0为根部门';
COMMENT ON COLUMN manager_dept.sort_order IS '排序号';
COMMENT ON COLUMN manager_dept.create_time IS '创建时间';

INSERT INTO "manager_dept" ("dept_id", "dept_name", "parent_id", "sort_order", "create_time") VALUES (2027208167634780162, '总经办', 0, 0, '2026-02-27 10:24:25.521');

-- 修改 manager_user.dept_id 为 BIGINT，与 manager_dept.dept_id 类型一致
ALTER TABLE manager_user ALTER COLUMN dept_id TYPE BIGINT;

-- 将所有用户默认分配到总经办
UPDATE "manager_user" SET "dept_id" = 2027208167634780162;
