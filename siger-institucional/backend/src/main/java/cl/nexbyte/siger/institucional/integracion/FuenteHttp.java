package cl.nexbyte.siger.institucional.integracion;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public final class FuenteHttp implements FuenteComunicaciones {
    public record Lote(List<SobreComunicacion> comunicaciones) {}
    private final RestClient client;
    public FuenteHttp(URI base, String token, boolean demo) {
        boolean loopback = java.util.Set.of("localhost", "127.0.0.1", "[::1]").contains(base.getHost());
        if (base.getHost() == null || base.getRawUserInfo() != null || base.getQuery() != null || base.getFragment() != null
                || !(base.getPath().isEmpty() || base.getPath().equals("/"))
                || !("https".equals(base.getScheme()) || (demo && loopback && "http".equals(base.getScheme())))
                || token == null || token.length() < 32 || token.length() > 400 || token.contains("\r") || token.contains("\n"))
            throw new IllegalArgumentException("Configuración del transporte inválida");
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(10));
        client = RestClient.builder().baseUrl(base.toString().replaceAll("/$", "")).requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + token).build();
    }
    public List<SobreComunicacion> recuperar() {
        var batch = client.get().uri("/integracion/v1/comunicaciones/pendientes?limite=20").retrieve().body(Lote.class);
        if (batch == null || batch.comunicaciones() == null || batch.comunicaciones().size() > 20)
            throw new IllegalStateException("Lote inválido");
        return List.copyOf(batch.comunicaciones());
    }
    public void confirmar(UUID id, SobreComunicacion.Confirmacion receipt) {
        var response = client.post().uri("/integracion/v1/comunicaciones/{id}/confirmacion", id).body(receipt).retrieve().toBodilessEntity();
        if (response.getStatusCode().value() != 204) throw new IllegalStateException("Confirmación no aceptada");
    }
}
