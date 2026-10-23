package cl.nexbyte.siger.institucional.integracion;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@Profile("local-integration-demo")
@ConditionalOnProperty(name="institucional.integracion.habilitada", havingValue="true")
public class IntegracionDemoConfiguration {
    @Bean RecepcionInstitucionalArchivosDemo recepcion(Environment env, JsonMapper mapper) throws java.io.IOException {
        if (!"127.0.0.1".equals(env.getProperty("server.address", "127.0.0.1")))
            throw new IllegalStateException("La demostración institucional requiere loopback");
        var directory = env.getRequiredProperty("institucional.demo.directorio");
        if (directory.isBlank() || !Path.of(directory).isAbsolute()) throw new IllegalStateException("Directorio de demostración absoluto requerido");
        return new RecepcionInstitucionalArchivosDemo(Path.of(directory), mapper);
    }
    @Bean FuenteHttp fuente(Environment env) {
        return new FuenteHttp(URI.create(env.getRequiredProperty("institucional.integracion.origen-publico")),
                env.getRequiredProperty("institucional.integracion.token"), true);
    }
    @Bean RecuperarComunicaciones recuperar(FuenteHttp fuente, RecepcionInstitucional recepcion) { return new RecuperarComunicaciones(fuente, recepcion); }
    @Bean Planificador planificador(RecuperarComunicaciones recuperar) { return new Planificador(recuperar); }

    static final class Planificador {
        private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Planificador.class);
        private final RecuperarComunicaciones recuperar;
        private int fallos;
        private Instant siguiente = Instant.EPOCH;
        Planificador(RecuperarComunicaciones recuperar) { this.recuperar = recuperar; }
        @Scheduled(fixedDelay=5000)
        public void tick() {
            if (Instant.now().isBefore(siguiente)) return;
            try {
                var result = recuperar.ejecutar();
                if (result.fallidas() > 0) { diferir(); LOG.warn("DEMO_ENTREGA fallidas={} reintentoDiferido=true", result.fallidas()); }
                else { fallos = 0; if (result.confirmadas() > 0) LOG.info("DEMO_ENTREGA confirmadas={}", result.confirmadas()); }
            } catch (RuntimeException e) { diferir(); LOG.warn("DEMO_FUENTE_NO_DISPONIBLE reintentoDiferido=true"); }
        }
        private void diferir() {
            fallos = Math.min(7, fallos + 1);
            long seconds = Math.min(300, 5L << (fallos - 1));
            siguiente = Instant.now().plusSeconds(seconds + java.util.concurrent.ThreadLocalRandom.current().nextLong(1, 6));
        }
    }
}
