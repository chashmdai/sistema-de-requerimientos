package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.identidad.*;import cl.nexbyte.siger.institucional.notificaciones.*;import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.Path;import java.util.*;import java.util.concurrent.*;import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;

class AvisosGestionTest {
 @TempDir Path temp;DatosNucleoTest d;AreasFixture f;NucleoU2 u;NotificacionesInstitucionales n;String siguiente,colaborador;
 final AreasInstitucionalesTest.Reloj clock=new AreasInstitucionalesTest.Reloj();
 @BeforeEach void setup()throws Exception{String mysql=System.getProperty("siger.test.autoridad.mysql");d=new DatosNucleoTest(temp,mysql==null?null:mysql+"_materia?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=10000");if(mysql!=null)limpiar();f=new AreasFixture(d,clock);siguiente=f.user("siguiente.aviso");colaborador=f.user("colaborador.aviso");f.publicar(f.end);f.otorgar(siguiente,"PROFESIONAL","AREA_A");f.otorgar(colaborador,"PROFESIONAL","AREA_A");u=f.core.u2();n=new NotificacionesInstitucionales(d.db,f.cat,new RecursosNotificacion(d.db,f.cat,f.core),clock);}
 void limpiar(){d.limpiar();for(var t:List.of("staff_profile_history","identity_event","staff_assignment","staff_account","org_coverage"))d.db.sql.update("DELETE FROM "+t);}
 @AfterEach void close(){if(d!=null){if(System.getProperty("siger.test.autoridad.mysql")!=null)limpiar();d.close();}}
 UUID key(){return UUID.randomUUID();}long rev(String table,UUID id){return d.db.sql.queryForObject("SELECT revision FROM "+table+" WHERE id=?",Long.class,id.toString());}
 UUID req(){var sol=u.distribuir(f.a(f.dist),key(),d.nuevaWeb(),new NucleoU2.Encaminar(0,new NuevoDestino("UPR","DEMO_SEDE_A",f.headA,"AREA_A"),"Clasificación protegida",null)).id();return u.asumir(f.a(f.headA),key(),sol,new NucleoU2.Asumir(0,f.profA,"Atención concreta",null)).id();}
 UUID caso(UUID q){return u.abrirCaso(f.a(f.headA),key(),q,new NucleoU2.Apertura(rev("core_requirement",q),f.profA,"Objetivo sensible que no va en avisos")).id();}
 void proyectar(){for(int i=0;i<3;i++)n.proyectar();}
 List<Map<String,Object>> rows(String tipo,UUID recurso){return d.db.sql.queryForList("SELECT * FROM institution_notification WHERE resource_type='AVISO_GESTION' AND resource_id=? AND category=? ORDER BY seq",recurso.toString(),tipo);}
 Set<String> destinatarios(String tipo,UUID recurso){var ids=new HashSet<String>();rows(tipo,recurso).forEach(r->ids.add(r.get("recipient_id").toString()));return ids;}
 UUID aviso(String tipo,UUID recurso,String actor){return UUID.fromString(rows(tipo,recurso).stream().filter(r->actor.equals(r.get("recipient_id"))).reduce((a,b)->b).orElseThrow().get("id").toString());}
 void denied(int status,Runnable action){assertEquals(status,assertThrows(FalloTecnico.class,action::run).status);}
 void reasignar(UUID c){f.core.asignar(f.a(f.headA),key(),c,new Asignacion(rev("core_case",c),siguiente,null));}
 void solicitarTermino(UUID c){f.core.revisarCaso(f.a(f.profA),key(),c,new Decision(rev("core_case",c),"Gestión terminada"),"SOLICITUD_TERMINO");}
 void cerrar(UUID c){solicitarTermino(c);f.core.revisarCaso(f.a(f.headA),key(),c,new Decision(rev("core_case",c),"Revisión independiente"),"CIERRE");}

 @Test void aperturaAvisaEntranteYJefaturaSinDuplicarSenalHistoricaNiAcusar(){
  var q=req();var c=caso(q);proyectar();assertEquals(Set.of(f.profA,f.headA),destinatarios("CASO_ASIGNADO",c));
  assertEquals(2,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_notification WHERE resource_id=? AND category='CASO_ASIGNADO'",Long.class,c.toString()));
  UUID id=aviso("CASO_ASIGNADO",c,f.profA);assertEquals("caso/"+c,n.contextoInstitucional(f.a(f.profA),id).ruta());assertTrue(n.contextoInstitucional(f.a(f.profA),id).pendienteActual());n.leer(f.a(f.profA),id);assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_assignment_receipt",Long.class));assertEquals(0,rev("core_case",c));
  for(String other:List.of(f.ti,f.profB,f.headB,colaborador))denied(404,()->n.contextoInstitucional(f.a(other),id));
 }
 @Test void reasignacionAvisaSalienteConReferenciaMinimaSinRestituirCasoYTareas(){
  var q=req();var c=caso(q);proyectar();reasignar(c);proyectar();assertEquals(Set.of(f.profA,siguiente,f.headA),destinatarios("CASO_ASIGNADO",c));
  var old=n.contextoInstitucional(f.a(f.profA),aviso("CASO_ASIGNADO",c,f.profA));assertNull(old.ruta());assertFalse(old.pendienteActual(),"El saliente sin consulta no conoce el acuse del entrante");assertFalse(d.mapper.writeValueAsString(old).contains("Objetivo sensible"));denied(404,()->f.core.caso(f.a(f.profA),c));assertEquals(siguiente,f.core.caso(f.a(siguiente),c).responsable());
  assertEquals("caso/"+c,n.contextoInstitucional(f.a(siguiente),aviso("CASO_ASIGNADO",c,siguiente)).ruta());assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_assignment_receipt",Long.class));assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM core_requirement WHERE id=?",Long.class,q.toString()));
 }
 @Test void acuseManualActualizaPendienteSinAlterarAsignacionNiCrearGestiones(){
  var c=caso(req());proyectar();UUID id=aviso("CASO_ASIGNADO",c,f.profA),asignacion=UUID.fromString(d.db.sql.queryForObject("SELECT assignment_id FROM institution_assignment_current WHERE case_id=?",String.class,c.toString()));
  f.core.continuidad().acusar(f.a(f.profA),key(),c,new ContinuidadU2.Acusar(asignacion));assertFalse(n.contextoInstitucional(f.a(f.profA),id).pendienteActual());assertEquals(0,rev("core_case",c));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM core_action WHERE case_id=?",Long.class,c.toString()));
 }
 @Test void salientePierdeConstanciaSiCuentaOCredencialYaNoEstaAcreditada(){
  var c=caso(req());reasignar(c);proyectar();UUID id=aviso("CASO_ASIGNADO",c,f.profA);assertNull(n.contextoInstitucional(f.a(f.profA),id).ruta());
  var principal=(IdentidadInstitucional.Principal)f.ids.autenticar("prof.a");f.ids.cambiarClavePersonal(principal,new IdentidadInstitucional.CambioClave(f.passwords.get("prof.a"),UUID.randomUUID()+"AZ"));denied(404,()->n.contextoInstitucional(f.a(f.profA),id));assertTrue(n.listar(f.a(f.profA),0,50).items().stream().noneMatch(v->v.id().equals(id)));
 }
 @Test void cuentaAliasDeLaMismaPersonaNoRecibeLaConstanciaNominativaAjena()throws Exception{
  var c=caso(req());reasignar(c);proyectar();UUID id=aviso("CASO_ASIGNADO",c,f.profA);String alias=f.user("alias.saliente");var r=d.mapper.readValue(Base64.getDecoder().decode(d.db.sql.queryForObject("SELECT signed_payload FROM authority_batch ORDER BY revision DESC LIMIT 1",String.class)),AutoridadesInstitucionales.Registro11.class);var bindings=new ArrayList<>(r.vinculos());bindings.add(new AutoridadesInstitucionales.Vinculo(alias,"p-"+f.profA,f.start,f.end,ConcesionFixture.huella(d.db,alias)));f.importar(new AutoridadesInstitucionales.Registro11("1.1",r.revision()+1,clock.instant(),f.end,bindings,r.mandatos(),r.areas(),r.ambitos(),r.cambiosArea()));f.otorgar(alias,"PROFESIONAL","AREA_A");assertEquals(f.f.autoridades.persona(alias),f.f.autoridades.persona(f.profA));denied(404,()->n.contextoInstitucional(f.a(alias),id));assertTrue(n.listar(f.a(alias),0,50).items().isEmpty());
 }
 @Test void devolucionYCierreAvisanEquipoActualSinCerrarReqNiDuplicarCierre(){
  var q=req();var c=caso(q);f.core.equipo().incorporar(f.a(f.headA),key(),c,new EquipoCaso.Incorporar(rev("core_case",c),colaborador,"Apoyo legítimo"));solicitarTermino(c);f.core.revisarCaso(f.a(f.headA),key(),c,new Decision(rev("core_case",c),"Falta completar revisión"),"DEVOLUCION");proyectar();assertEquals(Set.of(f.profA,colaborador,f.headA),destinatarios("CASO_TERMINO_DEVUELTO",c));
  cerrar(c);proyectar();assertEquals(Set.of(f.profA,colaborador,f.headA),destinatarios("CASO_CERRADO",c));assertEquals("ACTIVO",u.requerimiento(f.a(f.headA),q).get("state"));assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_case_closure WHERE case_id=?",Long.class,c.toString()));
 }
 @Test void reqSinCasoTerminadoInformaSinImponerAcuseOGenerarCaso(){
  var q=req();u.actuar(f.a(f.profA),key(),q,new Decision(0,"Atención resuelta sin Caso"),true);proyectar();assertEquals(Set.of(f.profA,f.headA),destinatarios("REQ_TERMINADO",q));UUID id=aviso("REQ_TERMINADO",q,f.profA);n.leer(f.a(f.profA),id);assertEquals("TERMINADO",u.requerimiento(f.a(f.profA),q).get("state"));assertFalse(n.contextoInstitucional(f.a(f.profA),id).pendienteActual());assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_case",Long.class));assertTrue(d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_assignment_receipt",Long.class)==0);
 }
 @Test void reactivacionSoloSeNotificaAlAprobarReaperturaNoAlAutorizarla(){
  var q=req();var c=caso(q);cerrar(c);u.actuar(f.a(f.headA),key(),q,new Decision(rev("core_requirement",q),"Cierre principal expreso"),true);var x=f.core.continuidad();var p=x.solicitarReapertura(f.a(f.profA),key(),c,new ContinuidadU2.Reabrir(rev("core_case",c),"Nuevo antecedente","Fundamento acreditado")).id();x.autorizarReactivacion(f.a(f.headA),key(),p,new ContinuidadU2.Reactivar(0,rev("core_requirement",q),"Reactivación condicionada"));proyectar();assertTrue(rows("REQ_REACTIVADO",q).isEmpty());
  x.decidirReapertura(f.a(f.headA),key(),p,new ContinuidadU2.DecidirReapertura(1,rev("core_case",c),"APROBADA","Revisión de jefatura"));proyectar();assertEquals(Set.of(f.profA,f.headA),destinatarios("REQ_REACTIVADO",q));assertEquals("ACTIVO",u.requerimiento(f.a(f.profA),q).get("state"));assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_case_closure WHERE case_id=?",Long.class,c.toString()));assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM core_management_period WHERE case_id=?",Long.class,c.toString()));
 }
 @Test void falloDeIntencionRevierteCierreYReintentoConservaUnSoloEvento(){
  var q=req();var k=key();var body=new Decision(0,"Cierre atómico sin Caso");d.ddl(sql->sql.execute("ALTER TABLE workflow_event ADD CONSTRAINT aviso_gestion_rollback CHECK(resource_type<>'AVISO_GESTION')"));try{assertThrows(org.springframework.dao.DataAccessException.class,()->u.actuar(f.a(f.profA),k,q,body,true));}finally{d.ddl(sql->sql.execute("ALTER TABLE workflow_event DROP CONSTRAINT aviso_gestion_rollback"));}
  assertEquals("ACTIVO",u.requerimiento(f.a(f.profA),q).get("state"));assertEquals(0,rev("core_requirement",q));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM core_command WHERE id=?",Long.class,k.toString()));var result=u.actuar(f.a(f.profA),k,q,body,true);assertEquals(result,u.actuar(f.a(f.profA),k,q,body,true));assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM workflow_event WHERE resource_type='AVISO_GESTION' AND resource_id=?",Long.class,q.toString()));
 }
 @Test void proyeccionesConcurrentesDeCierreNoDuplicanNotificacionNiCorreo()throws Exception{
  var q=req();u.actuar(f.a(f.profA),key(),q,new Decision(0,"Atención completa"),true);try(var pool=Executors.newFixedThreadPool(2)){var gate=new CountDownLatch(1);var a=pool.submit(()->{gate.await();return n.proyectar();});var b=pool.submit(()->{gate.await();return n.proyectar();});gate.countDown();a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);}
  assertEquals(2,rows("REQ_TERMINADO",q).size());for(var row:rows("REQ_TERMINADO",q))assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM notification_outbox WHERE notification_id=?",Long.class,row.get("id")));
 }
 @Test void primeraAtencionFinalizadaInformaTerminoSinUnaAsignacionFicticia(){
  var q=u.primeraGestion(f.a(f.profA),key(),new NucleoU2.PrimeraGestion("1.0",new Manual("Atención telefónica sintética","TELEFONO",clock.instant().minusSeconds(3600),null,null),new NuevoDestino("UPR","DEMO_SEDE_A",f.headA,"AREA_A"),"Orientación efectivamente prestada","ORIENTACION",clock.instant().minusSeconds(1800))).id();proyectar();assertEquals(Set.of(f.profA,f.headA),destinatarios("REQ_TERMINADO",q));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_sol",Long.class));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_case",Long.class));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM workflow_event WHERE code='REQ_ASIGNADO' AND resource_id=?",Long.class,q.toString()));
 }
 @Test void rootExpiradaRetiraConstanciaDeSalienteSinReabrirOReasignarCaso(){
  var c=caso(req());reasignar(c);proyectar();UUID id=aviso("CASO_ASIGNADO",c,f.profA);clock.now=f.end.plusMillis(1);denied(404,()->n.contextoInstitucional(f.a(f.profA),id));assertTrue(n.listar(f.a(f.profA),0,50).items().isEmpty());assertEquals(siguiente,d.db.sql.queryForObject("SELECT responsible_id FROM core_case WHERE id=?",String.class,c.toString()));
 }
 @Test void fuenteConVersionAlteradaNoFiltraDetalleContadorNiCorreo(){
  var q=req();u.actuar(f.a(f.profA),key(),q,new Decision(0,"Atención resuelta"),true);proyectar();UUID id=aviso("REQ_TERMINADO",q,f.profA);var source=d.db.sql.queryForMap("SELECT seq,snapshot_doc FROM institution_flow_history WHERE resource_id=? AND action_code='AVISO_GESTION_PROGRAMADO'",q.toString());String json=source.get("snapshot_doc").toString().replace("\"version\":\"1.0\"","\"version\":\"2.0\"");d.db.sql.update("UPDATE institution_flow_history SET snapshot_doc=? WHERE seq=?",json,source.get("seq"));denied(404,()->n.contextoInstitucional(f.a(f.profA),id));assertTrue(n.listar(f.a(f.profA),0,50).items().stream().noneMatch(v->v.id().equals(id)));assertEquals(0,n.pendientes(f.a(f.profA)));
 }
 @Test void retirarColaboradorRevocaDetalleListadoYCorreoDelCierreAnterior(){
  var c=caso(req());f.core.equipo().incorporar(f.a(f.headA),key(),c,new EquipoCaso.Incorporar(rev("core_case",c),colaborador,"Apoyo autorizado"));cerrar(c);proyectar();UUID id=aviso("CASO_CERRADO",c,colaborador);var account=f.a(colaborador);assertTrue(n.enviable(account,n.propia(account,id)));f.core.equipo().retirar(f.a(f.headA),key(),c,new EquipoCaso.Retirar(rev("core_case",c),colaborador,null,"Término de colaboración local"));denied(404,()->n.contextoInstitucional(f.a(colaborador),id));assertTrue(n.listar(f.a(colaborador),0,50).items().stream().noneMatch(v->v.id().equals(id)));denied(404,()->f.core.caso(f.a(colaborador),c));
 }
 @Test void responsableDeCasoColaboradorRecibeCierreReqSinExpedienteMixto(){
  var q=req();var a=caso(q);var p=u.colaborar(f.a(f.headA),key(),q,new NucleoU2.Colaborar(rev("core_requirement",q),new NuevoDestino("UPR","DEMO_SEDE_A",f.headB,"AREA_B"),"Línea independienteB")).id();u.resolver(f.a(f.headB),key(),p,new NucleoU2.ResolverColaboracion(0,"ACEPTADA",f.profB,"Competencia de áreaB"));var b=UUID.fromString(d.db.sql.queryForObject("SELECT case_id FROM institution_collaboration WHERE id=?",String.class,p.toString()));cerrar(a);f.core.revisarCaso(f.a(f.profB),key(),b,new Decision(rev("core_case",b),"TrabajoB finalizado"),"SOLICITUD_TERMINO");f.core.revisarCaso(f.a(f.headB),key(),b,new Decision(rev("core_case",b),"RevisiónB independiente"),"CIERRE");u.actuar(f.a(f.headA),key(),q,new Decision(rev("core_requirement",q),"Cierre de todas las líneas"),true);proyectar();assertEquals(Set.of(f.profA,f.profB,f.headA),destinatarios("REQ_TERMINADO",q));var v=n.contextoInstitucional(f.a(f.profB),aviso("REQ_TERMINADO",q,f.profB));assertNull(v.ruta());denied(404,()->u.requerimiento(f.a(f.profB),q));denied(404,()->f.core.caso(f.a(f.profB),a));assertFalse(d.mapper.writeValueAsString(v).contains("Objetivo sensible"));
 }
}
