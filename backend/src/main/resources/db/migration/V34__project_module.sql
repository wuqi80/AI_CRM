ALTER TABLE crm_task
    ADD COLUMN IF NOT EXISTS project_id BIGINT,
    ADD COLUMN IF NOT EXISTS lane_id BIGINT,
    ADD COLUMN IF NOT EXISTS participant_user_ids TEXT,
    ADD COLUMN IF NOT EXISTS source VARCHAR(32),
    ADD COLUMN IF NOT EXISTS ai_source_text TEXT,
    ADD COLUMN IF NOT EXISTS has_attachments BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS has_schedule BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_crm_task_project_id ON crm_task(project_id);
CREATE INDEX IF NOT EXISTS idx_crm_task_lane_id ON crm_task(lane_id);

CREATE TABLE IF NOT EXISTS crm_project (
    project_id BIGINT PRIMARY KEY,
    tenant_id BIGINT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED',
    owner_id BIGINT,
    customer_id BIGINT,
    customer_name VARCHAR(255),
    start_date TIMESTAMP,
    due_date TIMESTAMP,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE crm_project
    ADD COLUMN IF NOT EXISTS customer_name VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_crm_project_tenant_update_time ON crm_project(tenant_id, update_time DESC);
CREATE INDEX IF NOT EXISTS idx_crm_project_owner_id ON crm_project(owner_id);
CREATE INDEX IF NOT EXISTS idx_crm_project_customer_id ON crm_project(customer_id);

CREATE TABLE IF NOT EXISTS crm_project_lane (
    lane_id BIGINT PRIMARY KEY,
    tenant_id BIGINT,
    project_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(64),
    sort_order INTEGER NOT NULL DEFAULT 0,
    system_flag BOOLEAN NOT NULL DEFAULT FALSE,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_crm_project_lane_project_order ON crm_project_lane(project_id, sort_order);

CREATE TABLE IF NOT EXISTS crm_project_member (
    member_id BIGINT PRIMARY KEY,
    tenant_id BIGINT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    member_name VARCHAR(100) NOT NULL,
    account VARCHAR(100),
    role VARCHAR(32) NOT NULL,
    dept_name VARCHAR(100),
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_action_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    permissions TEXT,
    remark TEXT,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_project_member_user ON crm_project_member(project_id, user_id);
CREATE INDEX IF NOT EXISTS idx_crm_project_member_user_status ON crm_project_member(user_id, status);

CREATE TABLE IF NOT EXISTS crm_project_member_log (
    log_id BIGINT PRIMARY KEY,
    tenant_id BIGINT,
    project_id BIGINT NOT NULL,
    operator_id BIGINT,
    operator_name VARCHAR(100),
    action_type VARCHAR(32) NOT NULL,
    target_user_id BIGINT,
    target_user_name VARCHAR(100),
    before_summary TEXT,
    after_summary TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_crm_project_member_log_project_time ON crm_project_member_log(project_id, create_time DESC);

CREATE TABLE IF NOT EXISTS crm_project_chat_message (
    message_id BIGINT PRIMARY KEY,
    tenant_id BIGINT,
    project_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    create_user_id BIGINT,
    create_user_name VARCHAR(100),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_crm_project_chat_project_time ON crm_project_chat_message(project_id, create_time);

CREATE TABLE IF NOT EXISTS crm_project_task_chat_message (
    message_id BIGINT PRIMARY KEY,
    tenant_id BIGINT,
    project_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    create_user_id BIGINT,
    create_user_name VARCHAR(100),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_crm_project_task_chat_task_time ON crm_project_task_chat_message(task_id, create_time);

CREATE TABLE IF NOT EXISTS crm_project_task_note (
    note_id BIGINT PRIMARY KEY,
    tenant_id BIGINT,
    project_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    create_user_id BIGINT,
    create_user_name VARCHAR(100),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_crm_project_task_note_task_time ON crm_project_task_note(task_id, create_time DESC);

CREATE TABLE IF NOT EXISTS crm_project_task_attachment (
    attachment_id BIGINT PRIMARY KEY,
    tenant_id BIGINT,
    project_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    file_url TEXT,
    create_user_id BIGINT,
    create_user_name VARCHAR(100),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_crm_project_task_attachment_task ON crm_project_task_attachment(task_id);

CREATE TABLE IF NOT EXISTS crm_project_task_schedule (
    schedule_id BIGINT PRIMARY KEY,
    tenant_id BIGINT,
    project_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    schedule_time TIMESTAMP,
    create_user_id BIGINT,
    create_user_name VARCHAR(100),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_crm_project_task_schedule_task_time ON crm_project_task_schedule(task_id, schedule_time);
