package cl.nexbyte.siger.institucional.robustez;
import cl.nexbyte.siger.institucional.integracion.SobreComunicacion;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

public final class FuenteHttpV2 implements FuenteEntregasV2,AutoCloseable {
    public record Lote(List<Reserva> entregas){}
    private final URI origin;private final String token;private final JsonMapper mapper;private final HttpClient http;private final Duration timeout;
    public FuenteHttpV2(URI origin,String token,JsonMapper mapper,boolean local){this(origin,token,mapper,local,Duration.ofSeconds(10));}
    public FuenteHttpV2(URI origin,String token,JsonMapper mapper,boolean local,Duration timeout){
        this(origin,token,mapper,local,timeout,null);
    }
    private FuenteHttpV2(URI origin,String token,JsonMapper mapper,boolean local,Duration timeout,javax.net.ssl.SSLContext ssl){
        boolean loopback=origin.getHost()!=null&&Set.of("127.0.0.1","localhost","[::1]").contains(origin.getHost());
        if(origin.getHost()==null||origin.getUserInfo()!=null||origin.getQuery()!=null||origin.getFragment()!=null
            ||!(origin.getPath().isEmpty()||origin.getPath().equals("/"))||!("https".equals(origin.getScheme())||(local&&loopback&&"http".equals(origin.getScheme())))
            ||token==null||!token.matches("[A-Za-z0-9_-]{32,400}"))throw new IllegalArgumentException("Origen o credencial inválidos");
        this.origin=origin;this.token=token;this.mapper=mapper;this.timeout=timeout;
        var builder=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER);
        if(ssl!=null)builder.sslContext(ssl);
        http=builder.build();
    }
    /** CA explícita de esta conexión, con verificación normal de cadena, vigencia y hostname. */
    public static FuenteHttpV2 conCA(URI origin,String token,JsonMapper mapper,boolean local,java.nio.file.Path path){
        try{
            if(java.nio.file.Files.size(path)>32768)throw new IllegalArgumentException("CA demasiado grande");
            java.security.cert.X509Certificate cert;
            try(var input=java.nio.file.Files.newInputStream(path)){cert=(java.security.cert.X509Certificate)java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(input);}
            cert.checkValidity();if(cert.getBasicConstraints()<0)throw new IllegalArgumentException("Se requiere CA");
            var store=java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType());store.load(null,null);store.setCertificateEntry("origen-publico",cert);
            var managers=javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());managers.init(store);
            var ssl=javax.net.ssl.SSLContext.getInstance("TLS");ssl.init(null,managers.getTrustManagers(),null);
            return new FuenteHttpV2(origin,token,mapper,local,Duration.ofSeconds(10),ssl);
        }catch(java.security.GeneralSecurityException|java.io.IOException e){throw new IllegalArgumentException("CA configurada inválida");}
    }
    public List<Reserva> reclamar(){
        var batch=mapper.readValue(post("/integracion/v3/entregas/reclamar",Map.of("limite",20),200),Lote.class);
        if(batch==null||batch.entregas()==null||batch.entregas().size()>20)throw new FalloTecnico(502);
        var result=List.copyOf(batch.entregas());
        if(result.stream().anyMatch(r->r==null||r.reserva()==null||r.entrega()==null||r.intento()<1)
            ||result.stream().map(r->r.entrega().comunicacion().id()).distinct().count()!=result.size())throw new FalloTecnico(502);
        return result;
    }
    public void confirmar(Reserva r,RegistroInstitucional receipt){post("/integracion/v3/entregas/"+r.entrega().comunicacion().id()+"/confirmacion",Map.of("reserva",r.reserva(),"recepcion",receipt),204);}
    public void fallo(Reserva r,String code){post("/integracion/v3/entregas/"+r.entrega().comunicacion().id()+"/fallo",Map.of("reserva",r.reserva(),"codigo",code),204);}
    private byte[] post(String path,Object body,int expected){
        try{
            var request=HttpRequest.newBuilder(origin.resolve(path)).timeout(timeout).header("Content-Type","application/json")
                .header("Authorization","Bearer "+token).POST(HttpRequest.BodyPublishers.ofByteArray(mapper.writeValueAsBytes(body))).build();
            var response=http.send(request,HttpResponse.BodyHandlers.limiting(HttpResponse.BodyHandlers.ofByteArray(),524288));
            if(response.statusCode()!=expected)throw new FalloTecnico(response.statusCode());return response.body();
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new FalloTecnico(503);}
        catch(java.io.IOException e){throw new FalloTecnico(503);}
    }
    public void close(){http.close();}
}
