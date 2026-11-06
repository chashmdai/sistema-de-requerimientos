CREATE TABLE institution_function_scope(
 scope_hash VARCHAR(64) PRIMARY KEY,
 function_code VARCHAR(64) NOT NULL,
 role_code VARCHAR(32) NOT NULL,
 scope_code VARCHAR(24) NOT NULL,
 unit_code VARCHAR(8),site_id VARCHAR(40) NOT NULL,area_id VARCHAR(64),
 first_revision BIGINT NOT NULL,
 FOREIGN KEY(first_revision) REFERENCES authority_batch(revision),
 FOREIGN KEY(site_id) REFERENCES core_site(id),
 FOREIGN KEY(unit_code) REFERENCES core_unit(code),
 FOREIGN KEY(area_id) REFERENCES institution_area(id)
);
CREATE TABLE institution_substitution(
 id VARCHAR(36) PRIMARY KEY,
 designation_id VARCHAR(64) NOT NULL,function_code VARCHAR(64) NOT NULL,
 designation_hash VARCHAR(64) NOT NULL,designation_doc LONGTEXT NOT NULL,
 authority_revision BIGINT NOT NULL,
 titular_person VARCHAR(64) NOT NULL,substitute_person VARCHAR(64) NOT NULL,
 requested_by VARCHAR(64) NOT NULL,requested_person VARCHAR(64) NOT NULL,
 requested_ms BIGINT NOT NULL,reason_text VARCHAR(2000) NOT NULL,request_hash VARCHAR(64) NOT NULL,
 from_ms BIGINT NOT NULL,until_ms BIGINT NOT NULL,review_ms BIGINT,
 state VARCHAR(24) NOT NULL,revision BIGINT NOT NULL,
 approved_by VARCHAR(64),approved_person VARCHAR(64),approved_ms BIGINT,decision_text VARCHAR(2000),
 executed_by VARCHAR(64),executed_person VARCHAR(64),executed_ms BIGINT,
 activated_ms BIGINT,ended_by VARCHAR(64),ended_ms BIGINT,end_reason VARCHAR(2000),
 previous_id VARCHAR(36),
 FOREIGN KEY(authority_revision) REFERENCES authority_batch(revision),
 FOREIGN KEY(requested_by) REFERENCES staff_account(actor_id),
 FOREIGN KEY(approved_by) REFERENCES core_actor(id),
 FOREIGN KEY(executed_by) REFERENCES core_actor(id),
 FOREIGN KEY(ended_by) REFERENCES core_actor(id),
 FOREIGN KEY(previous_id) REFERENCES institution_substitution(id),
 CHECK(until_ms>from_ms),
 CHECK(requested_person=substitute_person AND titular_person<>substitute_person),
 CHECK(approved_person IS NULL OR (approved_person<>requested_person AND approved_person<>titular_person)),
 CHECK(executed_person IS NULL OR (executed_person<>requested_person AND executed_person<>approved_person)),
 CHECK(state<>'EJECUTADA' OR (approved_by IS NOT NULL AND approved_person IS NOT NULL AND approved_ms IS NOT NULL AND executed_by IS NOT NULL AND executed_person IS NOT NULL AND executed_ms IS NOT NULL)),
 CHECK(state IN ('PENDIENTE','APROBADA','EJECUTADA','RECHAZADA','RETIRADA','REVOCADA','FINALIZADA','EXPIRADA','REVISION_REQUERIDA'))
);
CREATE INDEX ix_substitution_function ON institution_substitution(function_code,state,from_ms,until_ms);
CREATE INDEX ix_substitution_person ON institution_substitution(substitute_person,state);
CREATE TABLE function_command(
 id VARCHAR(36) PRIMARY KEY,actor_id VARCHAR(64) NOT NULL,
 action_code VARCHAR(40) NOT NULL,request_hash VARCHAR(64) NOT NULL,
 result_id VARCHAR(36) NOT NULL,result_revision BIGINT NOT NULL,
 FOREIGN KEY(actor_id) REFERENCES core_actor(id),
 FOREIGN KEY(result_id) REFERENCES institution_substitution(id)
);
