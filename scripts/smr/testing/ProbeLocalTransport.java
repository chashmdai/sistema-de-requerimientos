import cl.nexbyte.siger.institucional.formulario.*;import cl.nexbyte.siger.institucional.robustez.*;import java.net.*;import java.nio.file.*;import java.util.*;
class ProbeLocalTransport {
 public static void main(String[] args)throws Exception{var json=EntregaCompleta.lector();var p=json.readTree(System.in);boolean tls=false,bearer=false;
  try(var good=new FuenteFormulario(URI.create(p.path("origin").asString()),p.path("token").asString(),Path.of(p.path("ca").asString()),false)){if(!good.comprobarConexion().preparada())throw new IllegalStateException("Sonda local no preparada");}
  try(var wrong=new FuenteFormulario(URI.create(p.path("origin").asString()),"v".repeat(64),Path.of(p.path("ca").asString()),false)){bearer=wrong.comprobarConexion().autenticada()==401;}
  try(var wrong=new FuenteFormulario(URI.create(p.path("origin").asString()),p.path("token").asString(),Path.of(p.path("wrongCa").asString()),false)){wrong.comprobarSinCredencial();}catch(FalloTecnico expected){tls="TLS_NO_VERIFICADO".equals(expected.code);}
  if(!tls||!bearer)throw new IllegalStateException("TLS/Bearer inválidos no rechazados");System.out.println(json.writeValueAsString(Map.of("validProbe","401/401/404","invalidBearerRejected",bearer,"wrongCaRejected",tls,"pullPerformed",false)));
 }
}
