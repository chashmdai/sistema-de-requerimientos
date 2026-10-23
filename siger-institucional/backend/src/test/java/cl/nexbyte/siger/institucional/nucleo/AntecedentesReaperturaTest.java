package cl.nexbyte.siger.institucional.nucleo;
import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import cl.nexbyte.siger.institucional.robustez.FalloTecnico;
import java.nio.file.Path;import java.util.*;import java.util.concurrent.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;

class AntecedentesReaperturaTest {
 @TempDir Path dir;DatosNucleoTest d;NucleoInstitucional core;Operador dist,head,prof,other,foreign;UUID root,c1,c2;Destino dest;
 @BeforeEach void setup()throws Exception{
  d=new DatosNucleoTest(dir);core=d.core;dist=d.actor("demo.distribucion");head=d.actor("demo.jefatura.upr.a");prof=d.actor("demo.profesional.upr.a");other=d.actor("demo.profesional2.upr.a");foreign=d.actor("demo.jefatura.uep.b");root=d.web();
  core.distribuir(dist,UUID.randomUUID(),root,new Distribucion(0,List.of(new NuevoDestino("UPR","DEMO_SEDE_A",head.id()))));dest=core.detalle(head,root).destinos().getFirst();
  c1=core.abrirCaso(head,UUID.randomUUID(),dest.id(),new Asignacion(dest.revision(),prof.id(),null)).id();dest=core.detalle(head,root).destinos().getFirst();
  c2=core.abrirCaso(head,UUID.randomUUID(),dest.id(),new Asignacion(dest.revision(),other.id(),null)).id();
 }
 @AfterEach void close(){if(d!=null)d.close();}
 long rev(UUID c){return core.caso(head,c).revision();}
 void fail(Runnable r,int code){assertThatThrownBy(r::run).isInstanceOf(FalloTecnico.class).extracting(e->((FalloTecnico)e).status).isEqualTo(code);}
 UUID link(UUID c){UUID child=d.nuevaWeb();core.vincular(dist,UUID.randomUUID(),child,new Vinculacion(root,c,"Vínculo sintético"));return child;}
 void eval(UUID child,String decision){var l=core.antecedente(prof,child);core.evaluarAntecedente(prof,UUID.randomUUID(),child,new EvaluarAntecedente(l.revision(),rev(c1),decision,"Fundamento sintético"));}
 void closeCase(UUID c,Operador p){core.clasificar(head,UUID.randomUUID(),c,new Clasificacion(rev(c),"BAJA","Clasificación sintética"));core.revisarCaso(p,UUID.randomUUID(),c,new Decision(rev(c),"Cierre sintético"),"CIERRE");}
 UUID pending(UUID child){return core.solicitarReapertura(prof,UUID.randomUUID(),c1,new SolicitarReapertura(rev(c1),List.of(child),"Nuevos antecedentes admitidos",null,null)).id();}
 DecidirReapertura decision(UUID r){var x=core.reapertura(head,r);return new DecidirReapertura(x.revision(),x.revisionCasoActual(),"Decisión sintética de jefatura");}
 long count(String t){return d.db.sql.queryForObject("SELECT COUNT(*) FROM "+t,Long.class);}
 @Test void vincularNoAdmiteNiReabreYNoConcedeAccesoAlAgrupador(){
  closeCase(c1,prof);UUID child=link(c1);assertThat(core.antecedente(prof,child).estado()).isEqualTo("PENDIENTE");assertThat(core.caso(prof,c1).estado()).isEqualTo("CERRADO");
  fail(()->core.antecedente(other,child),404);fail(()->core.antecedente(foreign,child),404);fail(()->core.detalle(prof,child),404);
  fail(()->core.evaluarAntecedente(other,UUID.randomUUID(),child,new EvaluarAntecedente(0,rev(c1),"ADMITIDA","Ajeno")),404);
  assertThat(core.antecedente(prof,child).contenido().texto()).isEqualTo("Relato sintético del núcleo");
 }
 @Test void noAdmisionConservaOriginalVinculoMotivoYEstado(){UUID child=link(c1);var before=d.db.sql.queryForMap("SELECT * FROM communication_registry WHERE comunicacion_id=?",child.toString());eval(child,"NO_ADMITIDA");var l=core.antecedente(prof,child);assertThat(l.estado()).isEqualTo("NO_ADMITIDA");assertThat(l.decisionFundamento()).isNotBlank();assertThat(l.decididoPor()).isEqualTo(prof.id());assertThat(l.decididoEn()).isNotNull();assertThat(l.contenido().texto()).isEqualTo("Relato sintético del núcleo");assertThat(d.db.sql.queryForMap("SELECT * FROM communication_registry WHERE comunicacion_id=?",child.toString())).isEqualTo(before);assertThat(core.caso(head,c1).estado()).isEqualTo("EN_GESTION");}
 @Test void admisionAbiertoReferenciaUnaVezSinCrearCasoNiCambiarRevisionDeTermino(){
  core.revisarCaso(prof,UUID.randomUUID(),c1,new Decision(rev(c1),"Solicitud previa"),"SOLICITUD_TERMINO");UUID child=link(c1);var l=core.antecedente(prof,child);var body=new EvaluarAntecedente(l.revision(),rev(c1),"ADMITIDA","Pertinente");UUID key=UUID.randomUUID();var result=core.evaluarAntecedente(prof,key,child,body);assertThat(core.evaluarAntecedente(prof,key,child,body)).isEqualTo(result);assertThat(count("core_case")).isEqualTo(2);assertThat(core.caso(head,c1).estado()).isEqualTo("REVISION_TERMINO");fail(()->pending(child),409);fail(()->core.evaluarAntecedente(prof,key,child,new EvaluarAntecedente(body.revision(),body.revisionCaso(),"NO_ADMITIDA","Distinto")),409);
 }
 @Test void rechazoResuelveTareaSinCambiarAdmisionYCicloIdenticoBloqueado(){closeCase(c1,prof);UUID child=link(c1);eval(child,"ADMITIDA");UUID r=pending(child);assertThat(core.caso(prof,c1).estado()).isEqualTo("CERRADO");fail(()->core.decidirReapertura(prof,UUID.randomUUID(),r,decision(r),true),404);fail(()->core.decidirReapertura(dist,UUID.randomUUID(),r,decision(r),true),404);fail(()->core.reapertura(foreign,r),404);core.decidirReapertura(head,UUID.randomUUID(),r,decision(r),false);assertThat(core.caso(prof,c1).estado()).isEqualTo("CERRADO");assertThat(core.antecedente(prof,child).estado()).isEqualTo("ADMITIDA");assertThat(core.reaperturas(head,null,true,0,20).total()).isZero();assertThat(core.avisos(prof)).isNotEmpty();fail(()->pending(child),409);}
 @Test void aprobarMismoCasoConCierreIntactoDestinoAfectadoReanudadoYAgrupadorActivo(){
  closeCase(c1,prof);closeCase(c2,other);var dd=core.detalle(head,root).destinos().getFirst();core.resolverDestino(head,UUID.randomUUID(),dd.id(),new Decision(dd.revision(),"Evaluación terminada provisional"),false);
  var before=d.db.sql.queryForMap("SELECT closed_ms,closed_by,closure_mode,created_ms FROM core_case WHERE id=?",c1.toString());UUID child=link(c1);eval(child,"ADMITIDA");UUID r=pending(child);var body=decision(r);UUID key=UUID.randomUUID();var result=core.decidirReapertura(head,key,r,body,true);assertThat(core.decidirReapertura(head,key,r,body,true)).isEqualTo(result);
  assertThat(core.caso(prof,c1).estado()).isEqualTo("EN_GESTION");assertThat(core.caso(head,c2).estado()).isEqualTo("CERRADO");assertThat(count("core_case")).isEqualTo(2);assertThat(count("core_requirement")).isEqualTo(1);assertThat(core.detalle(head,root).comunicacion().estadoRequerimiento()).isEqualTo("ACTIVO");assertThat(core.detalle(head,root).destinos().getFirst().estado()).isEqualTo("EVALUANDO");assertThat(d.db.sql.queryForMap("SELECT closed_ms,closed_by,closure_mode,created_ms FROM core_case WHERE id=?",c1.toString())).isEqualTo(before);assertThat(core.periodos(prof,c1)).hasSize(1);assertThat(core.caso(prof,c1).revisiones()).hasSize(1);
  assertThat(d.db.sql.queryForObject("SELECT COUNT(*) FROM core_event WHERE resource_kind='REAPERTURA' AND resource_id=? AND code='AUTORIZACION_REANUDA_DESTINO'",Long.class,r.toString())).isEqualTo(1);
 }
 @Test void nuevosAntecedentesObsoletanSolicitudYSustitucionEsExplicita(){closeCase(c1,prof);UUID one=link(c1);eval(one,"ADMITIDA");UUID r=pending(one);var old=decision(r);UUID two=link(c1);eval(two,"ADMITIDA");fail(()->core.decidirReapertura(head,UUID.randomUUID(),r,old,true),409);fail(()->core.decidirReapertura(head,UUID.randomUUID(),r,decision(r),true),409);fail(()->pending(two),409);UUID next=core.solicitarReapertura(prof,UUID.randomUUID(),c1,new SolicitarReapertura(rev(c1),List.of(one,two),"Revisar conjunto actualizado",r,0L)).id();assertThat(core.reapertura(head,r).estado()).isEqualTo("SUPERADA");assertThat(core.reapertura(head,next).antecedentes()).containsExactlyInAnyOrder(one,two);core.decidirReapertura(head,UUID.randomUUID(),next,decision(next),true);}
 @Test void reasignacionRevocaLecturaYDecisionAnteriorSinPrivilegioHistorico(){closeCase(c1,prof);UUID child=link(c1);eval(child,"ADMITIDA");UUID r=pending(child);core.asignar(head,UUID.randomUUID(),c1,new Asignacion(rev(c1),other.id(),null));fail(()->core.antecedente(prof,child),404);fail(()->core.reapertura(prof,r),404);fail(()->core.decidirReapertura(head,UUID.randomUUID(),r,decision(r),true),409);core.solicitarReapertura(other,UUID.randomUUID(),c1,new SolicitarReapertura(rev(c1),List.of(child),"Asignación vigente",r,0L));}
 @Test void requerimientoSoloNoSeleccionaEvaluadorNiExponeRelatoAOtrosProfesionales(){UUID child=link(null);var l=core.antecedente(dist,child);assertThat(l.evaluador()).isNull();assertThat(l.estado()).isEqualTo("PENDIENTE_ASIGNACION");assertThat(l.requerimientoId()).isEqualTo(core.caso(head,c1).requerimientoId());fail(()->core.antecedente(prof,child),404);assertThat(core.antecedentes(prof,null,true,0,20).total()).isZero();assertThat(core.bandeja(dist).sinEncaminar()).isEqualTo(1);}
 @Test void vinculoHistoricoNoEsAdmitidoPorDefecto(){UUID child=link(c1);d.db.sql.update("UPDATE core_link SET requirement_reference=NULL WHERE child_id=?",child.toString());assertThat(core.antecedente(prof,child).estado()).isEqualTo("SIN_EVALUACION_HISTORICA");}
 @Test void soloCompartirAgrupadorNoDifundeRelatoDeHijaSinEncaminarAJefaturas(){
  var target=d.actor("demo.jefatura.upd.a");core.distribuir(dist,UUID.randomUUID(),root,new Distribucion(core.detalle(dist,root).comunicacion().revision(),List.of(new NuevoDestino("UPD","DEMO_SEDE_A",target.id()))));UUID child=link(null);
  assertThat(core.antecedente(head,child).contenido()).isNull();assertThat(core.antecedente(target,child).contenido()).isNull();assertThat(core.antecedente(dist,child).contenido()).isNotNull();fail(()->core.detalle(target,child),404);fail(()->core.detalle(head,child),404);
 }
 @Test void responsableQueYaNoPerteneceAlAlcanceNoRecobraPermisosPorReferenciaHistorica(){UUID child=link(c1);var outside=d.actor("demo.profesional.uep.b");d.db.sql.update("UPDATE core_case SET responsible_id=? WHERE id=?",outside.id(),c1.toString());fail(()->core.antecedente(outside,child),404);fail(()->core.caso(outside,c1),404);fail(()->core.detalle(outside,root),404);assertThat(core.buzon(outside,0,20)).isEmpty();assertThat(core.antecedentes(outside,null,true,0,20).total()).isZero();}
 @Test void derivacionAceptadaNoSeDeshacePorReaperturaOrdinaria(){var t=core.solicitarDerivacion(head,UUID.randomUUID(),c1,new SolicitarDerivacion(rev(c1),"UPD","DEMO_SEDE_A","Motivo","Antecedente"));var target=d.actor("demo.jefatura.upd.a");var v=core.derivacion(target,t.id());core.aceptarDerivacion(target,UUID.randomUUID(),v.id(),new AceptarDerivacion(v.revision(),v.revisionOrigen(),null,null,"Aceptación"));UUID child=link(c1);eval(child,"ADMITIDA");fail(()->pending(child),409);assertThat(core.caso(prof,c1).modalidadCierre()).isEqualTo("DERIVACION");}
 @Test void rollbackPosteriorAAutorizacionRevierteTodoYReintentoRecupera(){
  closeCase(c1,prof);UUID child=link(c1);eval(child,"ADMITIDA");UUID r=pending(child);var body=decision(r);UUID key=UUID.randomUUID();d.ddl(sql->sql.execute("ALTER TABLE core_event ADD CONSTRAINT reject_reopen CHECK(code<>'REAPERTURA_AUTORIZADA_NUEVO_PERIODO')"));
  try{assertThatThrownBy(()->core.decidirReapertura(head,key,r,body,true)).isInstanceOf(org.springframework.dao.DataAccessException.class);assertThat(core.reapertura(head,r).estado()).isEqualTo("PENDIENTE");assertThat(core.caso(prof,c1).estado()).isEqualTo("CERRADO");assertThat(count("core_management_period")).isZero();}
  finally{d.ddl(sql->sql.execute("ALTER TABLE core_event DROP "+(System.getenv("ROBUST_DB_URL")==null?"CONSTRAINT":"CHECK")+" reject_reopen"));}core.decidirReapertura(head,key,r,body,true);
 }
 @Test void dosDecisionesConcurrentesUnSoloResultadoYPeriodo()throws Exception{
  closeCase(c1,prof);UUID child=link(c1);eval(child,"ADMITIDA");UUID r=pending(child);var body=decision(r);var start=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(2)){Callable<Integer> one=()->{start.await();try{core.decidirReapertura(head,UUID.randomUUID(),r,body,true);return 200;}catch(FalloTecnico e){return e.status;}};Callable<Integer> two=()->{start.await();try{core.decidirReapertura(head,UUID.randomUUID(),r,body,false);return 200;}catch(FalloTecnico e){return e.status;}};var f1=pool.submit(one);var f2=pool.submit(two);start.countDown();assertThat(List.of(f1.get(10,TimeUnit.SECONDS),f2.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);}
  assertThat(core.reapertura(head,r).estado()).isIn("APROBADA","RECHAZADA");assertThat(count("core_management_period")).isLessThanOrEqualTo(1);
 }
 @Test void conteosYBusquedaSonDurablesPaginadosYPorAlcance(){link(c1);assertThat(core.buscar(dist,"COM-",true,0,1).total()).isEqualTo(1);assertThat(core.buscar(dist,"COM-",false,0,1).total()).isEqualTo(2);assertThat(core.buscar(foreign,"",false,0,20).total()).isZero();assertThat(core.casos(prof,"",0,1).items()).hasSize(1);assertThat(core.casos(other,"",0,1).total()).isEqualTo(1);assertThat(core.bandeja(prof).antecedentesPendientes()).isEqualTo(1);assertThat(core.bandeja(other).antecedentesPendientes()).isZero();}
 @Test void repetirDecisionConcurrenteMismaClaveRecuperaAutorizacionUnica()throws Exception{
  closeCase(c1,prof);UUID child=link(c1);eval(child,"ADMITIDA");UUID r=pending(child);var body=decision(r);UUID key=UUID.randomUUID();var start=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(2)){Callable<Resultado> work=()->{start.await();return core.decidirReapertura(head,key,r,body,true);};var a=pool.submit(work);var b=pool.submit(work);start.countDown();assertThat(a.get(10,TimeUnit.SECONDS)).isEqualTo(b.get(10,TimeUnit.SECONDS));}assertThat(count("core_management_period")).isEqualTo(1);
 }
 @Test void destinoHermanoResueltoNoSeReanudaYDuplicacionPendienteSeRechaza(){
  var target=d.actor("demo.jefatura.upd.a");var comm=core.detalle(dist,root).comunicacion();core.distribuir(dist,UUID.randomUUID(),root,new Distribucion(comm.revision(),List.of(new NuevoDestino("UPD","DEMO_SEDE_A",target.id()))));var dd=core.detalle(target,root).destinos().getFirst();core.resolverDestino(target,UUID.randomUUID(),dd.id(),new Decision(dd.revision(),"Respuesta sin Caso"),true);
  closeCase(c1,prof);UUID child=link(c1);eval(child,"ADMITIDA");UUID r=pending(child);fail(()->pending(child),409);core.decidirReapertura(head,UUID.randomUUID(),r,decision(r),true);assertThat(core.detalle(target,root).destinos().getFirst().estado()).isEqualTo("RESUELTO");assertThat(core.caso(head,c2).estado()).isEqualTo("EN_GESTION");
 }
 @Test void foliobusquedaNoTruncaNumerosDeMasDeUnDigitoYPaginaSinDescargarLaBase(){
  for(int i=0;i<22;i++)d.nuevaWeb();var all=core.buscar(dist,"",false,0,20);assertThat(all.total()).isEqualTo(23);assertThat(all.items()).hasSize(20);assertThat(core.buscar(dist,"",false,1,20).items()).hasSize(3);var last=all.items().getFirst();assertThat(core.buscar(dist,last.folio(),false,0,20).items()).extracting(Resumen::id).containsExactly(last.id());
 }
}
