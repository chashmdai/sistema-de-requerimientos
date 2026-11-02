-- ACC-04. Sin alterar la forma de staff_account ni sus INSERT históricos.
-- La ausencia de estado conserva las cuentas anteriores; altas/reset nuevos registran el cambio pendiente.
CREATE TABLE staff_credential_state (
    actor_id VARCHAR(64) PRIMARY KEY,
    must_change_password BOOLEAN NOT NULL,
    FOREIGN KEY(actor_id) REFERENCES staff_account(actor_id) ON DELETE CASCADE
);
