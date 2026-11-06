-- Metadatos de la única actuación original: no copia textos ni infiere fechas históricas.
ALTER TABLE institution_flow_history ADD CONSTRAINT uq_flow_resource UNIQUE(seq,resource_id);
CREATE TABLE institution_case_action (
 action_id VARCHAR(36) PRIMARY KEY,
 case_id VARCHAR(36) NOT NULL,
 schema_version VARCHAR(8) NOT NULL,
 effective_ms BIGINT NOT NULL,
 rectifies_id VARCHAR(36) UNIQUE,
 correction_reason VARCHAR(2000),
 FOREIGN KEY(action_id,case_id) REFERENCES core_action(id,case_id),
 FOREIGN KEY(rectifies_id,case_id) REFERENCES core_action(id,case_id),
 CHECK(schema_version='1.0'),
 CHECK((rectifies_id IS NULL AND correction_reason IS NULL) OR (rectifies_id IS NOT NULL AND correction_reason IS NOT NULL AND rectifies_id<>action_id))
);
CREATE TABLE institution_req_action (
 history_seq BIGINT PRIMARY KEY,
 requirement_id VARCHAR(36) NOT NULL,
 schema_version VARCHAR(8) NOT NULL,
 effective_ms BIGINT NOT NULL,
 rectifies_seq BIGINT UNIQUE,
 correction_reason VARCHAR(2000),
 FOREIGN KEY(requirement_id) REFERENCES institution_req(requirement_id),
 FOREIGN KEY(history_seq,requirement_id) REFERENCES institution_flow_history(seq,resource_id),
 FOREIGN KEY(rectifies_seq,requirement_id) REFERENCES institution_flow_history(seq,resource_id),
 CHECK(schema_version='1.0'),
 CHECK((rectifies_seq IS NULL AND correction_reason IS NULL) OR (rectifies_seq IS NOT NULL AND correction_reason IS NOT NULL AND rectifies_seq<>history_seq))
);
