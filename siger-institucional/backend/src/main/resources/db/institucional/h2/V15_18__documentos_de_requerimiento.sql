-- Contexto documental REQ explícito. Los documentos anteriores conservan contexto nulo y sus políticas.
ALTER TABLE institution_document ADD COLUMN owner_context VARCHAR(8);
ALTER TABLE institution_document ADD CONSTRAINT ck_document_owner_context CHECK(owner_context IS NULL OR owner_context='REQ');
ALTER TABLE institution_document ADD CONSTRAINT uq_document_owner_context UNIQUE(id,owner_context);
CREATE TABLE requirement_document_policy (
 document_id VARCHAR(36) PRIMARY KEY,
 owner_context VARCHAR(8) NOT NULL,
 requirement_id VARCHAR(36) NOT NULL,
 unit_code VARCHAR(8) NOT NULL,
 site_id VARCHAR(40) NOT NULL,
 area_id VARCHAR(64),
 classification VARCHAR(20) NOT NULL,
 revision BIGINT NOT NULL,
 UNIQUE(document_id,requirement_id),
 FOREIGN KEY(document_id,owner_context) REFERENCES institution_document(id,owner_context),
 FOREIGN KEY(requirement_id) REFERENCES institution_req(requirement_id),
 FOREIGN KEY(unit_code,site_id) REFERENCES core_unit_site(unit_code,site_id),
 FOREIGN KEY(area_id) REFERENCES institution_area(id),
 CHECK(owner_context='REQ'),
 CHECK(classification IN ('SIN_CLASIFICAR','ORDINARIO','FIR','BLOQUEADO')),
 CHECK(revision>=0)
);
CREATE TABLE requirement_document_link (
 document_id VARCHAR(36) NOT NULL,
 requirement_id VARCHAR(36) NOT NULL,
 activity_seq BIGINT,
 context_key BIGINT NOT NULL,
 linked_by VARCHAR(64) NOT NULL,
 linked_ms BIGINT NOT NULL,
 PRIMARY KEY(document_id,requirement_id,context_key),
 FOREIGN KEY(document_id,requirement_id) REFERENCES requirement_document_policy(document_id,requirement_id),
 FOREIGN KEY(activity_seq) REFERENCES institution_req_action(history_seq),
 FOREIGN KEY(activity_seq,requirement_id) REFERENCES institution_flow_history(seq,resource_id),
 FOREIGN KEY(linked_by) REFERENCES core_actor(id),
 CHECK((activity_seq IS NULL AND context_key=0) OR (activity_seq IS NOT NULL AND activity_seq>0 AND context_key=activity_seq))
);
CREATE INDEX ix_req_document_context ON requirement_document_link(requirement_id,document_id);
CREATE TABLE requirement_document_command (
 id VARCHAR(36) PRIMARY KEY,
 actor_id VARCHAR(64) NOT NULL,
 requirement_id VARCHAR(36) NOT NULL,
 operation_code VARCHAR(24) NOT NULL,
 request_hash CHAR(64) NOT NULL,
 document_id VARCHAR(36) NOT NULL,
 created_ms BIGINT NOT NULL,
 FOREIGN KEY(actor_id) REFERENCES core_actor(id),
 FOREIGN KEY(requirement_id) REFERENCES institution_req(requirement_id),
 FOREIGN KEY(document_id) REFERENCES requirement_document_policy(document_id)
);
