CREATE TABLE communication_registry (
 numero BIGINT AUTO_INCREMENT PRIMARY KEY, comunicacion_id VARCHAR(36) NOT NULL UNIQUE, source VARCHAR(32) NOT NULL,
 operacion_id VARCHAR(36) NOT NULL, registrada_ms BIGINT NOT NULL, canal VARCHAR(40) NOT NULL,
 content_hash CHAR(64) NOT NULL, provenance_hash CHAR(64) NOT NULL, folio_prefix VARCHAR(16) NOT NULL, folio_width INT NOT NULL,
 CONSTRAINT uq_communication_operation UNIQUE(source,operacion_id),
 CONSTRAINT fk_communication_inbox FOREIGN KEY(source,operacion_id) REFERENCES institutional_inbox(source,id)) ENGINE=InnoDB;
CREATE INDEX ix_communication_date ON communication_registry(registrada_ms,numero);
ALTER TABLE institutional_inbox ADD COLUMN legacy_receipt TEXT;
ALTER TABLE institutional_inbox ADD COLUMN ack_lease VARCHAR(36);
UPDATE institutional_inbox SET legacy_receipt=receipt;
