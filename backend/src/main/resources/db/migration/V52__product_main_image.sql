ALTER TABLE crm_product
    ADD COLUMN IF NOT EXISTS main_image VARCHAR(500);

COMMENT ON COLUMN crm_product.main_image IS '产品主图存储路径';
