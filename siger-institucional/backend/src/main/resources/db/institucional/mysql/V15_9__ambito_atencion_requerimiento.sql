-- Conservación expresa del ámbito asignado; sin inferir ni actualizar registros previos.
-- La fila nace al aceptar la primera transferencia, preservando su contexto anterior.
CREATE TABLE institution_req_attention(
 requirement_id VARCHAR(36) PRIMARY KEY,actor_id VARCHAR(64) NOT NULL,unit_code VARCHAR(8) NOT NULL,site_id VARCHAR(40) NOT NULL,area_id VARCHAR(64),basis_transfer VARCHAR(36) NOT NULL,
 FOREIGN KEY(requirement_id) REFERENCES institution_req(requirement_id),FOREIGN KEY(actor_id) REFERENCES core_actor(id),FOREIGN KEY(unit_code,site_id) REFERENCES core_unit_site(unit_code,site_id),FOREIGN KEY(area_id) REFERENCES institution_area(id),FOREIGN KEY(basis_transfer) REFERENCES institution_req_transfer(id)
);
