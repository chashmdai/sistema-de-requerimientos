package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.nucleo.ContenidoInstitucional.*;
import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.*;import java.time.*;import java.util.*;import java.util.concurrent.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import static org.junit.jupiter.api.Assertions.*;
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContenidoInstitucionalTest {
 @TempDir static Path temp;ContenidoFixture x;AreasFixture f;final AreasInstitucionalesTest.Reloj clock=new AreasInstitucionalesTest.Reloj();
 @BeforeAll void setup()throws Exception{x=new ContenidoFixture(temp,clock,null);f=x.f;}
 @AfterAll void close()throws Exception{if(x!=null)x.close();}
 void denied(int status,org.junit.jupiter.api.function.Executable e){assertEquals(status,assertThrows(FalloTecnico.class,e).status);}
 @Test void principalNoSeInfiereNiTISeConvierteEnJefatura()throws Exception{
  UUID a=x.m.caso(),req=f.core.caso(f.a(f.headA),a).requerimientoId();denied(409,()->x.c().contextoReq(f.a(f.headA),req));
  x.acreditar(req,"ACTIVA",f.end);assertNotNull(x.c().contextoReq(f.a(f.headA),req));denied(404,()->x.c().contextoReq(f.a(f.headB),req));denied(404,()->x.c().contextoReq(f.a(f.ti),req));
  x.acreditar(req,"SUSPENDIDA",f.end);denied(409,()->x.c().contextoReq(f.a(f.headA),req));
 }
 @Test void reqMixtoNoEntregaHijaPorCompartirRequerimientoOMateria()throws Exception{
  var p=x.par();var body=new Pertinencia(0,List.of(p[1]),true,"Solo Caso y materia UPD");
  var key=UUID.randomUUID();var r=x.c().determinar(f.a(f.headA),key,p[2],p[3],body);assertEquals(r,x.c().determinar(f.a(f.headA),key,p[2],p[3],body));
  assertEquals(0,x.c().antecedentes(f.a(x.m.upr),p[0]).size());assertEquals(1,x.c().antecedentes(f.a(x.m.upd),p[1]).size());
  denied(404,()->f.core.antecedente(f.a(f.headA),p[3]));assertEquals(0,f.core.antecedentes(f.a(f.headA),null,false,0,20).items().stream().filter(a->a.hija().equals(p[3])).count());
  assertEquals(0,x.d.db.sql.queryForObject("SELECT COUNT(*) FROM case_evidence_assessment WHERE child_id=?",Long.class,p[3].toString()));
  denied(404,()->x.c().determinar(f.a(f.headB),UUID.randomUUID(),p[2],p[3],new Pertinencia(1,List.of(p[0]),true,"Jefatura no principal")));
  x.c().determinar(f.a(f.headA),UUID.randomUUID(),p[2],p[3],new Pertinencia(1,List.of(p[0]),false,"Rectificación solo metadatos"));
  assertEquals(0,x.c().antecedentes(f.a(x.m.upd),p[1]).size());assertNull(x.c().antecedentes(f.a(x.m.upr),p[0]).getFirst().get("contenido"));
  assertEquals(2,x.d.db.sql.queryForObject("SELECT COUNT(*) FROM communication_pertinence_history WHERE child_id=?",Long.class,p[3].toString()));
 }
 @Test void valoracionPorCasoEsSucesivaYNoReabreElCerrado()throws Exception{
  var p=x.par();x.c().determinar(f.a(f.headA),UUID.randomUUID(),p[2],p[3],new Pertinencia(0,List.of(p[0]),true,"Evaluación profesional contextual"));
  var c=f.core.caso(f.a(f.profA),p[0]);f.core.revisarCaso(f.a(f.profA),UUID.randomUUID(),p[0],new ContratosNucleo.Decision(c.revision(),"Solicitud de término"),"SOLICITUD_TERMINO");
  f.core.revisarCaso(f.a(f.headA),UUID.randomUUID(),p[0],new ContratosNucleo.Decision(c.revision()+1,"Cierre independiente"),"CIERRE");
  var first=x.c().valorar(f.a(f.profA),UUID.randomUUID(),p[0],p[3],new Valoracion(1,0,"NO_ADMITIDA","Evaluación inicial"));
  x.c().valorar(f.a(f.profA),UUID.randomUUID(),p[0],p[3],new Valoracion(1,first.revision(),"ADMITIDA","Rectificación fundada que conserva anterior"));
  assertEquals("CERRADO",f.core.caso(f.a(f.headA),p[0]).estado());assertEquals(2,((List<?>)x.c().antecedentes(f.a(f.profA),p[0]).getFirst().get("valoraciones")).size());
  denied(404,()->x.c().valorar(f.a(x.m.upr),UUID.randomUUID(),p[0],p[3],new Valoracion(1,0,"ADMITIDA","Consulta no es gestión")));
 }
 @Test void documentoExigeDecisionExplicitaYSeparaMetadatosContenidoDescarga(){
  UUID c=x.m.caso();x.m.clasificar(c,0,"MAT_UPR",true);UUID doc=x.cargar(c).id();assertTrue(x.docs.listar(f.a(x.m.upr),c).isEmpty());
  x.ordinario(c,doc,0);assertTrue(x.docs.listar(f.a(x.m.upr),c).isEmpty());
  x.c().publicar(f.a(f.headA),UUID.randomUUID(),c,doc,c,x.publicacion(0,false,false,"ACTIVA"));
  var d=x.docs.listar(f.a(x.m.upr),c).getFirst();assertFalse(d.consultable());assertFalse(d.descargable());
  denied(404,()->x.docs.referencia(f.a(x.m.upr),c,doc));denied(404,()->x.docs.historial(f.a(x.m.upr),c,doc));
  x.c().publicar(f.a(f.headA),UUID.randomUUID(),c,doc,c,x.publicacion(1,true,false,"ACTIVA"));
  assertNotNull(x.docs.referencia(f.a(x.m.upr),c,doc));denied(404,()->x.docs.referencia(f.a(x.m.upr),c,doc,"DESCARGA"));
  denied(404,()->x.c().proteger(f.a(x.m.upr),UUID.randomUUID(),c,doc,new Proteccion(1,"ORDINARIO","Rol consultor")));
 }
 @Test void firNoSeHeredaPorReferenciaOtraAreaNiDireccion()throws Exception{
  var p=x.par();UUID doc=x.cargar(p[0]).id();x.docs.vincular(f.a(f.headA),UUID.randomUUID(),doc,p[0],p[1],null);
  x.c().proteger(f.a(f.headA),UUID.randomUUID(),p[0],doc,new Proteccion(0,"FIR","Ficha interna reservada de su ámbito"));
  assertNotNull(x.docs.referencia(f.a(f.profA),p[0],doc));assertNotNull(x.docs.referencia(f.a(f.headA),p[0],doc));
  assertTrue(x.docs.listar(f.a(f.profB),p[1]).isEmpty());assertTrue(x.docs.listar(f.a(f.headB),p[1]).isEmpty());assertTrue(x.docs.listar(f.a(x.m.upr),p[0]).isEmpty());
  denied(404,()->x.docs.referencia(f.a(f.ti),p[0],doc));denied(404,()->x.docs.referencia(f.a(f.headB),p[1],doc,"METADATOS"));
  denied(409,()->x.c().publicar(f.a(f.headA),UUID.randomUUID(),p[0],doc,p[0],x.publicacion(0,true,true,"ACTIVA")));
 }
 @Test void proteccionRetiradaNoRevivePublicacionAnterior(){
  UUID c=x.m.caso();x.m.clasificar(c,0,"MAT_UPR",true);UUID doc=x.cargar(c).id();x.ordinario(c,doc,0);
  x.c().publicar(f.a(f.headA),UUID.randomUUID(),c,doc,c,x.publicacion(0,true,true,"ACTIVA"));assertNotNull(x.docs.referencia(f.a(x.m.upr),c,doc));
  x.c().proteger(f.a(f.headA),UUID.randomUUID(),c,doc,new Proteccion(1,"BLOQUEADO","Restricción fundada de consulta"));denied(404,()->x.docs.referencia(f.a(f.profA),c,doc));
  x.ordinario(c,doc,2);assertTrue(x.docs.listar(f.a(x.m.upr),c).isEmpty());assertNotNull(x.docs.referencia(f.a(f.profA),c,doc));
 }
 @Test void cambioDeMateriaOAreaInvalidaLaPublicacion(){
  UUID c=x.m.caso();x.m.clasificar(c,0,"MAT_UPR",true);UUID doc=x.cargar(c).id();x.ordinario(c,doc,0);x.c().publicar(f.a(f.headA),UUID.randomUUID(),c,doc,c,x.publicacion(0,true,true,"ACTIVA"));
  x.m.clasificar(c,1,"MAT_UPD",true);assertTrue(x.docs.listar(f.a(x.m.upd),c).isEmpty());x.m.clasificar(c,2,"MAT_UPR",true);assertTrue(x.docs.listar(f.a(x.m.upr),c).isEmpty());
  x.c().publicar(f.a(f.headA),UUID.randomUUID(),c,doc,c,x.publicacion(1,true,true,"ACTIVA"));f.core.cambiarArea(f.a(f.headA),UUID.randomUUID(),c,new ContratosNucleo.CambiarArea(3,"AREA_B","Cambio autorizado","AREA_A"));x.m.clasificar(c,4,"MAT_UPR",true);assertTrue(x.docs.listar(f.a(x.m.upr),c).isEmpty());
 }
 @Test void auditoriaAtomicaYCarreraDeProteccion()throws Exception{
  UUID c=x.m.caso(),doc=x.cargar(c).id();x.d.ddl(sql->sql.execute("ALTER TABLE document_content_history ADD CONSTRAINT test_historia CHECK(action_code<>'PROTECCION')"));
  try{assertThrows(org.springframework.dao.DataAccessException.class,()->x.ordinario(c,doc,0));}finally{x.d.ddl(sql->sql.execute("ALTER TABLE document_content_history DROP CONSTRAINT test_historia"));}
  assertEquals("SIN_CLASIFICAR",x.c().control(f.a(f.headA),c,doc).get("clasificacion"));
  var gate=new CountDownLatch(1);try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(()->race(gate,c,doc));var b=pool.submit(()->race(gate,c,doc));gate.countDown();var rs=new ArrayList<>(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS)));Collections.sort(rs);assertEquals(List.of(200,409),rs);}
 }
 int race(CountDownLatch gate,UUID c,UUID doc)throws Exception{gate.await();try{x.ordinario(c,doc,0);return 200;}catch(FalloTecnico e){return e.status;}}
 @Test void pertinenciaRevierteSiFallaSuHistoria()throws Exception{
  var p=x.par();UUID key=UUID.randomUUID();x.d.ddl(sql->sql.execute("ALTER TABLE communication_pertinence_history ADD CONSTRAINT test_pertinencia CHECK(reason_text<>'FORZAR_ROLLBACK')"));
  try{assertThrows(org.springframework.dao.DataAccessException.class,()->x.c().determinar(f.a(f.headA),key,p[2],p[3],new Pertinencia(0,List.of(p[0]),true,"FORZAR_ROLLBACK")));}finally{x.d.ddl(sql->sql.execute("ALTER TABLE communication_pertinence_history DROP CONSTRAINT test_pertinencia"));}
  assertEquals(0,x.d.db.sql.queryForObject("SELECT COUNT(*) FROM communication_pertinence WHERE child_id=?",Long.class,p[3].toString()));assertEquals(0,x.d.db.sql.queryForObject("SELECT COUNT(*) FROM communication_pertinence_target WHERE child_id=?",Long.class,p[3].toString()));assertEquals(0,x.d.db.sql.queryForObject("SELECT COUNT(*) FROM core_command WHERE id=?",Long.class,key.toString()));
 }
 @Test void valoracionesConcurrentesConservanUnSoloPredecesor()throws Exception{
  var p=x.par();x.c().determinar(f.a(f.headA),UUID.randomUUID(),p[2],p[3],new Pertinencia(0,List.of(p[0]),true,"Evaluación concurrente"));var gate=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(2)){java.util.concurrent.Callable<Integer> attempt=()->{gate.await();try{x.c().valorar(f.a(f.profA),UUID.randomUUID(),p[0],p[3],new Valoracion(1,0,"ADMITIDA","Valoración concurrente"));return 200;}catch(FalloTecnico e){return e.status;}};var a=pool.submit(attempt);var b=pool.submit(attempt);gate.countDown();var rs=new ArrayList<>(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS)));Collections.sort(rs);assertEquals(List.of(200,409),rs);}
  assertEquals(1,x.d.db.sql.queryForObject("SELECT COUNT(*) FROM case_evidence_assessment WHERE child_id=?",Long.class,p[3].toString()));
 }
 @Test void revocacionDuranteStreamingDetieneLosBloquesSiguientes()throws Exception{
  UUID c=x.m.caso();x.m.clasificar(c,0,"MAT_UPR",true);byte[] bytes=new byte[150000];Arrays.fill(bytes,(byte)'A');UUID doc=x.docs.cargar(f.a(f.profA),UUID.randomUUID(),c,null,"largo.txt",new java.io.ByteArrayInputStream(bytes)).id();x.ordinario(c,doc,0);x.c().publicar(f.a(f.headA),UUID.randomUUID(),c,doc,c,x.publicacion(0,true,true,"ACTIVA"));
  var ref=x.docs.referencia(f.a(x.m.upr),c,doc,"DESCARGA");var d=ref.original();var response=cl.nexbyte.siger.institucional.documental.TransferenciaDocumento.responder(x.docs.verificarArchivo(ref),d.tamanio(),d.sha256(),d.nombre(),d.tipo(),false,new org.springframework.mock.web.MockHttpServletRequest("GET","/original"),mode->{},()->x.docs.revalidar(f.a(x.m.upr),c,doc,"DESCARGA"));
  var out=new java.io.ByteArrayOutputStream(){boolean first=true;@Override public synchronized void write(byte[] b,int off,int len){super.write(b,off,len);if(first){first=false;x.c().proteger(f.a(f.headA),UUID.randomUUID(),c,doc,new Proteccion(1,"FIR","Reserva durante descarga"));}}};
  denied(404,()->response.getBody().writeTo(out));assertEquals(65536,out.size());
  assertEquals(1,x.d.db.sql.queryForObject("SELECT COUNT(*) FROM matter_access_event WHERE actor_id=? AND resource_id=? AND action_code='DOCUMENTO_DESCARGA'",Long.class,x.m.upr,doc.toString()));
  assertEquals(1,x.d.db.sql.queryForObject("SELECT COUNT(*) FROM matter_access_event WHERE actor_id=? AND resource_id=? AND action_code='DOCUMENTO_REVOCADO'",Long.class,x.m.upr,doc.toString()));
 }
 @Test void avisosDePertinenciaNoEntreganComDeOtraMateria()throws Exception{
  var p=x.par();x.c().determinar(f.a(f.headA),UUID.randomUUID(),p[2],p[3],new Pertinencia(0,List.of(p[1]),true,"Materia UPD únicamente"));
  var recursos=new cl.nexbyte.siger.institucional.notificaciones.RecursosNotificacion(x.d.db,f.cat,f.core);var notices=new cl.nexbyte.siger.institucional.notificaciones.NotificacionesInstitucionales(x.d.db,f.cat,recursos,clock);
  for(int i=0;i<50;i++)notices.proyectar();
  assertTrue(notices.listar(f.a(x.m.upd),0,50).items().stream().anyMatch(n->n.tipo().equals("PERTINENCIA_ACTUALIZADA")&&n.ruta().equals("caso/"+p[1])));
  assertFalse(notices.listar(f.a(x.m.upr),0,50).items().stream().anyMatch(n->n.tipo().equals("PERTINENCIA_ACTUALIZADA")&&n.ruta().equals("caso/"+p[1])));
  assertFalse(cl.nexbyte.siger.institucional.notificaciones.NotificacionesInstitucionales.CORREO.contains("PERTINENCIA_ACTUALIZADA"));
  x.m.clasificar(p[1],2,"MAT_UPR",true);assertFalse(notices.listar(f.a(x.m.upd),0,50).items().stream().anyMatch(n->n.ruta().equals("caso/"+p[1])));
 }
 @Test void responsableRestringePeroNoPublicaNiLevantaUnaFir(){
  UUID c=x.m.caso(),doc=x.cargar(c).id();assertTrue(x.docs.documento(f.a(f.profA),c,doc).restringible());
  x.c().proteger(f.a(f.profA),UUID.randomUUID(),c,doc,new Proteccion(0,"BLOQUEADO","Responsable restringe asociación a revisar"));denied(404,()->x.docs.referencia(f.a(f.profA),c,doc));
  denied(404,()->x.c().proteger(f.a(f.profA),UUID.randomUUID(),c,doc,new Proteccion(1,"ORDINARIO","No puede ampliar su permiso")));
  denied(404,()->x.c().publicar(f.a(f.profA),UUID.randomUUID(),c,doc,c,x.publicacion(0,true,true,"ACTIVA")));
  assertTrue(((List<?>)x.c().control(f.a(f.profA),c,doc).get("publicaciones")).isEmpty());x.ordinario(c,doc,1);assertNotNull(x.docs.referencia(f.a(f.profA),c,doc));
 }
 @Test void expiracionYSuspensionNoNecesitanReabrirLaMateria()throws Exception{
  UUID c=x.m.caso();x.m.clasificar(c,0,"MAT_UPR",true);UUID doc=x.cargar(c).id();x.ordinario(c,doc,0);x.c().publicar(f.a(f.headA),UUID.randomUUID(),c,doc,c,x.publicacion(0,true,true,"ACTIVA"));
  var before=clock.now;try{clock.now=clock.now.plusSeconds(1201);assertTrue(x.docs.listar(f.a(x.m.upr),c).isEmpty());}finally{clock.now=before;}
  x.m.publicar("SUSPENDIDA",f.end);try{x.c().publicar(f.a(f.headA),UUID.randomUUID(),c,doc,c,x.publicacion(1,false,false,"SUSPENDIDA"));}finally{x.m.publicar("ACTIVA",f.end);}
  assertTrue(x.docs.listar(f.a(x.m.upr),c).isEmpty());
 }
 @Test void streamingRevalidaAntesDelPrimerByte()throws Exception{
  UUID c=x.m.caso();x.m.clasificar(c,0,"MAT_UPR",true);UUID doc=x.cargar(c).id();x.ordinario(c,doc,0);x.c().publicar(f.a(f.headA),UUID.randomUUID(),c,doc,c,x.publicacion(0,true,true,"ACTIVA"));
  var ref=x.docs.referencia(f.a(x.m.upr),c,doc,"DESCARGA");var d=ref.original();var req=new org.springframework.mock.web.MockHttpServletRequest("GET","/original");req.addHeader("Range","bytes=0-3");
  var response=cl.nexbyte.siger.institucional.documental.TransferenciaDocumento.responder(x.docs.verificarArchivo(ref),d.tamanio(),d.sha256(),d.nombre(),d.tipo(),false,req,mode->{},()->x.docs.revalidar(f.a(x.m.upr),c,doc,"DESCARGA"));
  x.c().proteger(f.a(f.headA),UUID.randomUUID(),c,doc,new Proteccion(1,"FIR","Reserva durante preparación de respuesta"));var out=new java.io.ByteArrayOutputStream();denied(404,()->response.getBody().writeTo(out));assertEquals(0,out.size());
 }
 @Test void acreditacionNoSustituyeAceptacionDeTransferencia()throws Exception{
  UUID c=x.m.caso(),req=f.core.caso(f.a(f.headA),c).requerimientoId();x.acreditar(req,"ACTIVA",f.end);long rev=x.d.db.sql.queryForObject("SELECT revision FROM authority_registry WHERE id=1",Long.class);
  var list=new ArrayList<>(x.principales.values());list.removeIf(p->p.requerimiento().equals(req));list.add(new cl.nexbyte.siger.institucional.identidad.AutoridadesInstitucionales.PrincipalReq(req,"UPR","DEMO_SEDE_A","AREA_B",f.start,f.end,"ACTIVA","Cambio sin aceptación","b".repeat(64)));
  var b=x.base;denied(409,()->f.importar(new cl.nexbyte.siger.institucional.identidad.AutoridadesInstitucionales.Registro13("1.3",rev+1,clock.instant(),b.expira(),b.vinculos(),b.mandatos(),b.areas(),b.ambitos(),b.cambiosArea(),b.materias(),b.sedesRegionales(),b.ambitosMateria(),list)));
  assertEquals(rev,x.d.db.sql.queryForObject("SELECT revision FROM authority_registry WHERE id=1",Long.class));assertEquals("UPR",x.c().contextoReq(f.a(f.headA),req).get("unidad"));denied(404,()->x.c().contextoReq(f.a(f.headB),req));
 }
}

