-- Evolución aditiva: ninguna titularidad, aprobación o asignación histórica se infiere.
CREATE TABLE institution_req_transfer(
 id VARCHAR(36) PRIMARY KEY,requirement_id VARCHAR(36) NOT NULL,origin_destination VARCHAR(36) NOT NULL,
 unit_code VARCHAR(8) NOT NULL,site_id VARCHAR(40) NOT NULL,area_id VARCHAR(64),
 reason_text VARCHAR(2000) NOT NULL,summary_text VARCHAR(2000) NOT NULL,
 state VARCHAR(16) NOT NULL,revision BIGINT NOT NULL,requested_by VARCHAR(64) NOT NULL,created_ms BIGINT NOT NULL,
 accepted_authority BIGINT,accepted_destination VARCHAR(36) UNIQUE,
 FOREIGN KEY(requirement_id) REFERENCES institution_req(requirement_id),FOREIGN KEY(origin_destination) REFERENCES core_destination(id),
 FOREIGN KEY(unit_code,site_id) REFERENCES core_unit_site(unit_code,site_id),FOREIGN KEY(area_id) REFERENCES institution_area(id),
 FOREIGN KEY(requested_by) REFERENCES core_actor(id),FOREIGN KEY(accepted_authority) REFERENCES authority_batch(revision),FOREIGN KEY(accepted_destination) REFERENCES core_destination(id),CHECK(state IN ('PENDIENTE','ACEPTADA','RECHAZADA','RETIRADA')),
 CHECK((state='ACEPTADA' AND accepted_authority IS NOT NULL AND accepted_destination IS NOT NULL) OR (state<>'ACEPTADA' AND accepted_authority IS NULL AND accepted_destination IS NULL))
);
CREATE INDEX ix_req_transfer_pending ON institution_req_transfer(requirement_id,state);
CREATE TABLE institution_case_derivation(
 id VARCHAR(36) PRIMARY KEY,origin_case_id VARCHAR(36) NOT NULL,requirement_id VARCHAR(36) NOT NULL,
 origin_destination VARCHAR(36) NOT NULL,origin_area VARCHAR(64),origin_responsible VARCHAR(64) NOT NULL,
 unit_code VARCHAR(8) NOT NULL,site_id VARCHAR(40) NOT NULL,area_id VARCHAR(64),
 reason_text VARCHAR(2000) NOT NULL,summary_text VARCHAR(2000) NOT NULL,objective_text VARCHAR(2000) NOT NULL,
 state VARCHAR(16) NOT NULL,revision BIGINT NOT NULL,requested_by VARCHAR(64) NOT NULL,created_ms BIGINT NOT NULL,successor_id VARCHAR(36) UNIQUE,matter_id VARCHAR(64),
 FOREIGN KEY(origin_case_id) REFERENCES institution_case(case_id),FOREIGN KEY(requirement_id) REFERENCES institution_req(requirement_id),
 FOREIGN KEY(origin_destination) REFERENCES core_destination(id),FOREIGN KEY(origin_area) REFERENCES institution_area(id),FOREIGN KEY(origin_responsible) REFERENCES core_actor(id),
 FOREIGN KEY(unit_code,site_id) REFERENCES core_unit_site(unit_code,site_id),FOREIGN KEY(area_id) REFERENCES institution_area(id),
 FOREIGN KEY(requested_by) REFERENCES core_actor(id),FOREIGN KEY(successor_id) REFERENCES institution_case(case_id),FOREIGN KEY(matter_id) REFERENCES institution_matter(id),
 CHECK(state IN ('PENDIENTE','ACEPTADA','RECHAZADA','RETIRADA')),CHECK((state='ACEPTADA' AND successor_id IS NOT NULL) OR (state<>'ACEPTADA' AND successor_id IS NULL))
);
CREATE INDEX ix_case_derivation_pending ON institution_case_derivation(origin_case_id,state);
CREATE TABLE institution_reopen(
 request_id VARCHAR(36) PRIMARY KEY,evidence_text VARCHAR(2000) NOT NULL,
 principal_destination VARCHAR(36),requirement_revision BIGINT,reactivation_by VARCHAR(64),reactivation_ms BIGINT,
 FOREIGN KEY(request_id) REFERENCES core_reopen(id),FOREIGN KEY(principal_destination) REFERENCES core_destination(id),FOREIGN KEY(reactivation_by) REFERENCES core_actor(id),
 CHECK((reactivation_by IS NULL AND principal_destination IS NULL AND requirement_revision IS NULL AND reactivation_ms IS NULL)
 OR (reactivation_by IS NOT NULL AND principal_destination IS NOT NULL AND requirement_revision IS NOT NULL AND reactivation_ms IS NOT NULL))
);
CREATE TABLE institution_case_closure(
 case_id VARCHAR(36) NOT NULL,case_revision BIGINT NOT NULL,closed_ms BIGINT NOT NULL,closed_by VARCHAR(64) NOT NULL,mode_code VARCHAR(32) NOT NULL,
 PRIMARY KEY(case_id,case_revision),FOREIGN KEY(case_id) REFERENCES institution_case(case_id),FOREIGN KEY(closed_by) REFERENCES core_actor(id)
);
CREATE TABLE institution_case_closure_request(
 case_id VARCHAR(36) NOT NULL,case_revision BIGINT NOT NULL,mode_code VARCHAR(32) NOT NULL,
 PRIMARY KEY(case_id,case_revision),FOREIGN KEY(case_id) REFERENCES institution_case(case_id),CHECK(mode_code='DERIVACION')
);
CREATE TABLE institution_assignment_current(
 case_id VARCHAR(36) PRIMARY KEY,assignment_id VARCHAR(36) NOT NULL UNIQUE,
 FOREIGN KEY(case_id) REFERENCES institution_case(case_id),FOREIGN KEY(assignment_id) REFERENCES core_assignment(id)
);
CREATE TABLE institution_assignment_receipt(
 assignment_id VARCHAR(36) PRIMARY KEY,acknowledged_by VARCHAR(64) NOT NULL,acknowledged_ms BIGINT NOT NULL,
 FOREIGN KEY(assignment_id) REFERENCES core_assignment(id),FOREIGN KEY(acknowledged_by) REFERENCES core_actor(id)
);
