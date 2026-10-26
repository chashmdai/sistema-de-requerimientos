package cl.nexbyte.siger.institucional.identidad;
import cl.nexbyte.siger.institucional.nucleo.*;import cl.nexbyte.siger.institucional.robustez.*;
import org.springframework.context.annotation.*;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.nio.file.*;import java.nio.file.attribute.*;import java.time.*;import java.util.*;
@Configuration @ConditionalOnProperty(name="institucional.identidad.habilitada",havingValue="true")
public class IdentidadConfiguration {
 @Bean IdentidadInstitucional identidadInstitucional(BaseJdbc db,HabilitacionNucleo gate){if(!gate.habilitado())throw new IllegalStateException("Identidad cerrada");return new IdentidadInstitucional(db,Clock.systemUTC());}
 @Bean OrganizacionInstitucional organizacionInstitucional(BaseJdbc db,IdentidadInstitucional identidad){return new OrganizacionInstitucional(db,identidad,Clock.systemUTC());}
 @Bean org.springframework.boot.ApplicationRunner bootstrapIdentidad(IdentidadInstitucional identidad,org.springframework.core.env.Environment env){return args->{String configured=env.getProperty("institucional.identidad.bootstrap.archivo");if(configured==null)return;
  Path p=cl.nexbyte.siger.institucional.archivos.ConfiguracionPrivada.ruta(env,configured);
  if(Files.notExists(p)&&Files.exists(p.resolveSibling(p.getFileName()+".aplicado"),LinkOption.NOFOLLOW_LINKS)){cl.nexbyte.siger.institucional.archivos.ArchivoPrivado.validar(p.resolveSibling(p.getFileName()+".aplicado"),8192);return;}
  cl.nexbyte.siger.institucional.archivos.ArchivoPrivado.validar(p,8192);
  var c=tools.jackson.databind.json.JsonMapper.builder().build().readValue(Files.readAllBytes(p),IdentidadInstitucional.Crear.class);
  identidad.bootstrap(c);Files.move(p,p.resolveSibling(p.getFileName()+".aplicado"),StandardCopyOption.ATOMIC_MOVE);
 }; }
}
