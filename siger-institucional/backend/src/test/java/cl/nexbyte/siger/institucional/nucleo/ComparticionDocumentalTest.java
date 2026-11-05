package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.robustez.*;import cl.nexbyte.siger.institucional.nucleo.ContenidoInstitucional.*;
import java.nio.file.*;import java.time.*;import java.util.*;import java.util.concurrent.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import static org.junit.jupiter.api.Assertions.*;
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComparticionDocumentalTest {
 @TempDir static Path temp;ContenidoFixture x;AreasFixture f;
 @BeforeAll void open()throws Exception{x=new ContenidoFixture(temp,Clock.systemUTC(),null);f=x.f;}
 @AfterAll void close()throws Exception{x.close();}
 void denied(int code,org.junit.jupiter.api.function.Executable action){assertEquals(code,assertThrows(FalloTecnico.class,action).status);}
 @Test void referenciaNoConcedeConsultaYPermisoDeCasoNoConcedeLecturaPorMateria()throws Exception{
  var p=x.par();var doc=x.cargar(p[0]);var head=f.a(f.headA);x.docs.vincular(head,UUID.randomUUID(),doc.id(),p[0],p[1],null);
  assertTrue(x.docs.listar(f.a(f.profB),p[1]).isEmpty());x.ordinario(p[0],doc.id(),0);
  var key=UUID.randomUUID();var body=x.publicacion(0,true,false,"ACTIVA");var r=x.c().compartir(head,key,p[0],doc.id(),p[1],body);assertEquals(r,x.c().compartir(head,key,p[0],doc.id(),p[1],body));
  assertNotNull(x.docs.referencia(f.a(f.profB),p[1],doc.id()));denied(404,()->x.docs.referencia(f.a(f.profB),p[1],doc.id(),"DESCARGA"));
  assertFalse(x.docs.documento(f.a(f.profB),p[1],doc.id()).referenciable());assertTrue(x.docs.listar(f.a(x.m.upd),p[1]).isEmpty());
  denied(404,()->x.c().compartir(f.a(f.headB),UUID.randomUUID(),p[0],doc.id(),p[1],x.publicacion(1,true,true,"ACTIVA")));
  x.c().compartir(head,UUID.randomUUID(),p[0],doc.id(),p[1],x.publicacion(1,false,false,"SUSPENDIDA"));assertTrue(x.docs.listar(f.a(f.profB),p[1]).isEmpty());
 }
 @Test void multirrolNoReenviaConsultaDeMateriaHaciaSuCasoGestionable()throws Exception{
  var p=x.par();UUID doc=x.cargar(p[0]).id();x.ordinario(p[0],doc,0);x.c().publicar(f.a(f.headA),UUID.randomUUID(),p[0],doc,p[0],x.publicacion(0,true,true,"ACTIVA"));
  f.otorgar(x.m.upr,"COORDINADOR","AREA_B");assertNotNull(x.docs.referencia(f.a(x.m.upr),p[0],doc));
  denied(404,()->x.docs.vincular(f.a(x.m.upr),UUID.randomUUID(),doc,p[0],p[1],null));
  assertEquals(0,x.d.db.sql.queryForObject("SELECT COUNT(*) FROM management_document_link WHERE document_id=? AND case_id=?",Long.class,doc.toString(),p[1].toString()));
  assertFalse(x.docs.documento(f.a(x.m.upr),p[0],doc).referenciable());
 }
 @Test void cambiosDelPropietarioInvalidanConsultaHaciaOtrosCasos()throws Exception{
  var p=x.par();UUID doc=x.cargar(p[0]).id();x.docs.vincular(f.a(f.headA),UUID.randomUUID(),doc,p[0],p[1],null);x.ordinario(p[0],doc,0);
  x.c().compartir(f.a(f.headA),UUID.randomUUID(),p[0],doc,p[1],x.publicacion(0,true,true,"ACTIVA"));x.c().publicar(f.a(f.headA),UUID.randomUUID(),p[0],doc,p[1],x.publicacion(0,true,true,"ACTIVA"));
  assertNotNull(x.docs.referencia(f.a(f.profB),p[1],doc));assertNotNull(x.docs.referencia(f.a(x.m.upd),p[1],doc));
  x.m.clasificar(p[0],1,"MAT_UPD",true);assertTrue(x.docs.listar(f.a(f.profB),p[1]).isEmpty());assertTrue(x.docs.listar(f.a(x.m.upd),p[1]).isEmpty());
  x.m.clasificar(p[0],2,"MAT_UPR",true);assertTrue(x.docs.listar(f.a(f.profB),p[1]).isEmpty());
  x.c().compartir(f.a(f.headA),UUID.randomUUID(),p[0],doc,p[1],x.publicacion(1,true,true,"ACTIVA"));
  f.core.cambiarArea(f.a(f.headA),UUID.randomUUID(),p[0],new ContratosNucleo.CambiarArea(3,"AREA_B","Cambio de contexto propietario","AREA_A"));assertTrue(x.docs.listar(f.a(f.profB),p[1]).isEmpty());
 }
 @Test void consentimientoExpiradoYFirPermanecenCerrados()throws Exception{
  var p=x.par();UUID doc=x.cargar(p[0]).id();x.docs.vincular(f.a(f.headA),UUID.randomUUID(),doc,p[0],p[1],null);x.ordinario(p[0],doc,0);
  x.c().compartir(f.a(f.headA),UUID.randomUUID(),p[0],doc,p[1],new Publicacion(0,Instant.now().minusSeconds(100),Instant.now().minusSeconds(1),true,true,"ACTIVA","Vigencia ya terminada"));assertTrue(x.docs.listar(f.a(f.profB),p[1]).isEmpty());
  x.c().proteger(f.a(f.headA),UUID.randomUUID(),p[0],doc,new Proteccion(1,"FIR","Reserva no compartible por permiso ordinario"));denied(409,()->x.c().compartir(f.a(f.headA),UUID.randomUUID(),p[0],doc,p[1],x.publicacion(1,true,true,"ACTIVA")));
 }
 @Test void suspensionDeMateriaPropietariaRetiraLecturaIndirecta()throws Exception{
  var p=x.par();UUID doc=x.cargar(p[0]).id();x.docs.vincular(f.a(f.headA),UUID.randomUUID(),doc,p[0],p[1],null);x.ordinario(p[0],doc,0);x.c().compartir(f.a(f.headA),UUID.randomUUID(),p[0],doc,p[1],x.publicacion(0,true,true,"ACTIVA"));
  assertNotNull(x.docs.referencia(f.a(f.profB),p[1],doc));
  try{x.m.publicar("SUSPENDIDA",f.end);assertTrue(x.docs.listar(f.a(f.profB),p[1]).isEmpty());}finally{x.m.publicar("ACTIVA",f.end);}
 }
 @Test void carreraDeConsentimientoYFalloDeHistoriaSonAtomicos()throws Exception{
  var p=x.par();UUID doc=x.cargar(p[0]).id();x.docs.vincular(f.a(f.headA),UUID.randomUUID(),doc,p[0],p[1],null);x.ordinario(p[0],doc,0);
  x.d.ddl(sql->sql.execute("ALTER TABLE document_content_history ADD CONSTRAINT test_consent CHECK(action_code<>'CONSULTA_CASO')"));
  try{assertThrows(org.springframework.dao.DataAccessException.class,()->x.c().compartir(f.a(f.headA),UUID.randomUUID(),p[0],doc,p[1],x.publicacion(0,true,true,"ACTIVA")));}finally{x.d.ddl(sql->sql.execute("ALTER TABLE document_content_history DROP CONSTRAINT test_consent"));}
  assertEquals(0,x.d.db.sql.queryForObject("SELECT COUNT(*) FROM document_case_consent WHERE document_id=?",Long.class,doc.toString()));
  var gate=new CountDownLatch(1);var request=x.publicacion(0,true,true,"ACTIVA");try(var pool=Executors.newFixedThreadPool(2)){Callable<Integer> action=()->{gate.await();try{x.c().compartir(f.a(f.headA),UUID.randomUUID(),p[0],doc,p[1],request);return 200;}catch(FalloTecnico e){return e.status;}};var a=pool.submit(action);var b=pool.submit(action);gate.countDown();var codes=new ArrayList<>(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS)));Collections.sort(codes);assertEquals(List.of(200,409),codes);}
 }
}
