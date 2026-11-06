CREATE TABLE institution_closure_exception(
 id VARCHAR(36) PRIMARY KEY,case_id VARCHAR(36) NOT NULL,closure_request VARCHAR(36) NOT NULL,
 requested_by VARCHAR(64) NOT NULL,requested_person VARCHAR(64) NOT NULL,
 reviewer_id VARCHAR(64) NOT NULL,reviewer_person VARCHAR(64) NOT NULL,
 unit_code VARCHAR(8) NOT NULL,site_id VARCHAR(40) NOT NULL,area_id VARCHAR(64),matter_id VARCHAR(64),
 requested_ms BIGINT NOT NULL,from_ms BIGINT NOT NULL,until_ms BIGINT NOT NULL,
 reason_text VARCHAR(2000) NOT NULL,documents_doc LONGTEXT NOT NULL,request_hash VARCHAR(64) NOT NULL,
 state VARCHAR(24) NOT NULL,revision BIGINT NOT NULL,
 authorized_by VARCHAR(64),authorized_person VARCHAR(64),authorized_ms BIGINT,
 capability_id VARCHAR(64),capability_hash VARCHAR(64),authority_revision BIGINT,
 basis_ref VARCHAR(180),basis_sha256 VARCHAR(64),decision_text VARCHAR(2000),
 ended_by VARCHAR(64),ended_ms BIGINT,end_reason VARCHAR(2000),
 FOREIGN KEY(case_id) REFERENCES core_case(id),
 FOREIGN KEY(closure_request) REFERENCES core_review(id),
 FOREIGN KEY(requested_by) REFERENCES staff_account(actor_id),
 FOREIGN KEY(reviewer_id) REFERENCES staff_account(actor_id),
 FOREIGN KEY(authorized_by) REFERENCES staff_account(actor_id),
 FOREIGN KEY(ended_by) REFERENCES core_actor(id),
 FOREIGN KEY(unit_code) REFERENCES core_unit(code),FOREIGN KEY(site_id) REFERENCES core_site(id),
 FOREIGN KEY(area_id) REFERENCES institution_area(id),FOREIGN KEY(matter_id) REFERENCES institution_matter(id),
 FOREIGN KEY(authority_revision) REFERENCES authority_batch(revision),
 CHECK(until_ms>from_ms),CHECK(requested_person<>reviewer_person),
 CHECK(authorized_person IS NULL OR (authorized_person<>requested_person AND authorized_person<>reviewer_person)),
 CHECK(state IN ('PENDIENTE','HABILITADA','RECHAZADA','REVOCADA','EXPIRADA','UTILIZADA','RETIRADA')),
 CHECK(state<>'HABILITADA' OR (authorized_by IS NOT NULL AND authorized_person IS NOT NULL AND capability_hash IS NOT NULL AND authorized_ms IS NOT NULL))
);
CREATE INDEX ix_closure_exception_case ON institution_closure_exception(case_id,state,requested_ms);
CREATE INDEX ix_closure_exception_reviewer ON institution_closure_exception(reviewer_id,state,from_ms,until_ms);
