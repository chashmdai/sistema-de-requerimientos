ALTER TABLE core_case ADD COLUMN complexity_code VARCHAR(20) NULL CHECK(complexity_code IS NULL OR complexity_code='BAJA');
ALTER TABLE core_case ADD COLUMN closure_mode VARCHAR(32) NULL;
CREATE TABLE core_complexity(id VARCHAR(36) PRIMARY KEY,case_id VARCHAR(36) NOT NULL,classification VARCHAR(20),reason_text TEXT NOT NULL,actor_id VARCHAR(64) NOT NULL,at_ms BIGINT NOT NULL,FOREIGN KEY(case_id) REFERENCES core_case(id),FOREIGN KEY(actor_id) REFERENCES core_actor(id),CHECK(classification IS NULL OR classification='BAJA'));
