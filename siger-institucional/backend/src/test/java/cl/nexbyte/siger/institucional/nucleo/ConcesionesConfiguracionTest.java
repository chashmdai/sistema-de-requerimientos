package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.identidad.*;
import cl.nexbyte.siger.institucional.archivos.ArchivoPrivado;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.boot.DefaultApplicationArguments;
import static org.junit.jupiter.api.Assertions.*;

class ConcesionesConfiguracionTest {
 @TempDir Path temp;
 @Test void archivosPrivadosConClavePublicaYSobreGrandeHabilitanSoloLoFirmado()throws Exception{
  ArchivoPrivado.proteger(temp);
  try(var d=new DatosNucleoTest(temp)){
   var ids=new IdentidadInstitucional(d.db,Clock.systemUTC());String ti=ids.bootstrap(new IdentidadInstitucional.Crear("ti.config","TI sintético","ti.config@example.invalid","TI",null,null,UUID.randomUUID()+"AZ")).id();var f=ConcesionFixture.contexto(d.db,ids,ti,Clock.systemUTC());
   var grant=new IdentidadInstitucional.Otorgar("PROFESIONAL","UNIDAD_SEDE","UPR","DEMO_SEDE_A",Instant.now(),Instant.now().plusSeconds(3600));f.sincronizar(ti,grant);
   var mandates=new ArrayList<AutoridadesInstitucionales.Mandato>();var original=f.mandatos.values().iterator().next();
   for(int i=0;i<12;i++)mandates.add(new AutoridadesInstitucionales.Mandato("m-"+i,original.persona(),original.funcion(),original.rol(),original.alcance(),original.unidad(),original.sede(),original.desde(),original.hasta(),true,true,"Respaldo sintético "+"x".repeat(120),original.sha256()));
   var r=d.mapper.readValue(Base64.getDecoder().decode(d.db.sql.queryForObject("SELECT signed_payload FROM authority_batch ORDER BY revision DESC LIMIT 1",String.class)),AutoridadesInstitucionales.Registro.class);
   byte[] payload=d.mapper.writeValueAsBytes(new AutoridadesInstitucionales.Registro("1.0",2,Instant.now(),r.expira(),r.vinculos(),mandates));var signer=Signature.getInstance("Ed25519");signer.initSign(f.firma.getPrivate());signer.update(AutoridadesInstitucionales.contenidoFirmado(payload));var sobre=new AutoridadesInstitucionales.Sobre("fixture-autoridad",Base64.getEncoder().encodeToString(payload),Base64.getEncoder().encodeToString(signer.sign()));
   String pem="-----BEGIN PUBLIC KEY-----\n"+Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(f.firma.getPublic().getEncoded())+"\n-----END PUBLIC KEY-----";
   Path trust=temp.resolve("public-trust.json"),envelope=temp.resolve("signed-envelope.json");Files.write(trust,d.mapper.writeValueAsBytes(new ConcesionesConfiguration.Confianza(List.of(new ConcesionesConfiguration.ClavePublica("fixture-autoridad",pem)))));Files.write(envelope,d.mapper.writeValueAsBytes(sobre));ArchivoPrivado.proteger(trust);ArchivoPrivado.proteger(envelope);assertTrue(Files.size(envelope)>4096);
   var env=new MockEnvironment().withProperty("institucional.configuracion-privada.directorio",temp.toString()).withProperty("institucional.autoridad.confianza-archivo",trust.toString()).withProperty("institucional.autoridad.sobre-archivo",envelope.toString());var config=new ConcesionesConfiguration();var service=config.autoridadesInstitucionales(d.db,ids,d.mapper,env);config.incorporarAutoridades(service,d.mapper,env).run(new DefaultApplicationArguments());assertEquals(2L,service.contexto(ti).get("revisionRegistro"));assertEquals(12,((List<?>)service.contexto(f.revisor).get("mandatos")).size());
   // Reemplazar la clave bajo el mismo identificador no autentica el registro firmado por la anterior.
   var rotated=new AutoridadesInstitucionales(d.db,ids,d.mapper,Clock.systemUTC(),Map.of("fixture-autoridad",KeyPairGenerator.getInstance("Ed25519").generateKeyPair().getPublic()));assertEquals("NO_ACREDITADA",rotated.contexto(ti).get("identidad"));
   // La decisión usa el sobre íntegro, no una atribución ampliada alterando una proyección SQL.
   d.db.sql.update("UPDATE authority_mandate SET function_code='Alteración de proyección' WHERE batch_revision=2");assertFalse(service.contexto(f.revisor).toString().contains("Alteración de proyección"));
  }
 }
 @Test void sinConfiguracionNoImportaYErroresNoRevelanContenidoPrivado()throws Exception{
  var reader=AutoridadesInstitucionales.lectorPrivado();assertThrows(RuntimeException.class,()->reader.readValue("{\"keyId\":\"a\",\"keyId\":\"b\"}",AutoridadesInstitucionales.Sobre.class));assertThrows(RuntimeException.class,()->reader.readValue("{\"keyId\":\"a\",\"payload\":\"a\",\"firma\":\"a\",\"extra\":true}",AutoridadesInstitucionales.Sobre.class));
  ArchivoPrivado.proteger(temp);try(var d=new DatosNucleoTest(temp)){
   var ids=new IdentidadInstitucional(d.db,Clock.systemUTC());var config=new ConcesionesConfiguration();var closed=config.autoridadesInstitucionales(d.db,ids,d.mapper,new MockEnvironment());config.incorporarAutoridades(closed,d.mapper,new MockEnvironment()).run(new DefaultApplicationArguments());assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM authority_batch",Long.class));
   Path file=temp.resolve("malformado.json");String marker="CONTENIDO_PRIVADO_SINTETICO";Files.writeString(file,marker);ArchivoPrivado.proteger(file);var env=new MockEnvironment().withProperty("institucional.configuracion-privada.directorio",temp.toString()).withProperty("institucional.autoridad.confianza-archivo",file.toString());var error=assertThrows(IllegalStateException.class,()->config.autoridadesInstitucionales(d.db,ids,d.mapper,env));assertFalse(error.getMessage().contains(marker));assertNull(error.getCause());
  }
 }
}
