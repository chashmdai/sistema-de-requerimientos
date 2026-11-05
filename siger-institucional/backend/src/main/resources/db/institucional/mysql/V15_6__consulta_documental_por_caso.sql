-- Referenciar no concede consulta. Decisiones explícitas para gestores del Caso de destino.
CREATE TABLE document_case_consent (
 document_id VARCHAR(36) NOT NULL, case_id VARCHAR(36) NOT NULL, revision BIGINT NOT NULL, policy_revision BIGINT NOT NULL,
 area_seq BIGINT NOT NULL, matter_seq BIGINT NOT NULL, valid_from BIGINT NOT NULL, valid_until BIGINT NOT NULL,
 content_allowed BOOLEAN NOT NULL, download_allowed BOOLEAN NOT NULL, status VARCHAR(16) NOT NULL,
 PRIMARY KEY(document_id,case_id), FOREIGN KEY(document_id) REFERENCES document_content_policy(document_id), FOREIGN KEY(case_id) REFERENCES core_case(id),
 CHECK(valid_until>valid_from), CHECK(status IN ('ACTIVA','SUSPENDIDA')), CHECK(download_allowed=FALSE OR content_allowed=TRUE)
);
