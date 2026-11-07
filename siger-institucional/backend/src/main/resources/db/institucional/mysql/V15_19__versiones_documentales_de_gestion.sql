-- Declaraciones explícitas; no asigna fases o versiones a originales históricos.
CREATE TABLE management_document_metadata (
 document_id VARCHAR(36) PRIMARY KEY,
 owner_kind VARCHAR(8) NOT NULL,
 owner_resource VARCHAR(36) NOT NULL,
 revision BIGINT NOT NULL,
 version_number BIGINT NOT NULL,
 title_text VARCHAR(180) NOT NULL,
 type_text VARCHAR(120) NOT NULL,
 phase_code VARCHAR(12) NOT NULL,
 external_reference VARCHAR(500),
 finalized_ms BIGINT,
 finalized_by VARCHAR(64),
 FOREIGN KEY(document_id) REFERENCES institution_document(id),
 FOREIGN KEY(finalized_by) REFERENCES core_actor(id),
 CHECK(owner_kind IN ('CASO','REQ')),
 CHECK(revision>=0 AND version_number>0),
 CHECK(phase_code IN ('BORRADOR','FINAL')),
 CHECK((phase_code='BORRADOR' AND finalized_ms IS NULL AND finalized_by IS NULL AND external_reference IS NULL) OR (phase_code='FINAL' AND finalized_ms IS NOT NULL AND finalized_by IS NOT NULL))
);
CREATE TABLE management_document_metadata_history (
 seq BIGINT AUTO_INCREMENT PRIMARY KEY,
 document_id VARCHAR(36) NOT NULL,
 revision BIGINT NOT NULL,
 actor_id VARCHAR(64) NOT NULL,
 at_ms BIGINT NOT NULL,
 event_code VARCHAR(32) NOT NULL,
 reason_text VARCHAR(2000) NOT NULL,
 snapshot_doc LONGTEXT NOT NULL,
 UNIQUE(document_id,revision),
 FOREIGN KEY(document_id) REFERENCES management_document_metadata(document_id),
 FOREIGN KEY(actor_id) REFERENCES core_actor(id)
);
CREATE TABLE management_document_successor (
 document_id VARCHAR(36) PRIMARY KEY,
 predecessor_id VARCHAR(36) NOT NULL UNIQUE,
 actor_id VARCHAR(64) NOT NULL,
 at_ms BIGINT NOT NULL,
 reason_text VARCHAR(2000) NOT NULL,
 FOREIGN KEY(document_id) REFERENCES management_document_metadata(document_id),
 FOREIGN KEY(predecessor_id) REFERENCES management_document_metadata(document_id),
 FOREIGN KEY(actor_id) REFERENCES core_actor(id),
 CHECK(document_id<>predecessor_id)
);
CREATE TABLE management_document_metadata_command (
 id VARCHAR(36) PRIMARY KEY,
 actor_id VARCHAR(64) NOT NULL,
 owner_kind VARCHAR(8) NOT NULL,
 owner_resource VARCHAR(36) NOT NULL,
 document_id VARCHAR(36) NOT NULL,
 request_hash CHAR(64) NOT NULL,
 FOREIGN KEY(actor_id) REFERENCES core_actor(id),
 FOREIGN KEY(document_id) REFERENCES management_document_metadata(document_id),
 CHECK(owner_kind IN ('CASO','REQ'))
);
