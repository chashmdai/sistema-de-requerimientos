-- U5: preparación de Solicitudes. Facultades de respuesta/canalización/escalamiento pendientes.
CREATE TABLE institution_request(
 id VARCHAR(36) PRIMARY KEY,communication_id VARCHAR(36) NOT NULL,
 creator_id VARCHAR(64) NOT NULL,responsible_id VARCHAR(64) NOT NULL,
 unit_code VARCHAR(8),site_id VARCHAR(40),title VARCHAR(180) NOT NULL,description_text TEXT NOT NULL,
 state VARCHAR(24) NOT NULL,revision BIGINT NOT NULL,created_ms BIGINT NOT NULL,updated_ms BIGINT NOT NULL,
 prepared_kind VARCHAR(24),prepared_text TEXT,target_unit VARCHAR(8),target_site VARCHAR(40),
 FOREIGN KEY(communication_id) REFERENCES communication_registry(comunicacion_id),
 FOREIGN KEY(creator_id) REFERENCES staff_account(actor_id),FOREIGN KEY(responsible_id) REFERENCES staff_account(actor_id),
 FOREIGN KEY(unit_code,site_id) REFERENCES core_unit_site(unit_code,site_id),
 FOREIGN KEY(target_unit,target_site) REFERENCES core_unit_site(unit_code,site_id),
 CHECK(creator_id=responsible_id),CHECK(state IN ('EN_PREPARACION','PREPARADA')),
 CHECK((unit_code IS NULL AND site_id IS NULL) OR (unit_code IS NOT NULL AND site_id IS NOT NULL)),
 CHECK((prepared_kind IS NULL AND prepared_text IS NULL AND target_unit IS NULL AND target_site IS NULL AND state='EN_PREPARACION')
  OR (prepared_kind IS NOT NULL AND prepared_kind IN ('RESPUESTA','CANALIZACION','ESCALAMIENTO') AND prepared_text IS NOT NULL AND state='PREPARADA')),
 CHECK((target_unit IS NULL AND target_site IS NULL) OR (target_unit IS NOT NULL AND target_site IS NOT NULL))
);
CREATE INDEX ix_request_owner ON institution_request(responsible_id,created_ms);
CREATE INDEX ix_request_communication ON institution_request(communication_id,id);
CREATE TABLE request_history(seq BIGINT AUTO_INCREMENT PRIMARY KEY,request_id VARCHAR(36) NOT NULL,actor_id VARCHAR(64) NOT NULL,at_ms BIGINT NOT NULL,revision BIGINT NOT NULL,event_code VARCHAR(40) NOT NULL,snapshot_doc TEXT NOT NULL,FOREIGN KEY(request_id) REFERENCES institution_request(id),FOREIGN KEY(actor_id) REFERENCES core_actor(id));
CREATE TABLE request_followup(id VARCHAR(36) PRIMARY KEY,request_id VARCHAR(36) NOT NULL,actor_id VARCHAR(64) NOT NULL,at_ms BIGINT NOT NULL,body_text TEXT NOT NULL,FOREIGN KEY(request_id) REFERENCES institution_request(id),FOREIGN KEY(actor_id) REFERENCES core_actor(id));
CREATE TABLE request_command(id VARCHAR(36) PRIMARY KEY,actor_id VARCHAR(64) NOT NULL,action_code VARCHAR(24) NOT NULL,request_hash CHAR(64) NOT NULL,request_id VARCHAR(36) NOT NULL,result_revision BIGINT NOT NULL,FOREIGN KEY(actor_id) REFERENCES core_actor(id),FOREIGN KEY(request_id) REFERENCES institution_request(id));
ALTER TABLE institution_task ALTER COLUMN resource_type DROP NOT NULL;
ALTER TABLE institution_task ALTER COLUMN resource_id DROP NOT NULL;
ALTER TABLE institution_task ADD COLUMN request_id VARCHAR(36);
ALTER TABLE institution_task ADD CONSTRAINT fk_task_request FOREIGN KEY(request_id) REFERENCES institution_request(id);
-- Mantiene las FK y el CHECK CASO históricos; NULL permite contexto independiente o Solicitud.
ALTER TABLE institution_task ADD CONSTRAINT ck_task_context CHECK(
 (resource_type IS NOT NULL AND resource_type='CASO' AND resource_id IS NOT NULL AND request_id IS NULL)
 OR (resource_type IS NULL AND resource_id IS NULL));
