package cl.nexbyte.siger.institucional.robustez;
import java.nio.file.*;import java.security.PublicKey;import java.net.URI;import java.util.*;
import org.springframework.context.annotation.*;import org.springframework.core.env.Environment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.scheduling.annotation.Scheduled;
import tools.jackson.databind.json.JsonMapper;

@Configuration @Profile("jdbc-reception")
public class JdbcInstitucionalConfiguration {
    @Bean BaseJdbc institucionalDb(Environment env){
        var url=env.getRequiredProperty("institucional.db.url");
        ConfiguracionDatos.validar(url,env.getProperty("datos.mysql-local",Boolean.class,false)&&"127.0.0.1".equals(env.getProperty("server.address")));
        if(env.matchesProfiles("local-integration-demo"))throw new IllegalArgumentException("Perfiles de recepción incompatibles");
        return new BaseJdbc(url,env.getRequiredProperty("institucional.db.user"),env.getRequiredProperty("institucional.db.password"),env.getProperty("institucional.db.pool",Integer.class,12));
    }
    @Bean ControlesOperacion controles(BaseJdbc db){return new ControlesOperacion(db);}
    @Bean RegistroOperacionesExternas operaciones(BaseJdbc db){return new RegistroOperacionesExternas(db);}
    @Bean RecepcionInstitucionalJdbc recepcionJdbc(BaseJdbc db,JsonMapper mapper,Environment env)throws java.io.IOException{
        var path=Path.of(env.getRequiredProperty("institucional.comprobante.trusted-keys"));
        if(Files.size(path)>65536)throw new IllegalArgumentException("Archivo de claves demasiado grande");
        var tree=mapper.readTree(Files.readString(path));var trust=new HashMap<String,PublicKey>();
        for(var entry:tree.properties()){
            if(!entry.getKey().matches("[A-Za-z0-9_-]{1,64}"))throw new IllegalArgumentException("KeyId inválido");
            trust.put(entry.getKey(),ComprobanteDigital.publicPem(entry.getValue().asString()));
        }
        if(trust.isEmpty()||trust.size()>20)throw new IllegalArgumentException("Claves confiadas requeridas");
        var ownPath=Path.of(env.getRequiredProperty("institucional.comprobante.own-trusted-keys"));
        if(Files.size(ownPath)>65536)throw new IllegalArgumentException("Claves demasiado grandes");
        var own=new HashMap<String,PublicKey>();
        for(var entry:mapper.readTree(Files.readString(ownPath)).properties()){
            if(!entry.getKey().matches("[A-Za-z0-9_-]{1,64}"))throw new IllegalArgumentException("KeyId inválido");
            own.put(entry.getKey(),ComprobanteDigital.publicPem(entry.getValue().asString()));
        }
        if(own.isEmpty()||own.size()>20)throw new IllegalArgumentException("Falta confianza institucional");
        var keyPath=Path.of(env.getRequiredProperty("institucional.comprobante.private-key"));
        if(Files.size(keyPath)>8192)throw new IllegalArgumentException("Clave demasiado grande");
        return new RecepcionInstitucionalJdbc(db,mapper,trust,env.getRequiredProperty("institucional.comprobante.key-id"),
            ComprobanteDigital.privatePem(Files.readString(keyPath)),own,env.getProperty("institucional.folio.prefix","COM"),env.getProperty("institucional.folio.width",Integer.class,6),java.time.Clock.systemUTC());
    }
    @Bean @ConditionalOnProperty(name="institucional.v2.habilitada",havingValue="true")
    FuenteHttpV2 fuenteV2(Environment env,JsonMapper mapper){
        var origin=URI.create(env.getRequiredProperty("institucional.integracion.origen-publico"));var token=env.getRequiredProperty("institucional.integracion.token");
        boolean local=env.getProperty("institucional.v2.http-local",Boolean.class,false)&&"127.0.0.1".equals(env.getProperty("server.address"));
        var ca=env.getProperty("institucional.v2.ca-certificado","");
        return ca.isBlank()?new FuenteHttpV2(origin,token,mapper,local):FuenteHttpV2.conCA(origin,token,mapper,local,Path.of(ca));
    }
    @Bean @ConditionalOnProperty(name="institucional.v2.habilitada",havingValue="true")
    RecuperarEntregasV2 recuperar(FuenteHttpV2 source,RecepcionInstitucionalJdbc recepcion,Environment env){return new RecuperarEntregasV2(source,recepcion,env.getProperty("institucional.v2.paralelismo",Integer.class,4));}
    @Bean @ConditionalOnProperty(name="institucional.v2.habilitada",havingValue="true")
    PlanificadorV2 planificador(RecuperarEntregasV2 service,ControlesOperacion controls,BaseJdbc db){return new PlanificadorV2(service,controls,db);}
    static final class PlanificadorV2 {
        private final RecuperarEntregasV2 service;private final ControlesOperacion controls;private final BaseJdbc db;
        private int fallos;private long siguiente;
        PlanificadorV2(RecuperarEntregasV2 service,ControlesOperacion controls,BaseJdbc db){this.service=service;this.controls=controls;this.db=db;}
        @Scheduled(fixedDelayString="${institucional.v2.intervalo-ms:1000}")public void tick(){
            if(System.currentTimeMillis()<siguiente)return;
            try{
                var config=controls.estado();if(!config.recuperar()||!config.contingencia().equals("NORMAL"))return;
                service.conciliarConfirmaciones();
                long deadline=System.nanoTime()+java.time.Duration.ofSeconds(2).toNanos();
                int failed=0;
                for(int batch=0;batch<10;batch++){
                    var result=service.ejecutar();failed+=result.fallidas();
                    if(result.recuperadas()<20||failed>0||System.nanoTime()>=deadline)break;
                }
                if(failed>0){diferir();}else{if(fallos>0)db.transaction(()->{db.event("RECUPERACION","origen-publico","RECUPERADA");return null;});fallos=0;}
            }catch(RuntimeException e){
                if(fallos==0)try{db.transaction(()->{db.event("RECUPERACION","origen-publico","FUENTE_NO_DISPONIBLE");return null;});}catch(RuntimeException ignored){}
                diferir();
            }
        }
        private void diferir(){fallos=Math.min(7,fallos+1);siguiente=System.currentTimeMillis()+Math.min(300000,5000L<<(fallos-1))+java.util.concurrent.ThreadLocalRandom.current().nextLong(1,1000);}
    }
}
