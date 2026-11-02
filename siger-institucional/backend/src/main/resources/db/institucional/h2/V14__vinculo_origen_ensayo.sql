-- Opt-in del ensayo U4 aislado. El runtime QA existente no usa esta tabla.
CREATE TABLE consumer_origin_binding (
    id INT PRIMARY KEY CHECK (id=1),
    origin_ref VARCHAR(64) NOT NULL,
    binding_sha256 CHAR(64) NOT NULL,
    created_ms BIGINT NOT NULL
);
