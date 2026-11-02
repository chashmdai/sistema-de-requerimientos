import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.*;
import java.time.*;
import javax.net.ssl.*;
import java.util.*;

/** Sonda TLS sin credenciales, cookies, base de datos, reclamos ni ACK. */
class VerificarHttpsUbuntu {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("CA y URL requeridas");
        var uri = URI.create(args[1]);
        if (!"https".equals(uri.getScheme()) || uri.getPort() != 25443
                || !Set.of("formulario.defensorianinez.gob.cl", "127.0.0.1").contains(uri.getHost())
                || !"/api/session".equals(uri.getPath()) || uri.getQuery() != null || uri.getUserInfo() != null
                || uri.getFragment() != null
                || Arrays.stream(InetAddress.getAllByName(uri.getHost())).anyMatch(a -> !a.isLoopbackAddress()))
            throw new IllegalArgumentException("Solo sonda U4 loopback");
        var builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER);
        if (!"SYSTEM".equals(args[0])) {
            X509Certificate cert;
            try (var in = Files.newInputStream(Path.of(args[0]))) {
                cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
            }
            cert.checkValidity();
            if (cert.getBasicConstraints() < 0) throw new IllegalArgumentException("CA requerida");
            var store = KeyStore.getInstance(KeyStore.getDefaultType()); store.load(null, null);
            store.setCertificateEntry("ddn-ubuntu", cert);
            var tm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); tm.init(store);
            var ssl = SSLContext.getInstance("TLS"); ssl.init(null, tm.getTrustManagers(), null); builder.sslContext(ssl);
        }
        try (var client = builder.build()) {
            var response = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.discarding());
            var ssl = response.sslSession().orElseThrow();
            var cert = (X509Certificate) ssl.getPeerCertificates()[0];
            System.out.println("HTTP=" + response.statusCode() + " TLS=" + ssl.getProtocol() + " cipher=" + ssl.getCipherSuite());
            System.out.println("leafSHA256=" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded())));
            System.out.println("subject=" + cert.getSubjectX500Principal() + " SAN=" + cert.getSubjectAlternativeNames());
            if (response.statusCode() != 200) System.exit(2);
        } catch (SSLException invalid) {
            System.out.println("TLS_RECHAZADO"); System.exit(3);
        }
    }
}
