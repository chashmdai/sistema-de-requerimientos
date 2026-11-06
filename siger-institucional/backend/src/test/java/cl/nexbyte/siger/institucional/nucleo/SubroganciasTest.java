package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.identidad.*;import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.Path;import java.time.*;import java.util.*;import java.util.concurrent.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;

class SubroganciasTest {
    @TempDir Path temp;DatosNucleoTest d;SubroganciasFixture s;AreasInstitucionalesTest.Reloj clock;
    @BeforeEach void setup()throws Exception{clock=new AreasInstitucionalesTest.Reloj();String mysql=System.getProperty("siger.test.autoridad.mysql");d=new DatosNucleoTest(temp,mysql==null?null:mysql+"_subrog?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=10000");if(mysql!=null)limpiarDescartable();s=new SubroganciasFixture(d,clock);}
    void limpiarDescartable(){d.limpiar();for(var table:List.of("staff_profile_history","identity_event","staff_assignment","staff_account","org_coverage"))d.db.sql.update("DELETE FROM "+table);}
    @AfterEach void close(){if(d!=null){if(System.getProperty("siger.test.autoridad.mysql")!=null)limpiarDescartable();d.close();}}
    UUID key(){return UUID.randomUUID();}void denied(int status,Runnable action){assertEquals(status,assertThrows(FalloTecnico.class,action::run).status);}
    boolean head(String actor,String area){return s.f.a(actor).jefeEn("UPR","DEMO_SEDE_A",area);}
    long rev(UUID cas){return d.db.sql.queryForObject("SELECT revision FROM core_case WHERE id=?",Long.class,cas.toString());}
    @Test void designacionNoActivaAprobacionIndependienteEjecucionTiYRetornoSinTraslado(){
        assertTrue(head(s.f.headA,"AREA_A"));assertFalse(head(s.f.profA,"AREA_A"));
        UUID cas=s.f.caso("AREA_A",s.f.headA,s.f.profA);var before=d.db.sql.queryForMap("SELECT * FROM core_case WHERE id=?",cas.toString());
        var tasks=s.f.core.tareas();var task=tasks.crear(s.f.a(s.f.profA),key(),new cl.nexbyte.siger.institucional.notificaciones.TareasPersonales.Edicion(0,"Contacto sintético",null,clock.instant().plusSeconds(60),null,null,null,null,"CASO",cas,s.f.profA,false,null)).id();var taskBefore=d.db.sql.queryForMap("SELECT * FROM institution_task WHERE id=?",task.toString());
        UUID id=s.solicitar();denied(403,()->s.flow.ejecutar(s.f.profA,key(),id,0));denied(404,()->s.flow.decidir(s.f.profA,key(),id,new SubroganciasInstitucionales.Decision(0,"No puede autoaprobar"),true));
        s.aprobar(id);assertFalse(head(s.f.profA,"AREA_A"));s.ejecutar(id);assertTrue(head(s.f.profA,"AREA_A"));assertFalse(head(s.f.headA,"AREA_A"));assertFalse(head(s.aliasSub,"AREA_A"));assertFalse(head(s.f.profA,"AREA_B"));assertTrue(s.f.a(s.f.headA).profesionalEn("UPR","DEMO_SEDE_A","AREA_A"));
        s.f.core.revisarCaso(s.f.a(s.f.profA),key(),cas,new Decision(rev(cas),"Solicita como profesional"),"SOLICITUD_TERMINO");denied(409,()->s.f.core.revisarCaso(s.f.a(s.f.profA),key(),cas,new Decision(rev(cas),"No autocierre subrogante"),"CIERRE"));
        s.flow.terminar(s.f.headA,key(),id,new SubroganciasInstitucionales.Decision(2,"Titular retorna y revisará pendientes"),"RETORNAR");assertTrue(head(s.f.headA,"AREA_A"));assertFalse(head(s.f.profA,"AREA_A"));assertEquals(before.get("responsible_id"),d.db.sql.queryForObject("SELECT responsible_id FROM core_case WHERE id=?",String.class,cas.toString()));assertEquals("REVISION_TERMINO",s.f.core.caso(s.f.a(s.f.profA),cas).estado());
        assertEquals(taskBefore,d.db.sql.queryForMap("SELECT * FROM institution_task WHERE id=?",task.toString()));
        s.f.core.revisarCaso(s.f.a(s.f.headA),key(),cas,new Decision(rev(cas),"Revisión explícita posterior al retorno"),"CIERRE");assertEquals("CERRADO",s.f.core.caso(s.f.a(s.f.profA),cas).estado());assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM core_action WHERE case_id=?",Long.class,cas.toString()));
    }
    @Test void futuroCaducidadRevisionYProrrogaSonRegistrosDistintos(){
        var from=clock.instant().plusSeconds(20);var until=from.plusSeconds(60);var id=s.flow.solicitar(s.f.profA,key(),s.solicitud(from,null,until,null)).id();s.aprobar(id);s.ejecutar(id);assertFalse(head(s.f.profA,"AREA_A"));assertTrue(head(s.f.headA,"AREA_A"));
        clock.now=from;assertTrue(head(s.f.profA,"AREA_A"));s.flow.temporales();clock.now=until;assertFalse(head(s.f.profA,"AREA_A"));assertTrue(head(s.f.headA,"AREA_A"));s.flow.temporales();assertEquals("REVISION_REQUERIDA",d.db.sql.queryForObject("SELECT state FROM institution_substitution WHERE id=?",String.class,id.toString()));
        var next=s.flow.solicitar(s.f.profA,key(),s.solicitud(until,until.plusSeconds(60),null,id)).id();s.aprobar(next);s.ejecutar(next);assertTrue(head(s.f.profA,"AREA_A"));assertNotEquals(id,next);assertEquals(until.toEpochMilli(),d.db.sql.queryForObject("SELECT until_ms FROM institution_substitution WHERE id=?",Long.class,id.toString()));
    }
    @Test void suspensionOmisionesReinicioYCredencialesFallanCerrado()throws Exception{
        UUID id=s.solicitar();s.aprobar(id);s.ejecutar(id);s.funciones=List.of(s.funcion("DESIG_A","COORD_A",s.f.headA,List.of(s.f.profA),List.of(s.f.f.revisor),"AREA_A","SUSPENDIDA"));s.publicar();assertFalse(head(s.f.profA,"AREA_A"));assertFalse(head(s.f.headA,"AREA_A"));
        s.flow.terminar(s.f.f.revisor,key(),id,new SubroganciasInstitucionales.Decision(2,"Revocación por autoridad vigente"),"REVOCAR");s.funciones=List.of();s.publicar();assertFalse(head(s.f.headA,"AREA_A"));
        var restarted=new IdentidadInstitucional(d.db,clock);new AutoridadesInstitucionales(d.db,restarted,d.mapper,clock,Map.of("fixture-autoridad",s.f.f.firma.getPublic()));assertFalse(restarted.operador(s.f.headA).jefeEn("UPR","DEMO_SEDE_A","AREA_A"));assertTrue(restarted.operador(s.f.headA).profesionalEn("UPR","DEMO_SEDE_A","AREA_A"));
    }
    @Test void conflictoConcurrenteEIdempotenciaNoDuplicanFuncion()throws Exception{
        var body=s.solicitud(clock.instant(),clock.instant().plusSeconds(300),null,null);UUID k=key();var first=s.flow.solicitar(s.f.profA,k,body);assertEquals(first,s.flow.solicitar(s.f.profA,k,body));UUID second=s.solicitar();var latch=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(()->{latch.await();try{s.aprobar(first.id());return true;}catch(FalloTecnico e){assertEquals(409,e.status);return false;}});var b=pool.submit(()->{latch.await();try{s.aprobar(second);return true;}catch(FalloTecnico e){assertEquals(409,e.status);return false;}});latch.countDown();assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));}
        assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_substitution WHERE state='APROBADA'",Long.class));assertEquals(2,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_substitution",Long.class));
    }
    @Test void aliasYAdministracionNoAbrenRelatosNiExpedientes(){
        UUID cas=s.f.caso("AREA_A",s.f.headA,s.f.profA),id=s.solicitar();s.flow.decidir(s.aliasAutoridad,key(),id,new SubroganciasInstitucionales.Decision(0,"Autoriza otra cuenta de la misma autoridad"),true);var board=s.flow.bandeja(s.f.ti);String view=d.mapper.writeValueAsString(board);assertFalse(view.contains("Ausencia sintética fundada"));assertFalse(view.contains("designation_doc"));s.ejecutar(id);denied(404,()->s.f.core.caso(s.f.a(s.f.ti),cas));denied(404,()->s.f.core.caso(s.f.a(s.aliasSub),cas));
        long epoch=s.f.ids.operador(s.f.profA).versionAcceso();s.flow.terminar(s.f.f.revisor,key(),id,new SubroganciasInstitucionales.Decision(2,"Revocación fundada"),"REVOCAR");assertTrue(s.f.ids.operador(s.f.profA).versionAcceso()>epoch);assertFalse(head(s.f.profA,"AREA_A"));
    }
    @Test void rollbackNoDejaInstruccionNiCambioDePermiso(){
        UUID id=s.solicitar();s.aprobar(id);assertThrows(IllegalStateException.class,()->d.db.transaction(()->{s.ejecutar(id);throw new IllegalStateException("Rollback sintético");}));assertFalse(head(s.f.profA,"AREA_A"));assertEquals("APROBADA",d.db.sql.queryForObject("SELECT state FROM institution_substitution WHERE id=?",String.class,id.toString()));
    }
    @Test void titularRegionalAutocierraPorRutaExplicitaConDosFuncionesYReqAbierto(){
        var u=s.f.core.u2();var sol=u.distribuir(s.f.a(s.f.dist),key(),d.nuevaWeb(),new NucleoU2.Encaminar(0,new NuevoDestino("UPR","DEMO_SEDE_A",s.f.headA,"AREA_A"),"Clasificación sintética",null)).id();var q=u.asumir(s.f.a(s.f.headA),key(),sol,new NucleoU2.Asumir(0,s.f.headA,"Asume atención profesional","Decisión territorial sintética fundada")).id();var cas=u.abrirCaso(s.f.a(s.f.headA),key(),q,new NucleoU2.Apertura(0,s.f.headA,"Gestión personal titular")).id();
        assertTrue(s.f.core.autocierreRegional(s.f.a(s.f.headA),cas).get("permitido"));denied(409,()->s.f.core.revisarCaso(s.f.a(s.f.headA),key(),cas,new Decision(rev(cas),"No vía ordinaria"),"CIERRE"));denied(404,()->s.f.core.autocierreRegional(s.f.a(s.f.profA),key(),cas,new Decision(rev(cas),"No es titular")));
        var tasks=s.f.core.tareas();var mandatory=tasks.crear(s.f.a(s.f.headA),key(),new cl.nexbyte.siger.institucional.notificaciones.TareasPersonales.Edicion(0,"Obligatoria sintética",null,null,null,null,null,null,"CASO",cas,s.f.headA,true,null)).id();denied(409,()->s.f.core.autocierreRegional(s.f.a(s.f.headA),key(),cas,new Decision(rev(cas),"No omitir obligatoria")));tasks.estado(s.f.a(s.f.headA),key(),mandatory,new cl.nexbyte.siger.institucional.notificaciones.TareasPersonales.Cambio(0,"COMPLETADA",true));
        s.f.core.autocierreRegional(s.f.a(s.f.headA),key(),cas,new Decision(rev(cas),"Titular documenta ambas funciones"));assertEquals("CERRADO",s.f.core.caso(s.f.a(s.f.headA),cas).estado());assertEquals("ACTIVO",d.db.sql.queryForObject("SELECT state FROM core_requirement WHERE id=?",String.class,q.toString()));String snapshot=d.db.sql.queryForObject("SELECT snapshot_doc FROM institution_flow_history WHERE resource_id=? AND action_code='CASO_AUTOCIERRE_REGIONAL_TITULAR'",String.class,cas.toString());assertTrue(snapshot.contains("PROFESIONAL_RESPONSABLE"));assertTrue(snapshot.contains("COORDINACION_REGIONAL_TITULAR"));
    }
    @Test void solicitudesPreviasContinuanConSubroganciaYRetornoSinFalsearAutor(){
        var u=s.f.core.u2();var sol=u.distribuir(s.f.a(s.f.dist),key(),d.nuevaWeb(),new NucleoU2.Encaminar(0,new NuevoDestino("UPR","DEMO_SEDE_A",s.f.headA,"AREA_A"),"Clasificación",null)).id();var q=u.asumir(s.f.a(s.f.headA),key(),sol,new NucleoU2.Asumir(0,s.f.profA,"Atención","Decisión territorial sintética fundada")).id();
        var target=new NuevoDestino("UPR","DEMO_SEDE_A",s.f.headB,"AREA_B");var flow=s.f.core.continuidad();var proposal=flow.transferir(s.f.a(s.f.headA),key(),q,new ContinuidadU2.Trasladar(0,target,"Transferencia fundada","Resumen mínimo")).id();var sub=s.solicitar();s.aprobar(sub);s.ejecutar(sub);
        flow.decidirTransferencia(s.f.a(s.f.headB),key(),proposal,new ContinuidadU2.Resolver(0,"ACEPTADA",null,"Acepta destino manteniendo acto de titular ausente"));assertEquals(s.f.headA,d.db.sql.queryForObject("SELECT requested_by FROM institution_req_transfer WHERE id=?",String.class,proposal.toString()));assertEquals("AREA_B",d.db.sql.queryForObject("SELECT area_id FROM institution_req WHERE requirement_id=?",String.class,q.toString()));
    }
    @Test void reciprocidadCanonicaYSolapamientoDeFuncionesNoSeEludenConAlias()throws Exception{
        s.funciones=List.of(s.funciones.getFirst(),s.funcion("DESIG_B","COORD_B",s.f.headB,List.of(s.f.f.revisor),List.of(s.f.profA),"AREA_B","ACTIVA"));s.publicar();UUID first=s.solicitar();s.aprobar(first);var other=s.flow.solicitar(s.f.f.revisor,key(),new SubroganciasInstitucionales.Solicitud("DESIG_B",clock.instant(),clock.instant().plusSeconds(300),null,"Solicita segunda función",null)).id();
        denied(403,()->s.flow.decidir(s.aliasSub,key(),other,new SubroganciasInstitucionales.Decision(0,"No aprobar recíprocamente usando alias"),true));assertEquals("PENDIENTE",d.db.sql.queryForObject("SELECT state FROM institution_substitution WHERE id=?",String.class,other.toString()));
        s.funciones=List.of(s.funciones.getFirst(),s.funcion("OTRA","OTRA_FUNCION",s.f.headB,List.of(s.f.profB),List.of(s.f.f.revisor),"AREA_A","ACTIVA"));denied(409,()->{try{s.publicar();}catch(FalloTecnico e){throw e;}catch(Exception e){throw new RuntimeException(e);}});
    }
    @Test void avisosDeduplicadosSinCorreoNiAccesoTiYConteosPorPermisoActual(){
        UUID own=s.f.caso("AREA_A",s.f.headA,s.f.headA);s.f.caso("AREA_A",s.f.headA,s.f.profA);assertEquals(2,s.f.core.casos(s.f.a(s.f.headA),null,0,20).total());UUID id=s.solicitar();s.aprobar(id);s.ejecutar(id);assertEquals(1,s.f.core.casos(s.f.a(s.f.headA),null,0,20).total());assertEquals(2,s.f.core.casos(s.f.a(s.f.profA),null,0,20).total());
        var resources=new cl.nexbyte.siger.institucional.notificaciones.RecursosNotificacion(d.db,s.f.cat,s.f.core);var notices=new cl.nexbyte.siger.institucional.notificaciones.NotificacionesInstitucionales(d.db,s.f.cat,resources,clock);for(int i=0;i<5;i++)notices.proyectar();long count=d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_notification WHERE resource_type='SUBROGANCIA'",Long.class);assertTrue(count>=9);assertEquals(0,notices.proyectar());assertEquals(count,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_notification WHERE resource_type='SUBROGANCIA'",Long.class));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_notification WHERE resource_type='SUBROGANCIA' AND recipient_id=?",Long.class,s.f.ti));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM notification_outbox o JOIN institution_notification n ON n.id=o.notification_id WHERE n.resource_type='SUBROGANCIA' AND o.state<>'NO_APLICA'",Long.class));assertFalse(s.f.core.autocierreRegional(s.f.a(s.f.profA),own).get("permitido"));
    }
    @Test void tiAliasDelBeneficiarioNoEjecutaYRestablecerAutoridadNoConservaLaAprobacion()throws Exception{
        s.mandatosExtra.add(new AutoridadesInstitucionales.Mandato("TI_ALIAS","p-"+s.f.f.revisor,"AUTORIDAD_TECNICA_SINTETICA","TI","INSTITUCION",null,null,s.f.start,s.f.end,true,true,"Respaldo sintético de TI","a".repeat(64)));s.publicar();
        var grants=s.f.f.concesiones;var grant=grants.solicitar(s.f.ti,key(),new ConcesionesInstitucionales.Solicitud(s.aliasSub,"TI","INSTITUCION",null,null,clock.instant(),clock.instant().plusSeconds(1000),"Ejecución técnica sintética","1.0",null)).id();grants.decidir(s.f.f.revisor,key(),grant,new ConcesionesInstitucionales.Decision(0,"Independiente para rol TI"),true);grants.ejecutar(s.f.ti,key(),grant,new ConcesionesInstitucionales.Ejecucion(1));
        var id=s.solicitar();s.aprobar(id);denied(403,()->s.flow.ejecutar(s.aliasSub,key(),id,1));s.ejecutar(id);assertTrue(head(s.f.profA,"AREA_A"));
        s.f.ids.clave(s.f.ti,s.f.f.revisor,new IdentidadInstitucional.Clave(UUID.randomUUID()+"AZ",s.f.ids.operador(s.f.f.revisor).versionAcceso()));assertFalse(head(s.f.profA,"AREA_A"));assertFalse(head(s.f.headA,"AREA_A"));
        assertTrue(d.db.sql.queryForObject("SELECT COUNT(*) FROM authority_event WHERE resource_id=? AND result_code='DENEGADO_403'",Long.class,id.toString())>0);
    }
    @Test void alterarPeriodoAprobadoNoModificaElPermiso(){
        var id=s.solicitar();s.aprobar(id);d.db.sql.update("UPDATE institution_substitution SET until_ms=until_ms+1000 WHERE id=?",id.toString());denied(409,()->s.ejecutar(id));assertFalse(head(s.f.profA,"AREA_A"));
    }
    @Test void rutaCentralUsaJefaturaGestionYNoHeredaAutocierreRegional()throws Exception{
        var central=new AutoridadesInstitucionales.Funcion("DESIG_CENTRAL","FUNC_CENTRAL","CENTRAL","JEFATURA_GESTION","DEMO_SEDE_B",s.persona(s.f.headB),List.of(s.persona(s.f.profB)),List.of(s.persona(s.f.f.revisor)),List.of(new AutoridadesInstitucionales.AmbitoFuncion("DIRECTOR","UNIDAD_SEDE","UEP","DEMO_SEDE_B",null)),s.f.start,s.f.end,"Respaldo central sintético","f".repeat(64),"ACTIVA");s.funciones=List.of(s.funciones.getFirst(),central);s.publicar();
        var id=s.flow.solicitar(s.f.profB,key(),new SubroganciasInstitucionales.Solicitud("DESIG_CENTRAL",clock.instant(),clock.instant().plusSeconds(300),null,"Ausencia central sintética",null)).id();s.aprobar(id);s.ejecutar(id);assertTrue(s.f.a(s.f.profB).jefeEn("UEP","DEMO_SEDE_B"));assertFalse(head(s.f.profB,"AREA_A"));denied(403,()->s.flow.titularRegional(s.f.profB,"UEP","DEMO_SEDE_B",null));
    }
}
