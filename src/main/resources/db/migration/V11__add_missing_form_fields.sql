ALTER TABLE forms ADD COLUMN version INTEGER NOT NULL DEFAULT 1;
ALTER TABLE forms ADD COLUMN active BOOLEAN NOT NULL DEFAULT true;

ALTER TABLE form_fields ADD COLUMN section VARCHAR(200);
ALTER TABLE form_fields ADD COLUMN key VARCHAR(200);
ALTER TABLE form_fields ADD COLUMN financial BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE form_fields ADD COLUMN validation_rules TEXT;

CREATE UNIQUE INDEX idx_form_fields_form_id_key ON form_fields(form_id, key)
    WHERE key IS NOT NULL;
