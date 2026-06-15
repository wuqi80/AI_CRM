CREATE TABLE IF NOT EXISTS crm_project_attachment (
    attachment_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    file_url TEXT,
    create_user_id BIGINT,
    create_user_name VARCHAR(100),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (attachment_id)
);

CREATE INDEX IF NOT EXISTS idx_crm_project_attachment_project_time
    ON crm_project_attachment(project_id, create_time DESC);

CREATE TABLE IF NOT EXISTS crm_project_schedule (
    schedule_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    schedule_time TIMESTAMP,
    create_user_id BIGINT,
    create_user_name VARCHAR(100),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (schedule_id)
);

CREATE INDEX IF NOT EXISTS idx_crm_project_schedule_project_time
    ON crm_project_schedule(project_id, COALESCE(schedule_time, create_time) DESC);

COMMENT ON TABLE crm_project_attachment IS 'Project attachments';
COMMENT ON TABLE crm_project_schedule IS 'Project schedules';
