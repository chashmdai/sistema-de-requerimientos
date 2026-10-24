package cl.nexbyte.siger.institucional.formulario;
import cl.nexbyte.siger.institucional.robustez.*;
import java.io.*;import java.net.*;import java.net.http.*;import java.nio.file.*;import java.time.*;import java.util.*;import java.util.concurrent.*;import tools.jackson.databind.json.JsonMapper;

/** PULL independiente del consumidor histórico. Nunca sigue redirects ni propaga credenciales al navegador. */
public final class FuenteFormulario implements AutoCloseable {
    public record Reserva(UUID reserva,long venceEn,int intento,EntregaCompleta entrega){}
    public record Lote(List<Reserva> entregas){}
    private static final String ROOT="/integracion/v3/formulario-completo/entregas";
    private final URI origin;private final String token;private final HttpClient http;private final JsonMapper mapper=EntregaCompleta.lector();
    public FuenteFormulario(URI origin,String token,Path ca,boolean local)throws IOException{
        if(origin.getHost()==null||origin.getUserInfo()!=null||origin.getQuery()!=null||origin.getFragment()!=null||!Set.of("","/").contains(origin.getPath())||!("https".equals(origin.getScheme())||local&&"http".equals(origin.getScheme())&&Set.of("localhost","127.0.0.1").contains(origin.getHost()))||token==null||!token.matches("[A-Za-z0-9_-]{32,400}"))throw new IllegalArgumentException("Origen inválido");
        this.origin=origin;this.token=token;var builder=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER);
        if(ca!=null)try{
            if(Files.size(ca)>32768)throw new IOException("CA excesiva");java.security.cert.X509Certificate cert;try(var in=Files.newInputStream(ca)){cert=(java.security.cert.X509Certificate)java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(in);}cert.checkValidity();if(cert.getBasicConstraints()<0)throw new IOException("CA requerida");
            var store=java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType());store.load(null,null);store.setCertificateEntry("publico",cert);var trust=javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());trust.init(store);var ssl=javax.net.ssl.SSLContext.getInstance("TLS");ssl.init(null,trust.getTrustManagers(),null);builder.sslContext(ssl);
        }catch(java.security.GeneralSecurityException e){throw new IOException("CA inválida");}
        http=builder.build();
    }
    private HttpRequest.Builder request(String path){return HttpRequest.newBuilder(origin.resolve(ROOT+path)).timeout(Duration.ofSeconds(20)).header("Authorization","Bearer "+token);}
    private byte[] post(String path,Object value,int expected){try{var response=http.send(request(path).header("Content-Type","application/json").header("X-Formulario-Versiones","1.0,1.1").POST(HttpRequest.BodyPublishers.ofByteArray(mapper.writeValueAsBytes(value))).build(),HttpResponse.BodyHandlers.limiting(HttpResponse.BodyHandlers.ofByteArray(),2*1024*1024));if(response.statusCode()!=expected)throw new FalloTecnico(response.statusCode());return response.body();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new FalloTecnico(503);}catch(IOException e){throw new FalloTecnico(503);}}
    public List<Reserva> reclamar(){Lote b;try{b=mapper.readValue(post("/reclamar",Map.of("limite",1),200),Lote.class);}catch(tools.jackson.core.JacksonException|IllegalArgumentException invalid){throw new FalloTecnico(502);}if(b==null||b.entregas()==null||b.entregas().size()>1||b.entregas().stream().anyMatch(r->r==null||r.reserva()==null||r.entrega()==null||r.intento()<1||r.venceEn()<=System.currentTimeMillis()))throw new FalloTecnico(502);return List.copyOf(b.entregas());}
    public void descargar(Reserva r,EntregaCompleta.Adjunto a,ArchivoInstitucional files)throws IOException{
        if(files.disponible(r.entrega().id(),a))return;
        InputStream stream=null;
        try{
            var response=http.send(request("/"+r.entrega().id()+"/adjuntos/"+a.id()).header("X-Reserva",r.reserva().toString()).GET().build(),HttpResponse.BodyHandlers.ofInputStream());stream=response.body();
            if(response.statusCode()!=200)throw new FalloTecnico(response.statusCode());
            long length=response.headers().firstValueAsLong("Content-Length").orElse(-1);if(length!=-1&&length!=a.tamanio())throw new IOException("Longitud no corresponde");
            var input=stream;try(var worker=Executors.newVirtualThreadPerTaskExecutor()){
                var transfer=worker.submit(()->{files.guardar(r.entrega().id(),a,input);return true;});
                try{transfer.get(Math.max(1,Math.min(240000,r.venceEn()-System.currentTimeMillis()-1000)),TimeUnit.MILLISECONDS);}
                catch(TimeoutException e){input.close();transfer.cancel(true);throw new IOException("Reserva o transferencia vencida");}
                catch(ExecutionException e){throw new IOException("Transferencia inválida");}
            }
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Transferencia interrumpida");}finally{if(stream!=null)stream.close();}
    }
    public void confirmar(Reserva r,RegistroInstitucional receipt){post("/"+r.entrega().id()+"/confirmacion",Map.of("reserva",r.reserva(),"recepcion",receipt),204);}
    public void fallo(Reserva r,String code){post("/"+r.entrega().id()+"/fallo",Map.of("reserva",r.reserva(),"codigo",code),204);}
    public void close(){http.shutdownNow();}
}
