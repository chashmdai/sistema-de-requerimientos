package cl.nexbyte.siger.institucional.solicitudes;
import cl.nexbyte.siger.institucional.nucleo.*;
import cl.nexbyte.siger.institucional.notificaciones.RecursosNotificacion;
import cl.nexbyte.siger.institucional.robustez.BaseJdbc;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import tools.jackson.databind.json.JsonMapper;
import java.time.Clock;
@Configuration @ConditionalOnProperty(name="institucional.solicitudes.habilitadas",havingValue="true")
public class SolicitudesConfiguration {
 @Bean SolicitudesInstitucionales solicitudesInstitucionales(BaseJdbc db,RecursosNotificacion permisos,CatalogoLaboratorio cat,JsonMapper json){if(!cat.individual())throw new IllegalStateException("Solicitudes requieren identidad individual");return new SolicitudesInstitucionales(db,permisos,cat,json,Clock.systemUTC());}
}
