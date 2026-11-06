CREATE TABLE institution_collaboration_end(
 id VARCHAR(36) PRIMARY KEY,
 collaboration_id VARCHAR(36) NOT NULL,
 case_id VARCHAR(36) NOT NULL,
 origin_destination VARCHAR(36) NOT NULL,
 requirement_revision BIGINT NOT NULL,
 requested_by VARCHAR(64) NOT NULL,
 created_ms BIGINT NOT NULL,
 reason_text VARCHAR(2000) NOT NULL,
 state VARCHAR(16) NOT NULL,
 revision BIGINT NOT NULL,
 decided_by VARCHAR(64),
 decided_ms BIGINT,
 decision_text VARCHAR(2000),
 FOREIGN KEY(collaboration_id) REFERENCES institution_collaboration(id),
 FOREIGN KEY(case_id) REFERENCES institution_case(case_id),
 FOREIGN KEY(origin_destination) REFERENCES core_destination(id),
 FOREIGN KEY(requested_by) REFERENCES core_actor(id),
 FOREIGN KEY(decided_by) REFERENCES core_actor(id),
 CHECK(state IN ('PENDIENTE','ACEPTADA','RECHAZADA','RETIRADA'))
);
CREATE INDEX ix_collaboration_end_case ON institution_collaboration_end(case_id,state);
CREATE INDEX ix_collaboration_end_request ON institution_collaboration_end(collaboration_id,state);
