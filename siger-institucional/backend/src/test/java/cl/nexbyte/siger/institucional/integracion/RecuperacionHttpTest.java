package cl.nexbyte.siger.institucional.integracion;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

/** Proveedor público simulado SOLO dentro de pruebas. El adaptador HTTP y la recepción local son reales. */
class RecuperacionHttpTest {
    @TempDir Path directory;
    static final JsonMapper MAPPER = JsonMapper.builder().build();
    static SobreComunicacion sobre() {
        return SobreComunicacion.crear(UUID.fromString("11111111-1111-4111-8111-111111111111"), Instant.parse("2026-10-14T12:00:00Z"),
                new SobreComunicacion.Identidad("44444444-4", "Ciudadana", "Prueba"), "Mensaje de prueba", "demo-local-v1");
    }
    @Test void demoHttpIndisponibilidadReintentoConfirmacionPerdidaReinicioYDeduplicacion() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var pending = new AtomicBoolean(true);
        var unavailable = new AtomicBoolean(true);
        var loseFirstAck = new AtomicBoolean(true);
        var ackCalls = new AtomicInteger();
        var ackBody = new AtomicReference<SobreComunicacion.Confirmacion>();
        server.createContext("/integracion/v1/comunicaciones", exchange -> {
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer test-only-machine-token-01234567890123456789");
            int status;
            byte[] body = new byte[0];
            if (unavailable.get()) status = 503;
            else if (exchange.getRequestMethod().equals("GET")) {
                status = 200;
                body = MAPPER.writeValueAsBytes(new FuenteHttp.Lote(pending.get() ? List.of(sobre()) : List.of()));
            } else {
                assertThat(exchange.getRequestURI().getPath()).isEqualTo("/integracion/v1/comunicaciones/" + sobre().id() + "/confirmacion");
                ackCalls.incrementAndGet();
                var receipt = MAPPER.readValue(exchange.getRequestBody().readAllBytes(), SobreComunicacion.Confirmacion.class);
                // La confirmación solo puede ocurrir después de que el archivo institucional exista.
                assertThat(java.nio.file.Files.exists(directory.resolve(sobre().id() + ".json"))).isTrue();
                if (ackBody.get() != null) assertThat(receipt).isEqualTo(ackBody.get());
                ackBody.set(receipt);
                if (loseFirstAck.getAndSet(false)) status = 503;
                else { pending.set(false); status = 204; }
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            if (body.length > 0) exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            var source = new FuenteHttp(URI.create("http://127.0.0.1:" + server.getAddress().getPort()), "test-only-machine-token-01234567890123456789", true);
            try (var store = new RecepcionInstitucionalArchivosDemo(directory, MAPPER)) {
                var service = new RecuperarComunicaciones(source, store);
                assertThatThrownBy(service::ejecutar).isInstanceOf(RuntimeException.class);
                assertThat(ackCalls.get()).isZero();
                unavailable.set(false);
                assertThat(service.ejecutar()).isEqualTo(new RecuperarComunicaciones.Resultado(1, 0, 1));
                assertThat(pending.get()).isTrue();
            }
            try (var store = new RecepcionInstitucionalArchivosDemo(directory, MAPPER)) {
                var service = new RecuperarComunicaciones(source, store);
                assertThat(service.ejecutar()).isEqualTo(new RecuperarComunicaciones.Resultado(1, 1, 0));
                assertThat(service.ejecutar()).isEqualTo(new RecuperarComunicaciones.Resultado(0, 0, 0));
                assertThat(store.guardarSiAusente(sobre())).isEqualTo(ackBody.get());
                try (var files = java.nio.file.Files.list(directory)) { assertThat(files.filter(p -> p.toString().endsWith(".json")).count()).isEqualTo(1); }
                var modified = SobreComunicacion.crear(sobre().id(), sobre().recibidaEn(), sobre().identidad(), "Otro mensaje", "demo-local-v1");
                assertThatThrownBy(() -> store.guardarSiAusente(modified)).isInstanceOf(IllegalArgumentException.class);
            }
        } finally { server.stop(0); }
    }
    @Test void almacenamientoFallidoNuncaConfirmaYRegistroAlteradoNoSeRecibe() {
        var acknowledgments = new AtomicInteger();
        var source = new FuenteComunicaciones() {
            public List<SobreComunicacion> recuperar() { return List.of(sobre()); }
            public void confirmar(UUID id, SobreComunicacion.Confirmacion receipt) { acknowledgments.incrementAndGet(); }
        };
        var service = new RecuperarComunicaciones(source, value -> { throw new IllegalStateException("disco no disponible"); });
        assertThat(service.ejecutar().fallidas()).isEqualTo(1);
        assertThat(acknowledgments.get()).isZero();
        var valid = sobre();
        var tampered = new SobreComunicacion(valid.version(), valid.id(), valid.recibidaEn(), valid.identidad(), "Alterado", valid.avisoPrivacidadVersion(), valid.sha256());
        assertThat(tampered.integra()).isFalse();
    }
    @Test void rechazaHttpRemotoCredencialesDebilesYRutasConSecretosYNoSigueRedirecciones() throws Exception {
        assertThatThrownBy(() -> new FuenteHttp(URI.create("http://remote.example"), "test-only-machine-token-01234567890123456789", true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FuenteHttp(URI.create("https://public.example"), "weak", false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FuenteHttp(URI.create("https://user:secret@public.example"), "test-only-machine-token-01234567890123456789", false)).isInstanceOf(IllegalArgumentException.class);
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var leaked = new AtomicInteger();
        server.createContext("/integracion", x -> { x.getResponseHeaders().set("Location", "/unexpected"); x.sendResponseHeaders(302,-1); x.close(); });
        server.createContext("/unexpected", x -> { leaked.incrementAndGet(); x.sendResponseHeaders(200,-1); x.close(); });
        server.start();
        try {
            var source = new FuenteHttp(URI.create("http://127.0.0.1:" + server.getAddress().getPort()), "test-only-machine-token-01234567890123456789", true);
            assertThatThrownBy(source::recuperar).isInstanceOf(RuntimeException.class);
            assertThat(leaked.get()).isZero();
        } finally { server.stop(0); }
    }
}
