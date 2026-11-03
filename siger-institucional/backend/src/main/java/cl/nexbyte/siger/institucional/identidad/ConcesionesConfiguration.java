package cl.nexbyte.siger.institucional.identidad;

import cl.nexbyte.siger.institucional.archivos.*;
import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.*;
import java.security.*;
import java.time.Clock;
import java.util.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

@Configuration @ConditionalOnProperty(name="institucional.identidad.habilitada",havingValue="true")
public class ConcesionesConfiguration {
    public record ClavePublica(String id,String pem){}
    public record Confianza(List<ClavePublica> claves){}
    @Bean public AutoridadesInstitucionales autoridadesInstitucionales(BaseJdbc db,IdentidadInstitucional ids,JsonMapper json,Environment env){
        Map<String,PublicKey> trust=new HashMap<>();String file=env.getProperty("institucional.autoridad.confianza-archivo");
        if(file!=null)try{
            Path path=ConfiguracionPrivada.ruta(env,file);ArchivoPrivado.validar(path,65536);
            var conf=AutoridadesInstitucionales.lectorPrivado().readValue(Files.readAllBytes(path),Confianza.class);if(conf.claves()==null||conf.claves().isEmpty()||conf.claves().size()>8)throw new IllegalArgumentException();
            for(var key:conf.claves()){AutoridadesInstitucionales.codigo(key.id());if(trust.putIfAbsent(key.id(),ComprobanteDigital.publicPem(key.pem()))!=null)throw new IllegalArgumentException();}
        }catch(Exception e){throw new IllegalStateException("Confianza de autoridad inválida; no se habilitan designaciones");}
        return new AutoridadesInstitucionales(db,ids,json,Clock.systemUTC(),trust);
    }
    @Bean public ConcesionesInstitucionales concesionesInstitucionales(BaseJdbc db,IdentidadInstitucional ids,AutoridadesInstitucionales authority,JsonMapper json){return new ConcesionesInstitucionales(db,ids,authority,json,Clock.systemUTC());}
    @Bean @Order(100) public ApplicationRunner incorporarAutoridades(AutoridadesInstitucionales authority,JsonMapper json,Environment env){return args->{
        String file=env.getProperty("institucional.autoridad.sobre-archivo");if(file==null)return;
        try{Path path=ConfiguracionPrivada.ruta(env,file);ArchivoPrivado.validar(path,65536);authority.incorporar(AutoridadesInstitucionales.lectorPrivado().readValue(Files.readAllBytes(path),AutoridadesInstitucionales.Sobre.class));}
        catch(Exception e){throw new IllegalStateException("Registro firmado de autoridad no aceptado; revisar evidencia privada");}
    };}
}
