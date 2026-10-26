package cl.nexbyte.siger.institucional.formulario;
import java.nio.file.Path;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
/** Custodia primaria independiente de habilitar conexiones al Formulario Público. */
@Configuration @Profile("jdbc-reception") @ConditionalOnProperty(name="institucional.documentos.directorio")
public class CustodiaInstitucionalConfiguration {
    @Bean ArchivoInstitucional archivosInstitucionales(Environment e)throws java.io.IOException{return new ArchivoInstitucional(Path.of(e.getRequiredProperty("institucional.documentos.directorio")));}
}
