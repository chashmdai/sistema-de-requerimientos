package cl.nexbyte.siger.institucional.formulario;
import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.*;import java.time.*;import java.util.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import org.junit.jupiter.params.ParameterizedTest;import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
class SmrReceptionTest {
 @TempDir Path dir;
 String resource(String name)throws Exception{try(var in=getClass().getResourceAsStream("/smr/"+name)){return new String(Objects.requireNonNull(in).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}}
 @ParameterizedTest @ValueSource(strings={"1.0","1.1"}) void vectoresUbuntuSeVerificanConJavaAisladosYConRelojControlado(String version)throws Exception{
  try(var d=new DatosFormularioTest(dir)){
   d.now=Instant.parse("2026-11-04T12:01:00Z");
   var tree=d.mapper.readTree(resource("pull-respuesta-"+version+".json")).path("entregas").get(0);
   assertTrue(tree.path("venceEn").asLong()>d.clock.millis());assertTrue(tree.path("venceEn").asLong()<System.currentTimeMillis());
   var e=d.mapper.treeToValue(tree.path("entrega"),EntregaCompleta.class);var trust=Map.of("FORM_FIXTURE_PUBLIC",ComprobanteDigital.publicPem(resource("fixture-productor-public.pem")));
   var own=new HashMap<>(d.own);own.put("SMR_FIXTURE_ACK",ComprobanteDigital.publicPem(resource("fixture-ack-public.pem")));
   e.validar(trust,d.mapper);var ack=d.mapper.treeToValue(d.mapper.readTree(resource("ack-solicitud-"+version+".json")).path("recepcion"),RegistroInstitucional.class);e.validarRegistro(ack,own,d.mapper);
   assertEquals("NNA",e.tipo());assertEquals("1.1",ack.registro().version());
   for(var a:e.adjuntos())d.files.guardar(e.id(),a,new java.io.ByteArrayInputStream(resource("original-sintetico.txt").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
   var receiver=new RecepcionInstitucionalJdbc(d.db,d.mapper,trust,"institution-test",d.institutionKeys.getPrivate(),own,"COM",6,d.clock);
   var receipt=receiver.guardarCompleta(e,d.files,UUID.fromString(tree.path("reserva").asString()));
   assertEquals(1,receiver.confirmacionesCompletas().size());assertEquals(e.comunicacion(),receiver.completaRegistrada(receipt.registro().comunicacionId()).comunicacion());
  }
 }
 @Test void reservaYComprobanteSeConfirmanEnUnaSolaTransaccionYSeRecuperanTrasReinicio()throws Exception{
  try(var d=new DatosFormularioTest(dir)){var e=d.delivery("1.1","NNA",List.of(),false,1);var lease=UUID.randomUUID();var receipt=d.receiver.guardarCompleta(e,d.files,lease);d.restart();assertEquals(lease,d.receiver.confirmacionesCompletas().getFirst().reserva().reserva());assertEquals(receipt,d.receiver.guardarCompleta(e,d.files,UUID.randomUUID()));assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Integer.class));}
 }
 @Test void falloPersistiendoReservaRevierteTambienElRegistro()throws Exception{
  try(var d=new DatosFormularioTest(dir)){d.ddl("ALTER TABLE institutional_inbox ADD CONSTRAINT smr_fail_lease CHECK (ack_lease IS NULL)");var e=d.delivery("1.0","ADULTO_DECLARADO",List.of(),false,1);assertThrows(RuntimeException.class,()->d.receiver.guardarCompleta(e,d.files,UUID.randomUUID()));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Integer.class));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM institutional_inbox",Integer.class));}
 }
 @Test void bootstrapReintentadoTrasCommitNoCreaNiReiniciaCuentas()throws Exception{
  try(var d=new DatosFormularioTest(dir)){
   var identity=new cl.nexbyte.siger.institucional.identidad.IdentidadInstitucional(d.db,d.clock);var access=new cl.nexbyte.siger.institucional.smr.AccesoSmr(d.db,identity);
   Path root=Files.createDirectory(dir.resolve("private"));cl.nexbyte.siger.institucional.archivos.ArchivoPrivado.proteger(root);Path file=root.resolve("bootstrap.json");
   var data=new cl.nexbyte.siger.institucional.smr.SmrConfiguration.Bootstrap(new cl.nexbyte.siger.institucional.identidad.IdentidadInstitucional.Crear("smr.test","Prueba sintética","smr.test@example.invalid","Pruebas",null,null,UUID.randomUUID()+"Az"),true,true);
   String json=d.mapper.writeValueAsString(data);Files.writeString(file,json);cl.nexbyte.siger.institucional.archivos.ArchivoPrivado.proteger(file);
   var env=new org.springframework.mock.env.MockEnvironment().withProperty("institucional.configuracion-privada.directorio",root.toString()).withProperty("institucional.smr.bootstrap.archivo",file.toString());
   var runner=new cl.nexbyte.siger.institucional.smr.SmrConfiguration().bootstrapSmr(identity,access,d.db,env);runner.run(new org.springframework.boot.DefaultApplicationArguments());
   // Reproduce caída después de COMMIT y antes de retirar el archivo privado inicial.
   Files.delete(root.resolve("bootstrap.json.aplicado"));Files.writeString(file,json);runner.run(new org.springframework.boot.DefaultApplicationArguments());
   assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM staff_account",Integer.class));assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM identity_event WHERE event_code='SMR_BOOTSTRAP_APLICADO'",Integer.class));assertEquals("{\"applied\":true}",Files.readString(root.resolve("bootstrap.json.aplicado")));
  }
 }
}
