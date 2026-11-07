package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.contrato.DeclaracionNna;
import cl.nexbyte.siger.institucional.identidad.AutoridadesInstitucionales;
import cl.nexbyte.siger.institucional.robustez.*;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import java.time.*;import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Rectificar conserva captura y procedencia; no cambia domicilio de la SOL, principal ni gestión. */
public final class RectificacionesIngreso {
    public record Datos(String texto,String canalDeclarado,Instant recibidaEnDeclarada,String regionAsunto){@Override public String toString(){return "DatosIngreso[protegidos]";}}
    public record Solicitud(String version,long revision,Datos datos,String motivo){@Override public String toString(){return "RectificacionIngreso[protegida]";}}
    public record Cambio(String version,Datos anterior,Datos nuevo){@Override public String toString(){return "CambioIngreso[protegido]";}}
    public record Constancia(long secuencia,long revision,String autor,Instant registradaEn,String motivo,Datos anterior,Datos nuevo){}
    public record Vista(String version,long revision,Datos original,Datos vigente,List<Constancia> entradas,long total,int pagina,int tamanio){}
    private record Lectura(Datos original,Datos vigente,List<Constancia> entradas){}
    private static final String ACCION="COM_DATOS_RECTIFICADOS";
    private final BaseJdbc db;private final NucleoInstitucional core;private final JsonMapper json;private final Clock clock;
    RectificacionesIngreso(BaseJdbc db,NucleoInstitucional core,JsonMapper json,Clock clock){this.db=db;this.core=core;this.json=json;this.clock=clock;}
    private static String id(UUID id){if(id==null)throw new FalloTecnico(400);return id.toString();}
    private void autorizar(Operador a,UUID com){core.u2().rectificarIngreso(a,com);if(db.sql.queryForObject("SELECT COUNT(*) FROM core_manual WHERE communication_id=?",Long.class,id(com))!=1)throw new FalloTecnico(409,"RECTIFICACION_MANUAL_REQUERIDA");}
    private Lectura leer(UUID com){
        var c=core.contenidoOriginal(com);var regions=db.sql.queryForList("SELECT subject_region FROM communication_origin WHERE communication_id=?",id(com));
        String region=regions.isEmpty()||regions.getFirst().get("subject_region")==null?null:regions.getFirst().get("subject_region").toString();
        var original=new Datos(c.texto(),c.canalDeclarado(),c.recibidaEnDeclarada(),region);Datos vigente=original;var result=new ArrayList<Constancia>();
        for(var row:db.sql.queryForList("SELECT * FROM institution_flow_history WHERE communication_id=? AND resource_id=? AND resource_kind='COM' AND action_code=? ORDER BY seq",id(com),id(com),ACCION)){
            Cambio cambio;
            try{cambio=AutoridadesInstitucionales.lectorPrivado().readValue(row.get("snapshot_doc").toString(),Cambio.class);if(!"1.0".equals(cambio.version())||!vigente.equals(cambio.anterior())||cambio.nuevo()==null||vigente.equals(cambio.nuevo())||!cambio.nuevo().equals(validar(cambio.nuevo(),com)))throw new IllegalArgumentException();}
            catch(RuntimeException invalid){throw new FalloTecnico(409,"RECTIFICACION_HISTORIA_INVALIDA");}
            vigente=cambio.nuevo();result.add(new Constancia(((Number)row.get("seq")).longValue(),((Number)row.get("revision")).longValue(),row.get("actor_id").toString(),Instant.ofEpochMilli(((Number)row.get("at_ms")).longValue()),row.get("reason_text").toString(),cambio.anterior(),cambio.nuevo()));
        }
        return new Lectura(original,vigente,List.copyOf(result));
    }
    private long revision(UUID com){return db.sql.queryForObject("SELECT revision FROM core_communication WHERE id=?",Long.class,id(com));}
    public Vista consultar(Operador a,UUID com,int pagina,int tamanio){
        if(pagina<0||tamanio<1||tamanio>100)throw new FalloTecnico(400);
        return db.transaction(()->{autorizar(a,com);var read=leer(com);var reverse=new ArrayList<>(read.entradas());Collections.reverse(reverse);long offset=(long)pagina*tamanio;int start=(int)Math.min(offset,reverse.size()),end=(int)Math.min(offset+tamanio,reverse.size());
            new PoliticaMaterias(db,clock).auditar(a,id(com),"RECTIFICACIONES_CONSULTADAS");return new Vista("1.0",revision(com),read.original(),read.vigente(),List.copyOf(reverse.subList(start,end)),reverse.size(),pagina,tamanio);});
    }
    public Constancia constancia(Operador a,UUID com,long secuencia){return db.transaction(()->{autorizar(a,com);var result=leer(com).entradas().stream().filter(c->c.secuencia()==secuencia).findFirst().orElseThrow(()->new FalloTecnico(404));new PoliticaMaterias(db,clock).auditar(a,id(com),"RECTIFICACION_CONSULTADA");return result;});}
    private Datos validar(Datos input,UUID com){
        try{
            if(input==null||input.recibidaEnDeclarada()==null)throw new IllegalArgumentException();DeclaracionNna.relato(input.texto());DeclaracionNna.texto(input.canalDeclarado(),100,true);
            long recibida=input.recibidaEnDeclarada().toEpochMilli(),registrada=db.sql.queryForObject("SELECT registrada_ms FROM communication_registry WHERE comunicacion_id=?",Long.class,id(com));
            if(recibida>clock.millis()||recibida>registrada)throw new IllegalArgumentException();
            if(input.regionAsunto()!=null&&db.sql.queryForObject("SELECT COUNT(*) FROM org_region WHERE id=?",Long.class,input.regionAsunto())!=1)throw new IllegalArgumentException();
            var respuestas=db.sql.queryForList("SELECT snapshot_doc FROM institution_flow_history h JOIN core_requirement q ON q.id=h.resource_id WHERE q.communication_id=? AND h.action_code='PRIMERA_GESTION_ACREDITADA'",id(com));
            for(var row:respuestas){var primera=json.readValue(row.get("snapshot_doc").toString(),NucleoU2.ResultadoPrimeraGestion.class);if(recibida>primera.realizadaEn().toEpochMilli())throw new FalloTecnico(409,"RECEPCION_POSTERIOR_A_GESTION");}
            return new Datos(input.texto(),input.canalDeclarado(),Instant.ofEpochMilli(recibida),input.regionAsunto());
        }catch(IllegalArgumentException|ArithmeticException invalid){throw new FalloTecnico(400,"RECTIFICACION_DATOS_INVALIDOS");}
    }
    public ContratosNucleo.Resultado registrar(Operador a,UUID key,UUID com,Solicitud r){
        if(r==null||!"1.0".equals(r.version()))throw new FalloTecnico(400);
        return core.execute(key,a,"RECTIFICAR_INGRESO_1",List.of(com,r),()->{
            autorizar(a,com);var root=core.lock(com);if(r.revision()<0||((Number)root.get("revision")).longValue()!=r.revision())throw new FalloTecnico(409,"VERSION_OBSOLETA");
            try{DeclaracionNna.relato(r.motivo());}catch(IllegalArgumentException e){throw new FalloTecnico(400);}
            var read=leer(com);var nuevo=validar(r.datos(),com);if(read.vigente().equals(nuevo))throw new FalloTecnico(400,"RECTIFICACION_SIN_CAMBIOS");
            core.u2().historia(com,com,"COM",r.revision()+1,a,ACCION,r.motivo(),new Cambio("1.0",read.vigente(),nuevo));core.u2().bumpCom(com);
            FuentesRegistro.correccionManual(db,com,r.revision()+1,nuevo.recibidaEnDeclarada().toEpochMilli(),clock.millis(),a.id());
            return new ContratosNucleo.Resultado(com,r.revision()+1);
        });
    }
}
