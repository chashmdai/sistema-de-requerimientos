package cl.nexbyte.siger.institucional.nucleo;
import java.time.Clock;
import org.springframework.context.annotation.*;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import cl.nexbyte.siger.institucional.robustez.*;import tools.jackson.databind.json.JsonMapper;
@Configuration @ConditionalOnProperty(name="institucional.nucleo.habilitado",havingValue="true")
public class NucleoConfiguration {
    @Bean CatalogoLaboratorio catalogoLaboratorio(BaseJdbc db,HabilitacionNucleo gate){
        if(!gate.habilitado())throw new IllegalStateException("Núcleo cerrado");var c=new CatalogoLaboratorio();c.instalar(db);return c;
    }
    @Bean NucleoInstitucional nucleoInstitucional(BaseJdbc db,CatalogoLaboratorio catalogo,RecepcionInstitucionalJdbc recepcion,JsonMapper mapper,org.springframework.core.env.Environment env){return new NucleoInstitucional(db,catalogo,recepcion,mapper,Clock.systemUTC(),env.getProperty("institucional.folio.prefix","COM"),env.getProperty("institucional.folio.width",Integer.class,6));}
}
