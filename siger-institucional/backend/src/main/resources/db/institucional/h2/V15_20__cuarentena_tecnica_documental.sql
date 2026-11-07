-- Aislar no elimina, modifica o concede lectura del original.
CREATE TABLE document_technical_hold (
 id VARCHAR(36) PRIMARY KEY,
 document_id VARCHAR(36) NOT NULL,
 active_document_id VARCHAR(36) UNIQUE,
 revision BIGINT NOT NULL,
 state VARCHAR(12) NOT NULL,
 source_sha256 CHAR(64) NOT NULL,
 held_by VARCHAR(64) NOT NULL,
 held_person VARCHAR(64) NOT NULL,
 held_ms BIGINT NOT NULL,
 reason_text VARCHAR(2000) NOT NULL,
 incident_reference VARCHAR(500) NOT NULL,
 released_by VARCHAR(64),
 released_person VARCHAR(64),
 released_ms BIGINT,
 release_reason VARCHAR(2000),
 verification_profile VARCHAR(64),
 FOREIGN KEY(document_id) REFERENCES institution_document(id),
 FOREIGN KEY(active_document_id) REFERENCES institution_document(id),
 FOREIGN KEY(held_by) REFERENCES core_actor(id),
 FOREIGN KEY(released_by) REFERENCES core_actor(id),
 CHECK(revision>=0),
 CHECK(state IN ('RETENIDO','LIBERADO')),
 CHECK((state='RETENIDO' AND active_document_id IS NOT NULL AND active_document_id=document_id AND released_by IS NULL AND released_person IS NULL AND released_ms IS NULL AND release_reason IS NULL AND verification_profile IS NULL) OR (state='LIBERADO' AND active_document_id IS NULL AND released_by IS NOT NULL AND released_person IS NOT NULL AND released_person<>held_person AND released_ms IS NOT NULL AND release_reason IS NOT NULL AND verification_profile IS NOT NULL))
);
CREATE INDEX ix_technical_document_state ON document_technical_hold(document_id,state);
CREATE TABLE document_technical_history (
 seq BIGINT AUTO_INCREMENT PRIMARY KEY,
 hold_id VARCHAR(36),
 actor_id VARCHAR(64) NOT NULL,
 at_ms BIGINT NOT NULL,
 event_code VARCHAR(32) NOT NULL,
 reason_text VARCHAR(2000),
 FOREIGN KEY(hold_id) REFERENCES document_technical_hold(id),
 FOREIGN KEY(actor_id) REFERENCES core_actor(id)
);
CREATE TABLE document_technical_command (
 id VARCHAR(36) PRIMARY KEY,
 actor_id VARCHAR(64) NOT NULL,
 hold_id VARCHAR(36) NOT NULL,
 operation_code VARCHAR(16) NOT NULL,
 request_hash CHAR(64) NOT NULL,
 FOREIGN KEY(actor_id) REFERENCES core_actor(id),
 FOREIGN KEY(hold_id) REFERENCES document_technical_hold(id)
);
