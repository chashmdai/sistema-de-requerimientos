-- Conserva el alcance de todas las concesiones previas; no amplía sus firmas o hashes.
ALTER TABLE institution_sensitive_access ADD COLUMN scope_mode VARCHAR(20) NOT NULL DEFAULT 'CASO_DOCUMENTO';
ALTER TABLE institution_sensitive_access ADD CONSTRAINT ck_sensitive_scope_mode CHECK(scope_mode IN ('CASO_DOCUMENTO','SOLO_DOCUMENTO'));
