package cl.nexbyte.siger.institucional.nucleo;
import org.springframework.stereotype.Component;import org.springframework.core.env.Environment;
/** El acceso sintético nunca habilita un ambiente institucional o productivo. */
@Component
public final class HabilitacionNucleo {
    private final boolean enabled;
    public HabilitacionNucleo(Environment env){
        enabled=env.getProperty("institucional.nucleo.habilitado",Boolean.class,false);
        if(enabled&&(!env.matchesProfiles("nucleo-lab")||!env.matchesProfiles("jdbc-reception")||env.matchesProfiles("prod","production")
            ||!"LABORATORIO".equals(env.getProperty("institucional.nucleo.entorno"))||!"127.0.0.1".equals(env.getProperty("server.address"))
            ||!env.getProperty("datos.mysql-local",Boolean.class,false)||!env.getProperty("server.servlet.session.cookie.secure",Boolean.class,true)
            ||!env.getProperty("institucional.db.url","").matches("jdbc:mysql://127\\.0\\.0\\.1:13306/robust_inst_(local|test)\\?.*")))
            throw new IllegalStateException("Núcleo provisional: identidades sintéticas solo en laboratorio MySQL loopback, perfil explícito y cookie Secure");
    }
    public boolean habilitado(){return enabled;}
}
