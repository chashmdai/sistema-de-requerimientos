-- Copia exacta preparada por intento. Los intentos históricos permanecen sin copia acreditada.
CREATE TABLE mail_message_snapshot (
 attempt_id VARCHAR(36) PRIMARY KEY,
 template_version VARCHAR(40) NOT NULL,
 materialized_ms BIGINT NOT NULL,
 trigger_code VARCHAR(16) NOT NULL,
 requested_by VARCHAR(64),
 subject_text VARCHAR(240) NOT NULL,
 body_text VARCHAR(4096) NOT NULL,
 correlation_id VARCHAR(36) NOT NULL,
 content_sha256 CHAR(64) NOT NULL,
 FOREIGN KEY(attempt_id) REFERENCES mail_attempt(id),
 FOREIGN KEY(requested_by) REFERENCES core_actor(id),
 CHECK(template_version='AVISO_INTERNO_LAB_1.0'),
 CHECK((trigger_code='MANUAL' AND requested_by IS NOT NULL) OR (trigger_code='AUTOMATICO' AND requested_by IS NULL)),
 CHECK(materialized_ms>=0)
);
