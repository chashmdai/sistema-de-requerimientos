-- Versiones acreditadas e inmutables. La activación sigue dependiendo del registro firmado vigente.
CREATE TABLE temporal_calendar_version (
 id VARCHAR(64) NOT NULL,
 version_code VARCHAR(32) NOT NULL,
 snapshot_sha256 CHAR(64) NOT NULL,
 snapshot_doc LONGTEXT NOT NULL,
 authority_revision BIGINT NOT NULL,
 PRIMARY KEY(id,version_code),
 FOREIGN KEY(authority_revision) REFERENCES authority_batch(revision)
);
CREATE TABLE temporal_policy_version (
 id VARCHAR(64) NOT NULL,
 version_code VARCHAR(32) NOT NULL,
 snapshot_sha256 CHAR(64) NOT NULL,
 snapshot_doc LONGTEXT NOT NULL,
 authority_revision BIGINT NOT NULL,
 calendar_id VARCHAR(64),
 calendar_version VARCHAR(32),
 PRIMARY KEY(id,version_code),
 FOREIGN KEY(authority_revision) REFERENCES authority_batch(revision),
 FOREIGN KEY(calendar_id,calendar_version) REFERENCES temporal_calendar_version(id,version_code),
 CHECK((calendar_id IS NULL AND calendar_version IS NULL) OR (calendar_id IS NOT NULL AND calendar_version IS NOT NULL))
);
-- Hechos capturados en la transacción real, sin inferir ámbitos o reconstruir históricos.
CREATE TABLE temporal_registration_source (
 id VARCHAR(64) PRIMARY KEY,
 communication_id VARCHAR(36) NOT NULL,
 resource_kind VARCHAR(8) NOT NULL,
 resource_id VARCHAR(36) NOT NULL,
 topic_code VARCHAR(24) NOT NULL,
 source_type VARCHAR(24) NOT NULL,
 unit_code VARCHAR(8),
 site_id VARCHAR(40),
 area_id VARCHAR(64),
 priority_code VARCHAR(64),
 effective_ms BIGINT NOT NULL,
 registered_ms BIGINT NOT NULL,
 captured_ms BIGINT NOT NULL,
 registered_by VARCHAR(64),
 previous_id VARCHAR(64),
 source_sha256 CHAR(64) NOT NULL,
 FOREIGN KEY(communication_id) REFERENCES communication_registry(comunicacion_id),
 FOREIGN KEY(unit_code,site_id) REFERENCES core_unit_site(unit_code,site_id),
 FOREIGN KEY(area_id) REFERENCES institution_area(id),
 FOREIGN KEY(registered_by) REFERENCES core_actor(id),
 FOREIGN KEY(previous_id) REFERENCES temporal_registration_source(id),
 CHECK(resource_kind IN ('COM','REQ','CASO')),
 CHECK(topic_code IN ('REGISTRO_ATENCION','REGISTRO_GESTION')),
 CHECK(source_type IN ('MANUAL','FORMULARIO','GESTION_CASO','GESTION_REQ','RECTIFICACION_MANUAL')),
 CHECK(area_id IS NULL OR (unit_code IS NOT NULL AND site_id IS NOT NULL))
);
CREATE INDEX ix_temporal_source_resource ON temporal_registration_source(resource_kind,resource_id,captured_ms,id);
CREATE TABLE temporal_measurement (
 id VARCHAR(36) PRIMARY KEY,
 source_id VARCHAR(64) NOT NULL,
 source_sha256 CHAR(64) NOT NULL,
 policy_id VARCHAR(64) NOT NULL,
 policy_version VARCHAR(32) NOT NULL,
 policy_sha256 CHAR(64) NOT NULL,
 calendar_sha256 CHAR(64),
 authority_revision BIGINT NOT NULL,
 deadline_ms BIGINT NOT NULL,
 elapsed_ms BIGINT NOT NULL,
 result_code VARCHAR(24) NOT NULL,
 measurement_sha256 CHAR(64) NOT NULL,
 measured_by VARCHAR(64) NOT NULL,
 at_ms BIGINT NOT NULL,
 UNIQUE(source_id,source_sha256,policy_id,policy_version),
 FOREIGN KEY(source_id) REFERENCES temporal_registration_source(id),
 FOREIGN KEY(policy_id,policy_version) REFERENCES temporal_policy_version(id,version_code),
 FOREIGN KEY(authority_revision) REFERENCES authority_batch(revision),
 FOREIGN KEY(measured_by) REFERENCES core_actor(id),
 CHECK(elapsed_ms>=0),
 CHECK(result_code IN ('DENTRO_PLAZO','FUERA_PLAZO'))
);
CREATE TABLE temporal_measurement_command (
 id VARCHAR(36) PRIMARY KEY,
 actor_id VARCHAR(64) NOT NULL,
 resource_kind VARCHAR(8) NOT NULL,
 resource_id VARCHAR(36) NOT NULL,
 source_id VARCHAR(64) NOT NULL,
 request_hash CHAR(64) NOT NULL,
 measurement_id VARCHAR(36) NOT NULL,
 FOREIGN KEY(actor_id) REFERENCES core_actor(id),
 FOREIGN KEY(source_id) REFERENCES temporal_registration_source(id),
 FOREIGN KEY(measurement_id) REFERENCES temporal_measurement(id),
 CHECK(resource_kind IN ('COM','REQ','CASO'))
);
