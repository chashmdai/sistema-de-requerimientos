package cl.nexbyte.siger.institucional.integracion;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

/** Repositorio institucional simulado y durable localmente. No es la persistencia productiva. */
public final class RecepcionInstitucionalArchivosDemo implements RecepcionInstitucional, AutoCloseable {
    public record Registro(SobreComunicacion comunicacion, SobreComunicacion.Confirmacion confirmacion) {}
    private final ArchivosDemo files;
    public RecepcionInstitucionalArchivosDemo(Path path, JsonMapper mapper) throws IOException { files = new ArchivosDemo(path, mapper); }
    public synchronized SobreComunicacion.Confirmacion guardarSiAusente(SobreComunicacion sobre) {
        if (!sobre.integra()) throw new IllegalArgumentException("Integridad del contrato inválida");
        try {
            var name = sobre.id() + ".json";
            if (files.exists(name)) {
                var previous = files.read(name, Registro.class);
                if (!previous.comunicacion().integra() || !previous.comunicacion().sha256().equals(sobre.sha256()))
                    throw new IllegalArgumentException("Conflicto de idempotencia");
                return previous.confirmacion();
            }
            var receipt = new SobreComunicacion.Confirmacion(UUID.randomUUID(), Instant.now(), sobre.sha256());
            files.write(name, new Registro(sobre, receipt));
            return receipt;
        } catch (IOException | tools.jackson.core.JacksonException e) { throw new IllegalStateException("Persistencia local no disponible"); }
    }
    public void close() throws IOException { files.close(); }
}
