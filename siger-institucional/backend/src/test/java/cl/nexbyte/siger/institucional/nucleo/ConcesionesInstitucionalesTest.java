package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.identidad.*;
import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.Path;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static cl.nexbyte.siger.institucional.identidad.ConcesionesInstitucionales.*;

class ConcesionesInstitucionalesTest {
 @TempDir Path temp;DatosNucleoTest d;IdentidadInstitucional ids;ConcesionFixture.Contexto f;String ti,target;
 final Clock clock=Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"),ZoneOffset.UTC);
 final Instant from=clock.instant().minusSeconds(1),until=clock.instant().plusSeconds(3600);
 @BeforeEach void setup()throws Exception{
  d=new DatosNucleoTest(temp);ids=new IdentidadInstitucional(d.db,clock);ti=ids.bootstrap(new IdentidadInstitucional.Crear("ti.authority","TI sintético","ti.authority@example.invalid","TI",null,null,UUID.randomUUID()+"AZ")).id();target=user("beneficiario");f=ConcesionFixture.contexto(d.db,ids,ti,clock);f.sincronizar(ti,grant());
 }
 @AfterEach void close(){d.close();}
 String user(String name){String clave=UUID.randomUUID()+"AZ";var c=ids.crear(ti,new IdentidadInstitucional.Crear(name,"Persona sintética "+name,name+"@example.invalid","Cargo no concede facultad",null,null,clave));ids.cambiarClavePersonal((IdentidadInstitucional.Principal)ids.autenticar(name),new IdentidadInstitucional.CambioClave(clave,UUID.randomUUID()+"AZ"));return c.id();}
 IdentidadInstitucional.Otorgar grant(){return new IdentidadInstitucional.Otorgar("PROFESIONAL","UNIDAD_SEDE","UPR","DEMO_SEDE_A",from,until);}
 Solicitud request(String beneficiary){return new Solicitud(beneficiary,"PROFESIONAL","UNIDAD_SEDE","UPR","DEMO_SEDE_A",from,until,"Solicitud sintética respaldada");}
 UUID pending(){return f.concesiones.solicitar(ti,UUID.randomUUID(),request(target)).id();}
 UUID approved(){var id=pending();f.concesiones.decidir(f.revisor,UUID.randomUUID(),id,new Decision(0,"Aprobación sintética"),true);return id;}
 long roles(){return d.db.sql.queryForObject("SELECT COUNT(*) FROM staff_assignment WHERE actor_id=?",Long.class,target);}
 void denied(int status,org.junit.jupiter.api.function.Executable work){assertEquals(status,assertThrows(FalloTecnico.class,work).status);}
 @Test void circuitoCompletoNoDaFacultadAntesDeEjecucionNiAutoridadPorRol(){
  var key=UUID.randomUUID();var first=f.concesiones.solicitar(ti,key,request(target));assertEquals(first,f.concesiones.solicitar(ti,key,request(target)));assertEquals(0,roles());
  assertFalse(ids.operador(f.revisor).jefatura());denied(403,()->f.concesiones.decidir(ti,UUID.randomUUID(),first.id(),new Decision(0,"TI no autoriza"),true));
  var approval=f.concesiones.decidir(f.revisor,UUID.randomUUID(),first.id(),new Decision(0,"Autoridad por mandato firmado"),true);assertEquals(0,roles());denied(403,()->f.concesiones.ejecutar(f.revisor,UUID.randomUUID(),first.id(),new Ejecucion(approval.revision())));
  var execution=f.concesiones.ejecutar(ti,UUID.randomUUID(),first.id(),new Ejecucion(approval.revision()));assertEquals(2,execution.revision());assertEquals(1,roles());assertTrue(ids.operador(target).profesionalEn("UPR","DEMO_SEDE_A"));assertFalse(ids.operador(target).jefeEn("UPR","DEMO_SEDE_A"));assertFalse(ids.operador(target).profesionalEn("UPD","DEMO_SEDE_A"));
  assertTrue(f.concesiones.historial(ti,first.id()).size()>=3);
 }
 @Test void rutaAntiguaNoPuedeOtorgarSinAprobacionNiTiAutohabilitarse(){
  denied(409,()->ids.otorgar(ti,target,grant()));denied(403,()->ids.otorgar(ti,ti,grant()));assertEquals(0,roles());
 }
 @Test void ejecucionConcurrenteIdempotenteSoloCreaUnaAsignacion()throws Exception{
  var id=approved();var key=UUID.randomUUID();var start=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(2)){
   var a=pool.submit(()->{assertTrue(start.await(3,TimeUnit.SECONDS));return f.concesiones.ejecutar(ti,key,id,new Ejecucion(1));});var b=pool.submit(()->{assertTrue(start.await(3,TimeUnit.SECONDS));return f.concesiones.ejecutar(ti,key,id,new Ejecucion(1));});start.countDown();assertEquals(a.get(5,TimeUnit.SECONDS),b.get(5,TimeUnit.SECONDS));
  }
  assertEquals(1,roles());assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM approved_assignment WHERE instruction_id=?",Long.class,id.toString()));
 }
 @Test void revocacionRetiraFacultadEInvalidaPrincipalSinBorrarHistoria(){
  var id=approved();f.concesiones.ejecutar(ti,UUID.randomUUID(),id,new Ejecucion(1));var account=ids.cuentas(ti).stream().filter(a->a.id().equals(target)).findFirst().orElseThrow();var p=(IdentidadInstitucional.Principal)ids.autenticar(account.usuario());
  String assignment=d.db.sql.queryForObject("SELECT assignment_id FROM access_instruction WHERE id=?",String.class,id.toString());denied(409,()->ids.revocar(ti,target,assignment));
  f.concesiones.revocar(f.revisor,UUID.randomUUID(),id,new Decision(2,"Revocación fundada"));assertFalse(ids.vigente(p));assertFalse(ids.operador(target).profesionalEn("UPR","DEMO_SEDE_A"));assertEquals(1,roles());assertEquals("REVOCADA",f.concesiones.leer(ti,id).get("state"));denied(409,()->f.concesiones.ejecutar(ti,UUID.randomUUID(),id,new Ejecucion(3)));
 }
 @Test void nuevoRegistroFirmadoExigeNuevaAprobacionAntesDeEjecutar(){
  var id=approved();user("otra.persona");f.sincronizar(ti,grant());denied(409,()->f.concesiones.ejecutar(ti,UUID.randomUUID(),id,new Ejecucion(1)));assertEquals(0,roles());
 }
 @Test void falloDeAuditoriaRevierteAsignacionEpochComandoYEstado(){
  var id=approved();long before=ids.operador(target).versionAcceso();var key=UUID.randomUUID();d.ddl(sql->sql.execute("ALTER TABLE authority_event ADD CONSTRAINT test_authority_event CHECK (action_code<>'EJECUTAR')"));
  try{assertThrows(org.springframework.dao.DataAccessException.class,()->f.concesiones.ejecutar(ti,key,id,new Ejecucion(1)));}finally{d.ddl(sql->sql.execute("ALTER TABLE authority_event DROP CONSTRAINT test_authority_event"));}
  assertEquals(0,roles());assertEquals(before,ids.operador(target).versionAcceso());assertEquals("APROBADA",f.concesiones.leer(ti,id).get("state"));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM authority_command WHERE id=?",Long.class,key.toString()));
  f.concesiones.ejecutar(ti,key,id,new Ejecucion(1));assertEquals(1,roles());
 }
 AutoridadesInstitucionales.Sobre firmado(AutoridadesInstitucionales.Registro r)throws Exception{
  byte[] payload=d.mapper.writeValueAsBytes(r);var signer=Signature.getInstance("Ed25519");signer.initSign(f.firma.getPrivate());signer.update(AutoridadesInstitucionales.contenidoFirmado(payload));return new AutoridadesInstitucionales.Sobre("fixture-autoridad",Base64.getEncoder().encodeToString(payload),Base64.getEncoder().encodeToString(signer.sign()));
 }
 @Test void firmaAjenaAlteradaORevisionReutilizadaNoCambianAutoridad()throws Exception{
  long revision=d.db.sql.queryForObject("SELECT revision FROM authority_registry WHERE id=1",Long.class);var r=new AutoridadesInstitucionales.Registro("1.0",revision+1,clock.instant(),Instant.parse("2099-01-01T00:00:00Z"),List.of(),List.of());var signed=firmado(r);
  var closed=new AutoridadesInstitucionales(d.db,ids,d.mapper,clock,Map.of());denied(403,()->closed.incorporar(signed));assertEquals("NO_ACREDITADA",closed.contexto(ti).get("identidad"));denied(409,()->new ConcesionesInstitucionales(d.db,ids,closed,d.mapper,clock).solicitar(ti,UUID.randomUUID(),request(target)));denied(403,()->f.autoridades.incorporar(new AutoridadesInstitucionales.Sobre(signed.keyId(),signed.payload(),Base64.getEncoder().encodeToString(new byte[64]))));assertEquals(revision,d.db.sql.queryForObject("SELECT revision FROM authority_registry WHERE id=1",Long.class));
 }
 AutoridadesInstitucionales.Registro registro(){String encoded=d.db.sql.queryForObject("SELECT signed_payload FROM authority_batch ORDER BY revision DESC LIMIT 1",String.class);return d.mapper.readValue(Base64.getDecoder().decode(encoded),AutoridadesInstitucionales.Registro.class);}
 void publicar(List<AutoridadesInstitucionales.Vinculo> bindings,List<AutoridadesInstitucionales.Mandato> mandates)throws Exception{var r=registro();f.autoridades.incorporar(firmado(new AutoridadesInstitucionales.Registro("1.0",r.revision()+1,clock.instant(),r.expira(),bindings,mandates)));}
 AutoridadesInstitucionales.Mandato mandato(String person){var m=registro().mandatos().getFirst();return new AutoridadesInstitucionales.Mandato(UUID.randomUUID().toString(),person,m.funcion(),m.rol(),m.alcance(),m.unidad(),m.sede(),m.desde(),m.hasta(),true,true,m.referencia(),m.sha256());}
 @Test void cuentasAliasNoEludenSeparacionYPersonaNoPuedeReasignarse()throws Exception{
  String alias=user("alias.ti"),aliasAutoridad=user("alias.autoridad");var r=registro();var b=new ArrayList<>(r.vinculos());b.add(new AutoridadesInstitucionales.Vinculo(alias,"p-"+ti,from,until,ConcesionFixture.huella(d.db,alias)));b.add(new AutoridadesInstitucionales.Vinculo(aliasAutoridad,"p-"+f.revisor,from,until,ConcesionFixture.huella(d.db,aliasAutoridad)));publicar(b,r.mandatos());
  denied(403,()->f.concesiones.solicitar(ti,UUID.randomUUID(),request(alias)));
  var mandates=new ArrayList<>(r.mandatos());mandates.add(mandato("p-"+ti));publicar(b,mandates);var own=pending();denied(403,()->f.concesiones.decidir(alias,UUID.randomUUID(),own,new Decision(0,"Misma persona solicitante"),true));
  var id=f.concesiones.solicitar(ti,UUID.randomUUID(),request(aliasAutoridad)).id();denied(403,()->f.concesiones.decidir(f.revisor,UUID.randomUUID(),id,new Decision(0,"Misma persona beneficiaria"),true));
  var changed=new ArrayList<>(b);changed.removeIf(v->v.actor().equals(alias));changed.add(new AutoridadesInstitucionales.Vinculo(alias,"otra-persona",from,until,ConcesionFixture.huella(d.db,alias)));denied(409,()->publicar(changed,r.mandatos()));assertEquals("p-"+ti,f.autoridades.persona(alias));
 }
 @Test void previeneCicloDeTresPersonasYNoElevaAutoridadPorCargo()throws Exception{
  String tercero=user("tercera.autoridad");f.sincronizar(ti,grant());var r=registro();var mandates=new ArrayList<>(r.mandatos());mandates.add(mandato("p-"+target));mandates.add(mandato("p-"+tercero));publicar(r.vinculos(),mandates);
  var uno=pending();f.concesiones.decidir(f.revisor,UUID.randomUUID(),uno,new Decision(0,"Primera aprobación"),true);
  var dos=f.concesiones.solicitar(ti,UUID.randomUUID(),request(tercero)).id();f.concesiones.decidir(target,UUID.randomUUID(),dos,new Decision(0,"Segunda aprobación"),true);
  var tres=f.concesiones.solicitar(ti,UUID.randomUUID(),request(f.revisor)).id();var error=assertThrows(FalloTecnico.class,()->f.concesiones.decidir(tercero,UUID.randomUUID(),tres,new Decision(0,"Cierre circular"),true));assertEquals("APROBACION_CRUZADA_NO_PERMITIDA",error.code);assertEquals("PENDIENTE",f.concesiones.leer(ti,tres).get("state"));assertEquals(0,roles());
 }
 @Test void reciprocidadConcurrenteConfirmaUnaSolaDecision()throws Exception{
  var r=registro();var m=new ArrayList<>(r.mandatos());m.add(mandato("p-"+target));publicar(r.vinculos(),m);
  var a=pending();var b=f.concesiones.solicitar(ti,UUID.randomUUID(),request(f.revisor)).id();var start=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(2)){var one=pool.submit(()->approveAfter(start,f.revisor,a));var two=pool.submit(()->approveAfter(start,target,b));start.countDown();var results=new ArrayList<>(List.of(one.get(5,TimeUnit.SECONDS),two.get(5,TimeUnit.SECONDS)));Collections.sort(results);assertEquals(List.of(200,403),results);}
  assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM access_instruction WHERE state='APROBADA'",Long.class));
 }
 int approveAfter(CountDownLatch start,String actor,UUID id)throws Exception{assertTrue(start.await(3,TimeUnit.SECONDS));try{f.concesiones.decidir(actor,UUID.randomUUID(),id,new Decision(0,"Decisión concurrente"),true);return 200;}catch(FalloTecnico e){return e.status;}}
 @Test void idempotenciaNoReutilizaPayloadNiAutorizacionRevocada(){
  var id=approved();var key=UUID.randomUUID();f.concesiones.ejecutar(ti,key,id,new Ejecucion(1));denied(409,()->f.concesiones.ejecutar(ti,key,id,new Ejecucion(2)));f.concesiones.revocar(f.revisor,UUID.randomUUID(),id,new Decision(2,"Retiro autorizado"));denied(409,()->f.concesiones.ejecutar(ti,key,id,new Ejecucion(1)));assertFalse(ids.operador(target).profesionalEn("UPR","DEMO_SEDE_A"));
 }
 @Test void alcanceAjenoYManipulacionPersistidaFallanCerrados(){
  var other=new Solicitud(target,"PROFESIONAL","UNIDAD_SEDE","UPD","DEMO_SEDE_A",from,until,"Ámbito ajeno");var id=f.concesiones.solicitar(ti,UUID.randomUUID(),other).id();denied(403,()->f.concesiones.decidir(f.revisor,UUID.randomUUID(),id,new Decision(0,"Sin mandato"),true));
  var valid=pending();d.db.sql.update("UPDATE access_instruction SET reason_text='Alteración posterior' WHERE id=?",valid.toString());denied(409,()->f.concesiones.decidir(f.revisor,UUID.randomUUID(),valid,new Decision(0,"Documento alterado"),true));assertEquals(0,roles());
 }
 @Test void retiradaDeAutoridadYVencimientoCierranOperaciones()throws Exception{
  var id=approved();var r=registro();publicar(r.vinculos(),List.of());denied(409,()->f.concesiones.ejecutar(ti,UUID.randomUUID(),id,new Ejecucion(1)));denied(403,()->f.concesiones.decidir(f.revisor,UUID.randomUUID(),pending(),new Decision(0,"Sin mandato vigente"),true));
  var future=Clock.fixed(Instant.parse("2100-01-01T00:00:00Z"),ZoneOffset.UTC);var expired=new AutoridadesInstitucionales(d.db,ids,d.mapper,future,Map.of("fixture-autoridad",f.firma.getPublic()));assertEquals("NO_ACREDITADA",expired.contexto(ti).get("identidad"));assertEquals(0,roles());
 }
 @Test void ejecutarYRevocarCompitenPorLaMismaRevisionSinEstadoParcial()throws Exception{
  var id=approved();var start=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(2)){
   var a=pool.submit(()->{start.await();try{f.concesiones.ejecutar(ti,UUID.randomUUID(),id,new Ejecucion(1));return 200;}catch(FalloTecnico e){return e.status;}});
   var b=pool.submit(()->{start.await();try{f.concesiones.revocar(f.revisor,UUID.randomUUID(),id,new Decision(1,"Retiro concurrente"));return 200;}catch(FalloTecnico e){return e.status;}});start.countDown();var results=new ArrayList<>(List.of(a.get(5,TimeUnit.SECONDS),b.get(5,TimeUnit.SECONDS)));Collections.sort(results);assertEquals(List.of(200,409),results);
  }
  var row=f.concesiones.leer(ti,id);assertEquals(2,((Number)row.get("revision")).longValue());assertEquals(row.get("state").equals("EJECUTADA")?1:0,roles());
  if(row.get("state").equals("EJECUTADA"))f.concesiones.revocar(f.revisor,UUID.randomUUID(),id,new Decision(2,"Retiro tras revisión"));assertFalse(ids.operador(target).profesionalEn("UPR","DEMO_SEDE_A"));
 }
 @Test void registroRepetidoEsIdempotentePeroRollbackYContenidoDistintoNoSeAceptan()throws Exception{
  var r=registro();var signed=firmado(r);assertEquals(r.revision(),f.autoridades.incorporar(signed));
  denied(409,()->f.autoridades.incorporar(firmado(new AutoridadesInstitucionales.Registro("1.0",r.revision(),r.emitida(),r.expira(),r.vinculos(),List.of()))));
  denied(409,()->f.autoridades.incorporar(firmado(new AutoridadesInstitucionales.Registro("1.0",r.revision()+2,r.emitida(),r.expira(),r.vinculos(),r.mandatos()))));
 }
 @Test void aprobadorDesactivadoImpideEjecutarYNoConsumeRevision(){
  var id=approved();var c=ids.cuentas(ti).stream().filter(a->a.id().equals(f.revisor)).findFirst().orElseThrow();ids.actualizar(ti,f.revisor,new IdentidadInstitucional.Perfil(c.nombre(),c.correo(),c.cargo(),c.unidad(),c.sede(),false,c.revision()));denied(409,()->f.concesiones.ejecutar(ti,UUID.randomUUID(),id,new Ejecucion(1)));assertEquals("APROBADA",f.concesiones.leer(ti,id).get("state"));assertEquals(0,roles());
 }
 @Test void restablecerClaveDeAutoridadNoPermiteATiSuplantarSuAprobacion()throws Exception{
  var id=approved();var c=ids.cuentas(ti).stream().filter(a->a.id().equals(f.revisor)).findFirst().orElseThrow();String temporal=UUID.randomUUID()+"AZ";
  ids.clave(ti,f.revisor,new IdentidadInstitucional.Clave(temporal,c.revision()));denied(409,()->f.concesiones.ejecutar(ti,UUID.randomUUID(),id,new Ejecucion(1)));
  // Simula que TI conoce su propia contraseña temporal e intenta completar el cambio e impersonar a la autoridad.
  var p=(IdentidadInstitucional.Principal)ids.autenticar(c.usuario());ids.cambiarClavePersonal(p,new IdentidadInstitucional.CambioClave(temporal,UUID.randomUUID()+"AZ"));assertEquals("NO_ACREDITADA",f.autoridades.contexto(f.revisor).get("identidad"));
  var next=pending();denied(409,()->f.concesiones.decidir(f.revisor,UUID.randomUUID(),next,new Decision(0,"Intento con credencial restablecida"),true));denied(409,()->f.concesiones.ejecutar(ti,UUID.randomUUID(),id,new Ejecucion(1)));assertEquals(0,roles());
  // Solo un registro nuevo firmado por el canal independiente puede acreditar la nueva credencial.
  var r=registro();var bindings=r.vinculos().stream().map(v->v.actor().equals(f.revisor)?new AutoridadesInstitucionales.Vinculo(v.actor(),v.persona(),v.desde(),v.hasta(),ConcesionFixture.huella(d.db,v.actor())):v).toList();publicar(bindings,r.mandatos());denied(409,()->f.concesiones.ejecutar(ti,UUID.randomUUID(),id,new Ejecucion(1)));
  f.concesiones.decidir(f.revisor,UUID.randomUUID(),next,new Decision(0,"Nueva acreditación independiente"),true);f.concesiones.ejecutar(ti,UUID.randomUUID(),next,new Ejecucion(1));assertEquals(1,roles());
 }
 void desactivar(String admin,String actor){var c=ids.cuentas(admin).stream().filter(a->a.id().equals(actor)).findFirst().orElseThrow();ids.actualizar(admin,actor,new IdentidadInstitucional.Perfil(c.nombre(),c.correo(),c.cargo(),c.unidad(),c.sede(),false,c.revision()));}
 @Test void beneficiarioDesactivadoNoRecibeAsignacionNiPermiteReplayDeEjecucion(){
  var id=approved();desactivar(ti,target);denied(409,()->f.concesiones.ejecutar(ti,UUID.randomUUID(),id,new Ejecucion(1)));assertEquals(0,roles());assertEquals("APROBADA",f.concesiones.leer(ti,id).get("state"));denied(409,()->f.concesiones.solicitar(ti,UUID.randomUUID(),request(target)));
  var c=ids.cuentas(ti).stream().filter(a->a.id().equals(target)).findFirst().orElseThrow();ids.actualizar(ti,target,new IdentidadInstitucional.Perfil(c.nombre(),c.correo(),c.cargo(),c.unidad(),c.sede(),true,c.revision()));var key=UUID.randomUUID();f.concesiones.ejecutar(ti,key,id,new Ejecucion(1));desactivar(ti,target);denied(409,()->f.concesiones.ejecutar(ti,key,id,new Ejecucion(1)));assertEquals(1,roles());
  // Retirar una concesión de una cuenta desactivada sí debe seguir siendo posible.
  f.concesiones.revocar(f.revisor,UUID.randomUUID(),id,new Decision(2,"Retiro de cuenta inactiva"));assertEquals("REVOCADA",f.concesiones.leer(ti,id).get("state"));
 }
 @Test void otroTiNoEjecutaSolicitudDeCuentaDesactivadaSinInvalidarSuPropiaIdentidad(){
  String other=user("segundo.ti");ConcesionFixture.otorgar(d.db,ids,ti,other,new IdentidadInstitucional.Otorgar("TI","INSTITUCION",null,null,from,until),clock);var id=approved();desactivar(other,ti);
  denied(409,()->f.concesiones.ejecutar(other,UUID.randomUUID(),id,new Ejecucion(1)));assertTrue(ids.operador(other).ti());assertEquals(0,roles());assertEquals("APROBADA",f.concesiones.leer(other,id).get("state"));
 }
}
