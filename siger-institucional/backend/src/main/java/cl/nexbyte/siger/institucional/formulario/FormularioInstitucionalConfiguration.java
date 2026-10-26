package cl.nexbyte.siger.institucional.formulario;
import cl.nexbyte.siger.institucional.robustez.*;import java.nio.file.Path;import java.net.URI;import org.springframework.context.annotation.*;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.core.env.Environment;import org.springframework.scheduling.annotation.Scheduled;
@Configuration @Profile("jdbc-reception") @ConditionalOnProperty(name="institucional.formulario-completo.habilitado",havingValue="true")
public class FormularioInstitucionalConfiguration {
    @Bean FuenteFormulario fuenteFormulario(Environment e)throws java.io.IOException{var ca=e.getProperty("institucional.formulario-completo.ca-certificado","");return new FuenteFormulario(URI.create(e.getRequiredProperty("institucional.formulario-completo.origen-publico")),e.getRequiredProperty("institucional.formulario-completo.token"),ca.isBlank()?null:Path.of(ca),false);}
    @Bean RecuperarFormulario recuperarFormulario(FuenteFormulario s,RecepcionInstitucionalJdbc r,ArchivoInstitucional a){return new RecuperarFormulario(s,r,a);}
    @Bean @ConditionalOnProperty(name="institucional.formulario-completo.automatico",havingValue="true") Planificador planificadorFormulario(RecuperarFormulario r,ControlesOperacion c){return new Planificador(r,c);}
    static final class Planificador{
        private final RecuperarFormulario receiver;private final ControlesOperacion control;private long next;private int failures;
        Planificador(RecuperarFormulario r,ControlesOperacion c){receiver=r;control=c;}
        @Scheduled(fixedDelayString="${institucional.formulario-completo.intervalo-ms:5000}")public void tick(){if(System.currentTimeMillis()<next)return;try{var c=control.estado();if(!c.recuperar()||!c.contingencia().equals("NORMAL"))return;receiver.conciliar();var r=receiver.ejecutar();if(r.fallidas()>0)delay();else failures=0;}catch(RuntimeException e){delay();}}
        private void delay(){failures=Math.min(6,failures+1);next=System.currentTimeMillis()+Math.min(300000,5000L<<failures);}
    }
}
