-- Proyección explícita de asociaciones. No modifica originales ni vínculos históricos.
CREATE TABLE document_association_control (
 document_id VARCHAR(36) NOT NULL,
 owner_kind VARCHAR(8) NOT NULL,
 owner_resource VARCHAR(36) NOT NULL,
 revision BIGINT NOT NULL,
 state VARCHAR(12) NOT NULL,
 PRIMARY KEY(document_id,owner_kind,owner_resource),
 FOREIGN KEY(document_id) REFERENCES institution_document(id),
 CHECK(owner_kind IN ('CASO','REQ')),
 CHECK(state IN ('ACTIVA','RETIRADA')),
 CHECK(revision>=0)
);
CREATE TABLE document_association_target (
 document_id VARCHAR(36) NOT NULL,
 owner_kind VARCHAR(8) NOT NULL,
 owner_resource VARCHAR(36) NOT NULL,
 context_key VARCHAR(36) NOT NULL,
 PRIMARY KEY(document_id,owner_kind,owner_resource,context_key),
 FOREIGN KEY(document_id,owner_kind,owner_resource) REFERENCES document_association_control(document_id,owner_kind,owner_resource)
);
CREATE TABLE document_association_history (
 seq BIGINT AUTO_INCREMENT PRIMARY KEY,
 document_id VARCHAR(36) NOT NULL,
 owner_kind VARCHAR(8) NOT NULL,
 owner_resource VARCHAR(36) NOT NULL,
 revision BIGINT NOT NULL,
 actor_id VARCHAR(64) NOT NULL,
 at_ms BIGINT NOT NULL,
 reason_text VARCHAR(2000) NOT NULL,
 previous_doc LONGTEXT NOT NULL,
 current_doc LONGTEXT NOT NULL,
 UNIQUE(document_id,owner_kind,owner_resource,revision),
 FOREIGN KEY(document_id,owner_kind,owner_resource) REFERENCES document_association_control(document_id,owner_kind,owner_resource),
 FOREIGN KEY(actor_id) REFERENCES core_actor(id)
);
CREATE TABLE document_association_command (
 id VARCHAR(36) PRIMARY KEY,
 actor_id VARCHAR(64) NOT NULL,
 document_id VARCHAR(36) NOT NULL,
 owner_kind VARCHAR(8) NOT NULL,
 owner_resource VARCHAR(36) NOT NULL,
 request_hash CHAR(64) NOT NULL,
 FOREIGN KEY(document_id,owner_kind,owner_resource) REFERENCES document_association_control(document_id,owner_kind,owner_resource),
 FOREIGN KEY(actor_id) REFERENCES core_actor(id)
);
