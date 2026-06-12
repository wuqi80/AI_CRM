UPDATE crm_custom_field
SET field_type = 'select',
    placeholder = '请选择单位',
    options = '[{"value":"个","label":"个"},{"value":"套","label":"套"},{"value":"台","label":"台"},{"value":"件","label":"件"},{"value":"年","label":"年"},{"value":"月","label":"月"},{"value":"次","label":"次"}]'
WHERE entity_type = 'product'
  AND field_name = 'unit'
  AND COALESCE(field_source, 'system') = 'system';
