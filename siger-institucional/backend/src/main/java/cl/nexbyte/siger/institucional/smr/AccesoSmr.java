package cl.nexbyte.siger.institucional.smr;

import cl.nexbyte.siger.institucional.identidad.IdentidadInstitucional;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio;
import cl.nexbyte.siger.institucional.robustez.*;
import java.util.*;

/** Facultades SMR explícitas sobre su custodia. Event sourcing en el diario de identidad existente. */
public final class AccesoSmr {
    private final BaseJdbc db;private final IdentidadInstitucional identidad;
    public AccesoSmr(BaseJdbc db,IdentidadInstitucional identidad){this.db=db;this.identidad=identidad;}
    public boolean tiene(String id,String permiso){
        if(!Set.of("LECTURA","OPERACION").contains(permiso))throw new IllegalArgumentException();
        identidad.operador(id);
        var rows=db.sql.queryForList("SELECT event_code FROM identity_event WHERE subject_id=? AND event_code IN (?,?) ORDER BY seq DESC LIMIT 1",id,"SMR_"+permiso+"_OTORGADA","SMR_"+permiso+"_REVOCADA");
        return !rows.isEmpty()&&rows.getFirst().get("event_code").equals("SMR_"+permiso+"_OTORGADA");
    }
    public void exigir(CatalogoLaboratorio.Operador actor,String permiso){if(!tiene(actor.id(),permiso))throw new FalloTecnico(403);}
    public void cambiar(String admin,String id,String permiso,boolean conceder){
        if(!Set.of("LECTURA","OPERACION").contains(permiso))throw new FalloTecnico(400);
        db.transaction(()->{db.sql.queryForObject("SELECT id FROM identity_guard WHERE id=1 FOR UPDATE",Integer.class);
            if(!identidad.operador(admin).ti())throw new FalloTecnico(403);identidad.operador(id);
            db.sql.update("INSERT INTO identity_event(actor_id,subject_id,event_code,resource_id,at_ms) VALUES(?,?,?,?,?)",admin,id,"SMR_"+permiso+(conceder?"_OTORGADA":"_REVOCADA"),"CUSTODIA_SMR",System.currentTimeMillis());
            db.sql.update("UPDATE staff_account SET security_epoch=security_epoch+1,updated_ms=?,updated_by=? WHERE actor_id=?",System.currentTimeMillis(),admin,id);return null;});
    }
}
