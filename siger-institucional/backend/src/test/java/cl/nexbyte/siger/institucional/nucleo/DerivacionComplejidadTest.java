package cl.nexbyte.siger.institucional.nucleo;
import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import cl.nexbyte.siger.institucional.robustez.FalloTecnico;
import java.nio.file.Path;import java.util.*;import java.util.concurrent.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;
class DerivacionComplejidadTest {
 @TempDir Path dir;DatosNucleoTest data;NucleoInstitucional core;Operador dist,head,target,prof,other;UUID cid,c1,c2;Destino source;
 @BeforeEach void setup()throws Exception{
  data=new DatosNucleoTest(dir);core=data.core;dist=data.actor("demo.distribucion");head=data.actor("demo.jefatura.upr.a");target=data.actor("demo.jefatura.upd.a");prof=data.actor("demo.profesional.upr.a");other=data.actor("demo.profesional2.upr.a");cid=data.web();
  core.distribuir(dist,UUID.randomUUID(),cid,new Distribucion(0,List.of(new NuevoDestino("UPR","DEMO_SEDE_A",head.id()))));source=core.detalle(head,cid).destinos().getFirst();
  c1=core.abrirCaso(head,UUID.randomUUID(),source.id(),new Asignacion(source.revision(),prof.id(),"baja: texto histórico no clasificante")).id();source=core.detalle(head,cid).destinos().getFirst();
  c2=core.abrirCaso(head,UUID.randomUUID(),source.id(),new Asignacion(source.revision(),other.id(),null)).id();
 }
 @AfterEach void close(){if(data!=null)data.close();}
 long rev(UUID id){return core.caso(head,id).revision();}
 void baja(UUID id){core.clasificar(head,UUID.randomUUID(),id,new Clasificacion(rev(id),"BAJA","Fundamento sintético de clasificación"));}
 UUID request(){return core.solicitarDerivacion(head,UUID.randomUUID(),c2,new SolicitarDerivacion(rev(c2),"UPD","DEMO_SEDE_A","Motivo sintético","Antecedentes explícitos de evaluación, sin bitácora completa")).id();}
 AceptarDerivacion acceptance(UUID r){var t=core.derivacion(target,r);return new AceptarDerivacion(t.revision(),t.revisionOrigen(),null,null,"Fundamento sintético de aceptación");}
 void fail(Runnable action,int status){assertThatThrownBy(action::run).isInstanceOf(FalloTecnico.class).extracting(e->((FalloTecnico)e).status).isEqualTo(status);}
 long count(String table){return data.db.sql.queryForObject("SELECT COUNT(*) FROM "+table,Long.class);}
 @Test void bajaCierraProfesionalSinActuacionPreviaNiAprobacionYConservaHermanos(){
  baja(c1);var key=UUID.randomUUID();var body=new Decision(rev(c1),"Fundamento sintético del cierre directo");
  var result=core.revisarCaso(prof,key,c1,body,"CIERRE");assertThat(core.revisarCaso(prof,key,c1,body,"CIERRE")).isEqualTo(result);
  var c=core.caso(prof,c1);assertThat(c.estado()).isEqualTo("CERRADO");assertThat(c.modalidadCierre()).isEqualTo("PROFESIONAL_BAJA");assertThat(c.actuaciones()).isEmpty();assertThat(c.revisiones().getLast().autor()).isEqualTo(prof.id());assertThat(c.revisiones().getLast().fecha()).isNotNull();
  assertThat(core.caso(head,c2).estado()).isEqualTo("EN_GESTION");assertThat(core.detalle(head,cid).destinos().getFirst().estado()).isEqualTo("EVALUANDO");assertThat(core.detalle(dist,cid).comunicacion().estadoRequerimiento()).isEqualTo("ACTIVO");
  assertThat(core.detalle(head,cid).historial()).extracting(Evento::codigo).contains("CASO_CERRADO_PROFESIONAL_BAJA");
 }
 @Test void inclusoBajaNoPuedeCerrarMientrasEsperaAceptacionYConservaActuaciones(){
  baja(c2);request();long current=rev(c2);
  fail(()->core.revisarCaso(other,UUID.randomUUID(),c2,new Decision(current,"BAJA no permite cerrar durante derivación pendiente"),"CIERRE"),409);
  core.actuar(other,UUID.randomUUID(),c2,new Decision(current,"La espera no congela la actuación"));assertThat(core.caso(head,c2).estado()).isEqualTo("EN_GESTION");assertThat(core.caso(head,c2).actuaciones()).hasSize(1);
 }
 @Test void textoNoClasificaYProfesionalNoPuedeAutoclasificarNiCerrarCasoAjeno(){
  assertThat(core.caso(head,c1).clasificacion()).isNull();fail(()->core.revisarCaso(prof,UUID.randomUUID(),c1,new Decision(rev(c1),"Cierre inválido"),"CIERRE"),409);
  fail(()->core.clasificar(prof,UUID.randomUUID(),c1,new Clasificacion(rev(c1),"BAJA","Autorización inventada")),404);baja(c1);
  fail(()->core.revisarCaso(other,UUID.randomUUID(),c1,new Decision(rev(c1),"Caso ajeno"),"CIERRE"),404);
  assertThatThrownBy(()->core.revisarCaso(prof,UUID.randomUUID(),c1,new Decision(rev(c1)," "),"CIERRE")).isInstanceOf(IllegalArgumentException.class);
 }
 @Test void reclasificacionYReasignacionInvalidanDecisionesObsoletasEHistorialPermanece(){
  baja(c1);long stale=rev(c1);core.clasificar(head,UUID.randomUUID(),c1,new Clasificacion(stale,null,"Pendiente de clasificación nuevamente"));
  fail(()->core.revisarCaso(prof,UUID.randomUUID(),c1,new Decision(stale,"Obsoleta"),"CIERRE"),409);fail(()->core.revisarCaso(prof,UUID.randomUUID(),c1,new Decision(rev(c1),"Sin BAJA"),"CIERRE"),409);
  baja(c1);long obsolete=rev(c1);core.asignar(head,UUID.randomUUID(),c1,new Asignacion(obsolete,other.id(),"Descripción intacta"));
  fail(()->core.revisarCaso(prof,UUID.randomUUID(),c1,new Decision(obsolete,"Ya no responsable"),"CIERRE"),404);assertThat(core.caso(head,c1).clasificaciones()).hasSize(3);
 }
 @Test void solicitudMantieneOrigenYBitacoraSinNuevoCasoNiCierreYNoSeAutoacepta(){
  var r=request();assertThat(count("core_case")).isEqualTo(2);assertThat(core.caso(head,c2).estado()).isEqualTo("EN_GESTION");
  core.actuar(other,UUID.randomUUID(),c2,new Decision(rev(c2),"Actuación mientras espera"));assertThat(core.caso(head,c2).actuaciones()).hasSize(1);
  fail(()->core.revisarCaso(other,UUID.randomUUID(),c2,new Decision(rev(c2),"No cerrar pendiente"),"CIERRE"),409);fail(()->request(),409);
  fail(()->core.aceptarDerivacion(head,UUID.randomUUID(),r,acceptance(r)),404);var foreign=data.actor("demo.jefatura.uep.b");fail(()->core.derivacion(foreign,r),404);fail(()->core.aceptarDerivacion(foreign,UUID.randomUUID(),r,acceptance(r)),404);
  fail(()->core.detalle(target,cid),404);assertThat(core.entrantes(target)).hasSize(1);assertThat(core.derivacion(target,r).antecedentesEvaluacion()).startsWith("Antecedentes explícitos");
 }
 @Test void aceptacionAtomicaMismoRequerimientoSinHeredarResponsableOComplejidad(){
  baja(c2);var r=request();var before=core.caso(head,c2);var key=UUID.randomUUID();var body=acceptance(r);var result=core.aceptarDerivacion(target,key,r,body);
  assertThat(core.aceptarDerivacion(target,key,r,body)).isEqualTo(result);assertThat(count("core_case")).isEqualTo(3);var created=core.caso(target,result.id());
  assertThat(created.requerimientoId()).isEqualTo(before.requerimientoId());assertThat(created.casoOrigen()).isEqualTo(c2);assertThat(created.responsable()).isNull();assertThat(created.clasificacion()).isNull();assertThat(created.complejidadProvisional()).isNull();assertThat(created.asignaciones()).isEmpty();
  assertThat(core.caso(head,c2).modalidadCierre()).isEqualTo("DERIVACION");assertThat(core.caso(head,c1).estado()).isEqualTo("EN_GESTION");assertThat(core.derivacion(target,r).estado()).isEqualTo("ACEPTADA");assertThat(core.detalle(dist,cid).comunicacion().estadoRequerimiento()).isEqualTo("ACTIVO");
 }
 @Test void versionOrigenActualNecesariaPeroBitacoraNoQuedaCongelada(){var r=request();var stale=acceptance(r);core.actuar(other,UUID.randomUUID(),c2,new Decision(rev(c2),"Nuevo antecedente"));fail(()->core.aceptarDerivacion(target,UUID.randomUUID(),r,stale),409);assertThat(core.derivacion(target,r).estado()).isEqualTo("PENDIENTE");core.aceptarDerivacion(target,UUID.randomUUID(),r,acceptance(r));}
 @Test void dosAceptacionesConcurrentesMismaClaveRecuperanMismoResultado()throws Exception{
  var r=request();var body=acceptance(r);var key=UUID.randomUUID();var start=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(2)){var first=pool.submit(()->{start.await();return core.aceptarDerivacion(target,key,r,body);});var second=pool.submit(()->{start.await();return core.aceptarDerivacion(target,key,r,body);});start.countDown();assertThat(first.get(10,TimeUnit.SECONDS)).isEqualTo(second.get(10,TimeUnit.SECONDS));}
  assertThat(count("core_case")).isEqualTo(3);assertThat(count("core_requirement")).isEqualTo(1);
 }
 @Test void rollbackPosteriorAAceptacionConservaOrigenSolicitudYPermiteReintentar(){
  var r=request();var body=acceptance(r);var key=UUID.randomUUID();data.ddl(sql->sql.execute("ALTER TABLE core_event ADD CONSTRAINT reject_transfer CHECK(code<>'CASO_DERIVADO_CREADO_SIN_ASIGNACION')"));
  try{assertThatThrownBy(()->core.aceptarDerivacion(target,key,r,body)).isInstanceOf(org.springframework.dao.DataAccessException.class);assertThat(core.caso(head,c2).estado()).isEqualTo("EN_GESTION");assertThat(core.derivacion(target,r).estado()).isEqualTo("PENDIENTE");assertThat(count("core_case")).isEqualTo(2);assertThat(count("core_destination")).isEqualTo(1);}
  finally{data.ddl(sql->sql.execute("ALTER TABLE core_event DROP "+(System.getenv("ROBUST_DB_URL")==null?"CONSTRAINT":"CHECK")+" reject_transfer"));}
  core.aceptarDerivacion(target,key,r,body);assertThat(count("core_case")).isEqualTo(3);
 }
 @Test void destinoResueltoYRechazoReaperturaNoSeHabilitan(){
  var detail=core.detalle(dist,cid);core.distribuir(dist,UUID.randomUUID(),cid,new Distribucion(detail.comunicacion().revision(),List.of(new NuevoDestino("UPD","DEMO_SEDE_A",target.id()))));var d=core.detalle(target,cid).destinos().getFirst();core.resolverDestino(target,UUID.randomUUID(),d.id(),new Decision(d.revision(),"Atención ya respondida"),true);
  var r=request();var t=core.derivacion(target,r);fail(()->core.aceptarDerivacion(target,UUID.randomUUID(),r,new AceptarDerivacion(t.revision(),t.revisionOrigen(),d.id(),d.revision()+1,"No reabrir")),409);
  assertThat(count("core_case")).isEqualTo(2);assertThat(core.derivacion(target,r).estado()).isEqualTo("PENDIENTE");fail(()->core.decisionDerivacionPendiente(target,r),409);fail(()->core.transicionPendiente(head,c2),409);
 }
 @Test void aceptaDestinoExistenteEvaluandoSinCrearOtro(){
  var detail=core.detalle(dist,cid);core.distribuir(dist,UUID.randomUUID(),cid,new Distribucion(detail.comunicacion().revision(),List.of(new NuevoDestino("UPD","DEMO_SEDE_A",target.id()))));var d=core.detalle(target,cid).destinos().getFirst();
  var r=request();var t=core.derivacion(target,r);var result=core.aceptarDerivacion(target,UUID.randomUUID(),r,new AceptarDerivacion(t.revision(),t.revisionOrigen(),d.id(),d.revision(),"Aceptar destino aún en evaluación"));assertThat(count("core_destination")).isEqualTo(2);assertThat(core.caso(target,result.id()).destinoId()).isEqualTo(d.id());
 }
 @Test void dosAceptacionesConClavesDiferentesSoloUnaConfirma()throws Exception{
  var r=request();var body=acceptance(r);var start=new CountDownLatch(1);
  Callable<Integer> accept=()->{start.await();try{core.aceptarDerivacion(target,UUID.randomUUID(),r,body);return 200;}catch(FalloTecnico e){return e.status;}};
  try(var pool=Executors.newFixedThreadPool(2)){var one=pool.submit(accept);var two=pool.submit(accept);start.countDown();assertThat(List.of(one.get(10,TimeUnit.SECONDS),two.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);}
  assertThat(count("core_case")).isEqualTo(3);
 }
 @Test void reasignacionDuranteIntentoDeCierreNoPermiteCerrarConAutorizacionAnterior()throws Exception{
  baja(c1);var decision=new Decision(rev(c1),"Intento concurrente sintético");
  try(var executor=Executors.newSingleThreadExecutor()){
   Future<Integer> result=data.db.transaction(()->{data.db.sql.queryForObject("SELECT comunicacion_id FROM communication_registry WHERE comunicacion_id=? FOR UPDATE",String.class,cid.toString());
    var started=new CountDownLatch(1);var future=executor.submit(()->{started.countDown();try{core.revisarCaso(prof,UUID.randomUUID(),c1,decision,"CIERRE");return 200;}catch(FalloTecnico e){return e.status;}});
    try{assertThat(started.await(3,TimeUnit.SECONDS)).isTrue();}catch(InterruptedException e){throw new RuntimeException(e);}
    core.asignar(head,UUID.randomUUID(),c1,new Asignacion(decision.revision(),other.id(),"Reasignación concurrente"));return future;});
   assertThat(result.get(10,TimeUnit.SECONDS)).isIn(404,409);assertThat(core.caso(head,c1).estado()).isEqualTo("EN_GESTION");assertThat(core.caso(head,c1).responsable()).isEqualTo(other.id());
  }
 }
}

