-- Acreditaciones y decisiones nuevas; no se clasifican ni publican recursos históricos.
CREATE TABLE authority_requirement_principal (
 batch_revision BIGINT NOT NULL, requirement_id VARCHAR(36) NOT NULL, unit_code VARCHAR(8) NOT NULL, site_id VARCHAR(40) NOT NULL, area_id VARCHAR(64),
 valid_from BIGINT NOT NULL, valid_until BIGINT NOT NULL, status VARCHAR(16) NOT NULL, basis_ref VARCHAR(180) NOT NULL, basis_sha256 CHAR(64) NOT NULL,
 PRIMARY KEY(requirement_id,batch_revision), FOREIGN KEY(batch_revision) REFERENCES authority_batch(revision), FOREIGN KEY(requirement_id) REFERENCES core_requirement(id),
 FOREIGN KEY(unit_code,site_id) REFERENCES core_unit_site(unit_code,site_id), FOREIGN KEY(area_id) REFERENCES institution_area(id),
 CHECK(valid_until>valid_from), CHECK(status IN ('ACTIVA','SUSPENDIDA'))
);
CREATE TABLE communication_pertinence (
 child_id VARCHAR(36) PRIMARY KEY, revision BIGINT NOT NULL, content_allowed BOOLEAN NOT NULL, actor_id VARCHAR(64) NOT NULL, at_ms BIGINT NOT NULL, reason_text VARCHAR(1000) NOT NULL,
 FOREIGN KEY(child_id) REFERENCES core_link(child_id), FOREIGN KEY(actor_id) REFERENCES core_actor(id)
);
CREATE TABLE communication_pertinence_target (
 child_id VARCHAR(36) NOT NULL, case_id VARCHAR(36) NOT NULL, matter_id VARCHAR(64), area_seq BIGINT NOT NULL, matter_seq BIGINT NOT NULL,
 PRIMARY KEY(child_id,case_id), FOREIGN KEY(child_id) REFERENCES communication_pertinence(child_id), FOREIGN KEY(case_id) REFERENCES core_case(id), FOREIGN KEY(matter_id) REFERENCES institution_matter(id)
);
CREATE TABLE communication_pertinence_history (
 seq BIGINT AUTO_INCREMENT PRIMARY KEY, child_id VARCHAR(36) NOT NULL, revision BIGINT NOT NULL, actor_id VARCHAR(64) NOT NULL, at_ms BIGINT NOT NULL,
 reason_text VARCHAR(1000) NOT NULL, snapshot_doc TEXT NOT NULL, principal_batch BIGINT NOT NULL,
 UNIQUE(child_id,revision), FOREIGN KEY(child_id) REFERENCES core_link(child_id), FOREIGN KEY(actor_id) REFERENCES core_actor(id), FOREIGN KEY(principal_batch) REFERENCES authority_batch(revision)
);
CREATE TABLE case_evidence_assessment (
 seq BIGINT AUTO_INCREMENT PRIMARY KEY, child_id VARCHAR(36) NOT NULL, case_id VARCHAR(36) NOT NULL, pertinence_revision BIGINT NOT NULL,
 actor_id VARCHAR(64) NOT NULL, at_ms BIGINT NOT NULL, decision VARCHAR(20) NOT NULL, reason_text VARCHAR(2000) NOT NULL, supersedes_seq BIGINT,
 FOREIGN KEY(child_id) REFERENCES core_link(child_id), FOREIGN KEY(case_id) REFERENCES core_case(id), FOREIGN KEY(actor_id) REFERENCES core_actor(id), FOREIGN KEY(supersedes_seq) REFERENCES case_evidence_assessment(seq),
 CHECK(decision IN ('ADMITIDA','NO_ADMITIDA'))
);
CREATE INDEX ix_assessment_context ON case_evidence_assessment(child_id,case_id,seq);
CREATE TABLE document_content_policy (
 document_id VARCHAR(36) PRIMARY KEY, owner_case VARCHAR(36) NOT NULL, classification VARCHAR(20) NOT NULL, revision BIGINT NOT NULL,
 FOREIGN KEY(document_id) REFERENCES institution_document(id), FOREIGN KEY(owner_case) REFERENCES core_case(id), CHECK(classification IN ('SIN_CLASIFICAR','ORDINARIO','FIR','BLOQUEADO'))
);
CREATE TABLE document_content_history (
 seq BIGINT AUTO_INCREMENT PRIMARY KEY, document_id VARCHAR(36) NOT NULL, case_id VARCHAR(36) NOT NULL, revision BIGINT NOT NULL, actor_id VARCHAR(64) NOT NULL, at_ms BIGINT NOT NULL,
 action_code VARCHAR(24) NOT NULL, reason_text VARCHAR(1000) NOT NULL, snapshot_doc TEXT NOT NULL,
 FOREIGN KEY(document_id) REFERENCES institution_document(id), FOREIGN KEY(case_id) REFERENCES core_case(id), FOREIGN KEY(actor_id) REFERENCES core_actor(id)
);
CREATE TABLE document_disclosure (
 document_id VARCHAR(36) NOT NULL, case_id VARCHAR(36) NOT NULL, revision BIGINT NOT NULL, policy_revision BIGINT NOT NULL,
 matter_id VARCHAR(64) NOT NULL, area_seq BIGINT NOT NULL, matter_seq BIGINT NOT NULL, valid_from BIGINT NOT NULL, valid_until BIGINT NOT NULL,
 content_allowed BOOLEAN NOT NULL, download_allowed BOOLEAN NOT NULL, status VARCHAR(16) NOT NULL,
 PRIMARY KEY(document_id,case_id), FOREIGN KEY(document_id) REFERENCES document_content_policy(document_id), FOREIGN KEY(case_id) REFERENCES core_case(id), FOREIGN KEY(matter_id) REFERENCES institution_matter(id),
 CHECK(valid_until>valid_from), CHECK(status IN ('ACTIVA','SUSPENDIDA')), CHECK(download_allowed=FALSE OR content_allowed=TRUE)
);

