ALTER TABLE institution_task ADD COLUMN mandatory BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE institution_task ADD COLUMN assigned_by VARCHAR(64);
ALTER TABLE institution_task ADD COLUMN reminder_generation BIGINT NOT NULL DEFAULT 0;
ALTER TABLE task_history ADD COLUMN reason_text VARCHAR(2000);
ALTER TABLE institution_task ADD CONSTRAINT fk_task_assigner FOREIGN KEY(assigned_by) REFERENCES core_actor(id);
ALTER TABLE institution_task ADD CONSTRAINT ck_task_actor_context CHECK((resource_type='CASO' AND resource_id IS NOT NULL AND request_id IS NULL) OR creator_id=responsible_id);
CREATE TABLE case_team_member(
 id VARCHAR(36) PRIMARY KEY,case_id VARCHAR(36) NOT NULL,actor_id VARCHAR(64) NOT NULL,state VARCHAR(16) NOT NULL,revision BIGINT NOT NULL,
 joined_by VARCHAR(64) NOT NULL,joined_ms BIGINT NOT NULL,reason_text VARCHAR(2000) NOT NULL,removed_by VARCHAR(64),removed_ms BIGINT,removal_reason VARCHAR(2000),
 FOREIGN KEY(case_id) REFERENCES core_case(id),FOREIGN KEY(actor_id) REFERENCES staff_account(actor_id),FOREIGN KEY(joined_by) REFERENCES core_actor(id),FOREIGN KEY(removed_by) REFERENCES core_actor(id),CHECK(state IN ('ACTIVO','RETIRADO'))
);
CREATE TABLE case_team_access(case_id VARCHAR(36) NOT NULL,actor_id VARCHAR(64) NOT NULL,membership_id VARCHAR(36) NOT NULL UNIQUE,PRIMARY KEY(case_id,actor_id),FOREIGN KEY(membership_id) REFERENCES case_team_member(id),FOREIGN KEY(case_id) REFERENCES core_case(id),FOREIGN KEY(actor_id) REFERENCES staff_account(actor_id));
CREATE TABLE institution_task_attempt(id VARCHAR(36) PRIMARY KEY,task_id VARCHAR(36) NOT NULL,actor_id VARCHAR(64) NOT NULL,at_ms BIGINT NOT NULL,revision BIGINT NOT NULL,body_text VARCHAR(2000) NOT NULL,FOREIGN KEY(task_id) REFERENCES institution_task(id),FOREIGN KEY(actor_id) REFERENCES core_actor(id));
CREATE TABLE case_wait_request(id VARCHAR(36) PRIMARY KEY,case_id VARCHAR(36) NOT NULL,case_revision BIGINT NOT NULL,requested_by VARCHAR(64) NOT NULL,requested_ms BIGINT NOT NULL,missing_text VARCHAR(2000) NOT NULL,requested_from VARCHAR(2000) NOT NULL,requested_at BIGINT NOT NULL,state VARCHAR(20) NOT NULL,revision BIGINT NOT NULL,decided_by VARCHAR(64),decided_ms BIGINT,decision_text VARCHAR(2000),FOREIGN KEY(case_id) REFERENCES core_case(id),FOREIGN KEY(requested_by) REFERENCES core_actor(id),FOREIGN KEY(decided_by) REFERENCES core_actor(id),CHECK(state IN ('SOLICITADA','CONFIRMADA','RECHAZADA','FINALIZADA')));
CREATE INDEX ix_wait_case ON case_wait_request(case_id,state);
