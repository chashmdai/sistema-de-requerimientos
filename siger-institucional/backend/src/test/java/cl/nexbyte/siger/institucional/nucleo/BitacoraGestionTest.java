package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.robustez.FalloTecnico;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;

class BitacoraGestionTest {
    @TempDir Path temp;
    DatosAvisosTest f;
    BitacoraGestion bit;
    UUID req,cas;
    @BeforeEach void setup()throws Exception{
        String mysql=System.getProperty("siger.test.autoridad.mysql");
        f=new DatosAvisosTest(temp,mysql==null?null:mysql+"_u2?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=10000");
        bit=f.core.bitacora();req=nuevoReq();cas=abrir(req);
    }
    @AfterEach void close(){if(f!=null)f.close();}
    UUID key(){return UUID.randomUUID();}
    UUID nuevoReq(){var sol=f.core.u2().distribuir(f.a(f.dist),key(),f.d.nuevaWeb(),new NucleoU2.Encaminar(0,new NuevoDestino("UPR","DEMO_SEDE_A",f.head),"Clasificación sintética",null)).id();return f.core.u2().asumir(f.a(f.head),key(),sol,new NucleoU2.Asumir(0,f.prof,"Atención sin Caso",null)).id();}
    UUID abrir(UUID req){long v=f.d.db.sql.queryForObject("SELECT revision FROM core_requirement WHERE id=?",Long.class,req.toString());return f.core.u2().abrirCaso(f.a(f.head),key(),req,new NucleoU2.Apertura(v,f.prof,"Gestión concreta")).id();}
    long rev(){return f.d.db.sql.queryForObject("SELECT revision FROM core_case WHERE id=?",Long.class,cas.toString());}
    BitacoraGestion.Registrar registro(long rev,String description,String original){return new BitacoraGestion.Registrar("1.0",rev,f.tiempo.instant().minusSeconds(100),description,original,original==null?null:"Corrección fundada de constancia anterior");}
    BitacoraGestion.Vista vista(){return bit.caso(f.a(f.prof),cas,0,25);}
    void denied(int status,Runnable op){assertEquals(status,assertThrows(FalloTecnico.class,op::run).status);}

    @Test void fechaEfectivaYRegistroSonDistintosYLaActuacionNoSeDuplica(){
        Instant now=f.tiempo.instant();bit.registrarCaso(f.a(f.prof),key(),cas,registro(rev(),"Contacto efectivamente realizado",null));
        var e=vista().entradas().getFirst();assertEquals(now.minusSeconds(100),e.realizadaEn());assertEquals(now,e.registradaEn());assertEquals(f.prof,e.ejecutor());assertEquals(f.prof,e.registrador());assertEquals("1.0",e.version());
        assertEquals(1,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM core_action",Long.class));assertEquals(0,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_task",Long.class));
        assertEquals(e.descripcion(),f.core.caso(f.a(f.prof),cas).actuaciones().getFirst().texto());
        assertEquals(1,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM matter_access_event WHERE action_code='CONSULTAR_BITACORA_CASO'",Long.class));
    }
    @Test void rectificacionEsNuevaConstanciaYConservaOriginalSinRamasAmbiguas(){
        f.core.actuar(f.a(f.prof),key(),cas,new Decision(rev(),"Texto histórico inmutable"));
        var original=vista().entradas().getFirst();assertNull(original.realizadaEn());assertNull(original.ejecutor());assertNull(original.version());
        var before=f.d.db.sql.queryForMap("SELECT * FROM core_action WHERE id=?",original.id());
        bit.registrarCaso(f.a(f.prof),key(),cas,registro(rev(),"Descripción rectificada",original.id()));
        assertEquals(before,f.d.db.sql.queryForMap("SELECT * FROM core_action WHERE id=?",original.id()));
        var entries=vista().entradas();var corrected=entries.stream().filter(e->e.rectifica()!=null).findFirst().orElseThrow();
        assertEquals(original.id(),corrected.rectifica());assertEquals(before.get("body_text"),bit.constanciaCaso(f.a(f.prof),cas,original.id()).descripcion());assertEquals(corrected.id(),entries.stream().filter(e->e.id().equals(original.id())).findFirst().orElseThrow().rectificadaPor());
        denied(409,()->bit.registrarCaso(f.a(f.prof),key(),cas,registro(rev(),"No abrir otra rama",original.id())));
        bit.registrarCaso(f.a(f.head),key(),cas,registro(rev(),"Rectificación de la rectificación",corrected.id()));assertEquals(3,vista().total());
    }
    @Test void reqSinCasoUsaLaMismaHistoriaYNoCreaCasoNiRequerimiento(){
        UUID q=nuevoReq();long requirements=f.d.db.sql.queryForObject("SELECT COUNT(*) FROM core_requirement",Long.class);
        bit.registrarRequerimiento(f.a(f.prof),key(),q,registro(0,"Atención profesional sin expediente",null));
        var view=bit.requerimiento(f.a(f.prof),q,0,25);var e=view.entradas().getFirst();assertTrue(e.id().startsWith("REQ:"));assertEquals(f.prof,e.ejecutor());assertEquals(e,bit.constanciaReq(f.a(f.prof),q,e.id()));assertEquals(1,view.total());
        assertEquals(1,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_flow_history WHERE resource_id=? AND action_code='REQ_ATENCION_SIN_CASO'",Long.class,q.toString()));
        bit.registrarRequerimiento(f.a(f.head),key(),q,registro(view.revision(),"Corrección de gestión sin Caso",e.id()));
        assertEquals(2,bit.requerimiento(f.a(f.prof),q,0,25).total());assertEquals(requirements,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM core_requirement",Long.class));
        assertEquals(0,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM core_case WHERE requirement_id=?",Long.class,q.toString()));
        f.core.u2().actuar(f.a(f.head),key(),q,new Decision(2,"Cierre explícito de la atención"),true);
        assertFalse(bit.requerimiento(f.a(f.prof),q,0,25).puedeRegistrar());assertEquals(2,bit.requerimiento(f.a(f.prof),q,0,25).total());
        denied(409,()->bit.registrarRequerimiento(f.a(f.prof),key(),q,registro(3,"No actuación posterior",null)));
    }
    @Test void reqConCasoConservaSuHistoriaPreviaPeroImpideNuevasGestionesGlobales(){
        UUID q=nuevoReq();f.core.u2().actuar(f.a(f.prof),key(),q,new Decision(0,"Gestión anterior sin fecha efectiva"),false);
        var old=bit.requerimiento(f.a(f.head),q,0,25).entradas().getFirst();assertNull(old.realizadaEn());assertNull(old.ejecutor());abrir(q);
        assertEquals(1,bit.requerimiento(f.a(f.head),q,0,25).total());assertFalse(bit.requerimiento(f.a(f.head),q,0,25).puedeRegistrar());
        denied(409,()->bit.registrarRequerimiento(f.a(f.prof),key(),q,registro(2,"Debe registrarse en Caso autorizado",null)));
        denied(404,()->bit.requerimiento(f.a(f.otro),q,0,25));
    }
    @Test void noAccesoPorTiOtraUnidadOReferenciaACasoAjeno(){
        bit.registrarCaso(f.a(f.prof),key(),cas,registro(rev(),"Gestión en mi Caso",null));String original=vista().entradas().getFirst().id();UUID otroCaso=abrir(req);
        denied(404,()->bit.caso(f.a(f.ti),cas,0,25));denied(404,()->bit.caso(f.a(f.otro),cas,0,25));
        denied(404,()->bit.registrarCaso(f.a(f.ti),key(),cas,registro(rev(),"No es facultad técnica",null)));
        denied(404,()->bit.registrarCaso(f.a(f.prof),key(),otroCaso,registro(0,"No rectifica otro expediente",original)));
        assertEquals(0,bit.caso(f.a(f.prof),otroCaso,0,25).total());denied(404,()->bit.constanciaCaso(f.a(f.prof),otroCaso,original));
    }
    @Test void retiroDeEquipoRevocaLecturaYEscrituraSinEsperarLogin(){
        String col=f.cuenta("bitacora.col","UPR","DEMO_SEDE_A","PROFESIONAL","UNIDAD_SEDE");
        f.core.equipo().incorporar(f.a(f.prof),key(),cas,new EquipoCaso.Incorporar(rev(),col,"Integrante autorizado"));var actor=f.a(col);
        bit.registrarCaso(actor,key(),cas,registro(rev(),"Gestión del colaborador real",null));
        f.core.equipo().retirar(f.a(f.head),key(),cas,new EquipoCaso.Retirar(rev(),col,null,"Fin del apoyo sin tareas pendientes"));
        denied(404,()->bit.caso(actor,cas,0,25));denied(404,()->bit.registrarCaso(actor,key(),cas,registro(rev(),"No conserva acceso por autoría",null)));
        assertEquals(col,vista().entradas().getFirst().ejecutor());
    }
    @Test void reintentoYVersionObsoletaNoDuplicanNiModificanLaGestion(){
        UUID key=key();var input=registro(rev(),"Una sola actuación",null);var result=bit.registrarCaso(f.a(f.prof),key,cas,input);
        f.core.revisarCaso(f.a(f.prof),key(),cas,new Decision(rev(),"Solicita término"),"SOLICITUD_TERMINO");
        assertEquals(result,bit.registrarCaso(f.a(f.prof),key,cas,input));assertEquals(1,vista().total());
        denied(409,()->bit.registrarCaso(f.a(f.prof),key(),cas,input));
        denied(409,()->bit.registrarCaso(f.a(f.prof),key,cas,registro(0,"Payload cambiado",null)));
    }
    @Test void dosRectificacionesConcurrentesConservanSoloUnSucesor()throws Exception{
        bit.registrarCaso(f.a(f.prof),key(),cas,registro(rev(),"Original",null));String original=vista().entradas().getFirst().id();long revision=rev();var go=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            Callable<Boolean> op=()->{go.await();try{bit.registrarCaso(f.a(f.prof),key(),cas,registro(revision,"Corrección concurrente",original));return true;}catch(FalloTecnico e){assertEquals(409,e.status);return false;}};
            var a=pool.submit(op);var b=pool.submit(op);go.countDown();assertNotEquals(a.get(8,TimeUnit.SECONDS),b.get(8,TimeUnit.SECONDS));
        }
        assertEquals(2,vista().total());assertEquals(1,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_case_action WHERE rectifies_id=?",Long.class,original));
    }
    @Test void fallaAtomicaRevierteActuacionRevisionComandoYEvento(){
        var initial=f.d.db.sql.queryForMap("SELECT * FROM core_case WHERE id=?",cas.toString());long events=f.d.db.sql.queryForObject("SELECT COUNT(*) FROM core_event",Long.class);UUID key=key();
        f.d.ddl(sql->sql.execute("ALTER TABLE institution_case_action ADD CONSTRAINT bitacora_rollback CHECK(effective_ms=0)"));
        try{
            var failure=assertThrows(org.springframework.dao.DataAccessException.class,()->bit.registrarCaso(f.a(f.prof),key,cas,registro(rev(),"La transacción no puede quedar a medias",null)));
            var sql=assertInstanceOf(java.sql.SQLException.class,org.springframework.core.NestedExceptionUtils.getMostSpecificCause(failure));
            assertTrue(sql.getMessage().toLowerCase(Locale.ROOT).contains("bitacora_rollback"));
            if(System.getProperty("siger.test.autoridad.mysql")==null){assertEquals("23513",sql.getSQLState());assertEquals(23513,sql.getErrorCode());}
            else{assertEquals("HY000",sql.getSQLState());assertEquals(3819,sql.getErrorCode());}
        }
        finally{f.d.ddl(sql->sql.execute("ALTER TABLE institution_case_action DROP "+(System.getProperty("siger.test.autoridad.mysql")==null?"CONSTRAINT":"CHECK")+" bitacora_rollback"));}
        assertEquals(initial,f.d.db.sql.queryForMap("SELECT * FROM core_case WHERE id=?",cas.toString()));assertEquals(0,vista().total());assertEquals(events,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM core_event",Long.class));assertEquals(0,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM core_command WHERE id=?",Long.class,key.toString()));
    }
    @Test void noInventaFechasFuturasVersionesCamposOMotivos(){
        denied(400,()->bit.registrarCaso(f.a(f.prof),key(),cas,new BitacoraGestion.Registrar("2.0",0,f.tiempo.instant(),"Versión desconocida",null,null)));
        denied(400,()->bit.registrarCaso(f.a(f.prof),key(),cas,new BitacoraGestion.Registrar("1.0",0,f.tiempo.instant().plusMillis(1),"Gestión futura",null,null)));
        denied(400,()->bit.registrarCaso(f.a(f.prof),key(),cas,new BitacoraGestion.Registrar("1.0",0,null,"Falta fecha",null,null)));
        denied(400,()->bit.registrarCaso(f.a(f.prof),key(),cas,new BitacoraGestion.Registrar("1.0",0,f.tiempo.instant(),"Motivo sin rectificación",null,"No vinculado")));
        assertEquals(0,vista().total());
    }
    @Test void paginacionConteosYHistorialNoRevelanOtrosContextos(){
        for(int i=0;i<3;i++)bit.registrarCaso(f.a(f.prof),key(),cas,registro(rev(),"Gestión "+i,null));
        assertEquals(3,bit.caso(f.a(f.prof),cas,0,2).total());assertEquals(2,bit.caso(f.a(f.prof),cas,0,2).entradas().size());assertEquals(1,bit.caso(f.a(f.prof),cas,1,2).entradas().size());
        denied(400,()->bit.caso(f.a(f.prof),cas,-1,25));denied(400,()->bit.caso(f.a(f.prof),cas,0,101));
        assertEquals(0,bit.requerimiento(f.a(f.head),req,0,25).total());
    }
    @Test void cierreDeReqCompiteConGestionSinCasoSinPerderNingunaDecision()throws Exception{
        UUID q=nuevoReq();var go=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            var close=pool.submit(()->{go.await();try{f.core.u2().actuar(f.a(f.head),key(),q,new Decision(0,"Cierre explícito"),true);return true;}catch(FalloTecnico e){assertEquals(409,e.status);return false;}});
            var write=pool.submit(()->{go.await();try{bit.registrarRequerimiento(f.a(f.prof),key(),q,registro(0,"Gestión concurrente efectivamente realizada",null));return true;}catch(FalloTecnico e){assertEquals(409,e.status);return false;}});
            go.countDown();boolean closed=close.get(8,TimeUnit.SECONDS);assertNotEquals(closed,write.get(8,TimeUnit.SECONDS));
            assertEquals(closed?0:1,bit.requerimiento(f.a(f.head),q,0,25).total());
            assertEquals(closed?"TERMINADO":"ACTIVO",f.core.u2().requerimiento(f.a(f.head),q).get("state"));
        }
    }
}
