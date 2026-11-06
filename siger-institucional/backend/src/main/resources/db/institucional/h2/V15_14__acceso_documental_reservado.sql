-- Consentimiento excepcional por documento/Caso/cuenta. Sin roles, semillas o reescritura histórica.
CREATE TABLE institution_sensitive_access(
 id VARCHAR(36) PRIMARY KEY,document_id VARCHAR(36) NOT NULL,case_id VARCHAR(36) NOT NULL,
 policy_revision BIGINT NOT NULL,area_seq BIGINT NOT NULL,matter_seq BIGINT NOT NULL,
 unit_code VARCHAR(8) NOT NULL,site_id VARCHAR(40) NOT NULL,area_id VARCHAR(64),matter_id VARCHAR(64),
 requested_by VARCHAR(64) NOT NULL,requested_person VARCHAR(64) NOT NULL,request_authority BIGINT NOT NULL,
 beneficiary_id VARCHAR(64) NOT NULL,beneficiary_person VARCHAR(64) NOT NULL,
 requested_ms BIGINT NOT NULL,from_ms BIGINT NOT NULL,until_ms BIGINT NOT NULL,
 content_allowed BOOLEAN NOT NULL,download_allowed BOOLEAN NOT NULL,
 reason_text VARCHAR(2000) NOT NULL,request_hash CHAR(64) NOT NULL,
 state VARCHAR(24) NOT NULL,revision BIGINT NOT NULL,active_slot INT,
 authorized_by VARCHAR(64),authorized_person VARCHAR(64),authorized_ms BIGINT,
 capability_id VARCHAR(64),capability_hash CHAR(64),authority_revision BIGINT,
 basis_ref VARCHAR(180),basis_sha256 CHAR(64),decision_text VARCHAR(2000),
 ended_by VARCHAR(64),ended_ms BIGINT,end_reason VARCHAR(2000),
 FOREIGN KEY(document_id) REFERENCES institution_document(id),FOREIGN KEY(case_id) REFERENCES core_case(id),
 FOREIGN KEY(requested_by) REFERENCES staff_account(actor_id),FOREIGN KEY(beneficiary_id) REFERENCES staff_account(actor_id),
 FOREIGN KEY(authorized_by) REFERENCES staff_account(actor_id),FOREIGN KEY(ended_by) REFERENCES core_actor(id),
 FOREIGN KEY(request_authority) REFERENCES authority_batch(revision),FOREIGN KEY(authority_revision) REFERENCES authority_batch(revision),
 FOREIGN KEY(unit_code) REFERENCES core_unit(code),FOREIGN KEY(site_id) REFERENCES core_site(id),
 FOREIGN KEY(area_id) REFERENCES institution_area(id),FOREIGN KEY(matter_id) REFERENCES institution_matter(id),
 UNIQUE(document_id,case_id,beneficiary_id,active_slot),
 CHECK(until_ms>from_ms),CHECK(requested_person<>beneficiary_person),
 CHECK(NOT download_allowed OR content_allowed),
 CHECK(authorized_person IS NULL OR (authorized_person<>requested_person AND authorized_person<>beneficiary_person)),
 CHECK(state IN ('PENDIENTE','ACTIVA','RECHAZADA','RETIRADA','REVOCADA','EXPIRADA')),
 CHECK((state IN ('PENDIENTE','ACTIVA') AND active_slot IS NOT NULL AND active_slot=1) OR (state NOT IN ('PENDIENTE','ACTIVA') AND active_slot IS NULL)),
 CHECK(state<>'ACTIVA' OR (authorized_by IS NOT NULL AND authorized_person IS NOT NULL AND capability_hash IS NOT NULL AND authorized_ms IS NOT NULL))
);
CREATE INDEX ix_sensitive_access_case ON institution_sensitive_access(case_id,state,requested_ms);
CREATE INDEX ix_sensitive_access_beneficiary ON institution_sensitive_access(beneficiary_id,state,from_ms,until_ms);
