package cl.nexbyte.siger.institucional.smr;
import cl.nexbyte.siger.institucional.archivos.ArchivoPrivado;import cl.nexbyte.siger.institucional.robustez.FalloTecnico;
import java.nio.file.*;import java.util.*;import java.time.*;import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;
class SmrGateTest {
 @TempDir Path dir;
 MockEnvironment valid()throws Exception{
  Path root=Files.createDirectory(dir.resolve("private")),store=Files.createDirectory(dir.resolve("store"));ArchivoPrivado.proteger(root);ArchivoPrivado.proteger(store);
  var input=new HashMap<String,String>();for(String name:List.of("private-key","trusted-keys","own-trusted-keys")){var f=root.resolve(name);Files.writeString(f,"{}");ArchivoPrivado.proteger(f);input.put("institucional.comprobante."+name,f.toString());}
  input.putAll(Map.of("institucional.db.url","jdbc:mysql://127.0.0.1:13316/smr_test?sslMode=VERIFY_IDENTITY","institucional.db.user","smr_test_app","institucional.db.password","secreto-sintetico","server.ssl.key-store-password","secreto-sintetico","institucional.comprobante.key-id","smr-test-ack","institucional.configuracion-privada.directorio",root.toString(),"institucional.documentos.directorio",store.toString(),"institucional.formulario-completo.origen-publico","https://localhost:19443","institucional.smr.instalacion-id",UUID.randomUUID().toString(),"institucional.smr.sintetico","true"));
  var e=new MockEnvironment();e.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("smr-test",ArranqueSmr.configurar(input)));e.setActiveProfiles("smr","jdbc-reception");return e;
 }
 @Test void modoPropioSinNucleoSinteticoNiDependenciaDelOrigenDisponible()throws Exception{var e=valid();assertDoesNotThrow(()->ArranqueSmr.validar(e));assertFalse(e.getProperty("institucional.nucleo.habilitado",Boolean.class));assertFalse(e.getProperty("institucional.formulario-completo.automatico",Boolean.class));assertNotEquals(e.getProperty("server.servlet.session.cookie.name"),ArranqueSmr.configurar(Map.of("institucional.smr.instalacion-id",UUID.randomUUID().toString())).get("server.servlet.session.cookie.name"));}
 @Test void rechazaBaseQaTlsDebilYRamaDeNegocio()throws Exception{var e=valid();e.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("unsafe",Map.of("institucional.db.url","jdbc:mysql://127.0.0.1:3306/siger_institucional_windows_qa?sslMode=VERIFY_IDENTITY")));assertThrows(IllegalStateException.class,()->ArranqueSmr.validar(e));}
 @Test void rechazaPerfilQaYConfiguracionAjena()throws Exception{var e=valid();e.setActiveProfiles("smr","jdbc-reception","windows-lab");assertThrows(IllegalStateException.class,()->ArranqueSmr.validar(e));assertThrows(IllegalArgumentException.class,()->ArranqueSmr.configurar(Map.of("spring.flyway.enabled","true")));}
 @Test void ventanaYHuellaDeCustodiaSeExigenAntesDeLaRed()throws Exception{
  var e=valid();Path a=dir.resolve("approval.json");Files.writeString(a,"{}");ArchivoPrivado.proteger(a);e.setProperty("institucional.smr.autorizacion.archivo",a.toString());
  var failure=assertThrows(FalloTecnico.class,()->ArranqueSmr.verificarAutorizacion(e));assertEquals("CUSTODIA_COLA_NO_AUTORIZADA",failure.code);
  String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(e.getRequiredProperty("institucional.comprobante.own-trusted-keys")))));
  var approval=new HashMap<String,Object>();for(String flag:List.of("custodiaEnEstePc","todaColaElegible","receptorUnico","respaldoVerificado","confianzaAckInstalada","pullUbuntuAbierto"))approval.put(flag,true);approval.put("referencia","PRUEBA_SINTETICA_NO_PRODUCTIVA");approval.put("ventanaHastaUtc",Instant.now().plusSeconds(60).toString());approval.put("ackTrustSha256",hash);
  Files.writeString(a,tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(approval));assertDoesNotThrow(()->ArranqueSmr.verificarAutorizacion(e));approval.put("ventanaHastaUtc",Instant.now().minusSeconds(1).toString());Files.writeString(a,tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(approval));assertThrows(FalloTecnico.class,()->ArranqueSmr.verificarAutorizacion(e));
 }
}
