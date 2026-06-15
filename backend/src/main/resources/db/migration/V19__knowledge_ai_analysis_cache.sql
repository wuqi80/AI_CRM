ALTER TABLE crm_knowledge
    ADD COLUMN IF NOT EXISTS ai_analysis_snapshot TEXT;

ALTER TABLE crm_knowledge
    ADD COLUMN IF NOT EXISTS ai_analysis_time TIMESTAMP(3);

COMMENT ON COLUMN crm_knowledge.ai_analysis_snapshot IS 'AI analysis result snapshot';
COMMENT ON COLUMN crm_knowledge.ai_analysis_time IS 'AI analysis completion time';
