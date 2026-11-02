package cl.nexbyte.siger.institucional.windows;

import cl.nexbyte.siger.institucional.archivos.*;
import cl.nexbyte.siger.institucional.formulario.*;
import cl.nexbyte.siger.institucional.robustez.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Ejecución U4 acotada por stdin. Reutiliza el protocolo completo; no arranca Spring, web ni scheduler. */
public final class EnsayoFormularioUbuntu {
    private static final String ORIGIN = "https://formulario.defensorianinez.gob.cl:25443";
    private static final String SCHEMA = "siger_u4_ddn_ubuntu";
    private static final Set<String> KEYS = Set.of("accion", "origin", "ca", "caSHA256", "publicTrust", "publicTrustSHA256",
            "ownTrust", "keyId", "privateKey", "token", "archivo", "dbUrl", "dbUser", "dbPassword");
    private EnsayoFormularioUbuntu() {}

    static void validar(Map<String,String> p) throws Exception {
        if (!PlataformaArchivos.windows() || !KEYS.containsAll(p.keySet())
                || !Set.of("SONDA", "MIGRAR", "CICLO").contains(p.getOrDefault("accion", ""))) throw new IllegalArgumentException();
        if ("MIGRAR".equals(p.get("accion"))) { validarDb(p); return; }
        if (!ORIGIN.equals(p.get("origin"))
                || Arrays.stream(InetAddress.getAllByName(URI.create(ORIGIN).getHost())).anyMatch(a -> !a.isLoopbackAddress())) throw new IllegalArgumentException();
        if (!p.getOrDefault("archivo", "").matches("C:\\\\req\\\\testdata\\\\u4-ddn-ubuntu-[a-f0-9]{32}\\\\archivo")) throw new IllegalArgumentException();
        for (var pair : List.of(Map.entry("ca", "caSHA256"), Map.entry("publicTrust", "publicTrustSHA256"))) {
            var path = ArchivoPrivado.validar(Path.of(p.get(pair.getKey())), 65536);
            if (!hash(Files.readAllBytes(path)).equals(p.get(pair.getValue()))) throw new IllegalArgumentException();
        }
        ArchivoPrivado.validar(Path.of(p.get("ownTrust")), 65536);
        if (!p.getOrDefault("keyId", "").matches("[A-Za-z0-9_-]{1,64}") || !p.getOrDefault("token", "").matches("[A-Za-z0-9_-]{32,400}")) throw new IllegalArgumentException();
        if ("CICLO".equals(p.get("accion"))) validarDb(p);
    }

    static void validarDb(Map<String,String> p) {
        var url = p.get("dbUrl"); ConfiguracionDatos.validar(url, false);
        if (!url.startsWith("jdbc:mysql://127.0.0.1:3306/" + SCHEMA + "?")
                || !("MIGRAR".equals(p.get("accion")) ? "siger_u4_ubuntu_migrator" : "siger_u4_ubuntu_app").equals(p.get("dbUser"))
                || p.getOrDefault("dbPassword", "").isBlank()) throw new IllegalArgumentException();
    }

    private static String hash(byte[] bytes) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    private static Map<String,PublicKey> trust(Path path, JsonMapper json) throws Exception {
        var tree = json.readTree(Files.readString(path)); var keys = new HashMap<String,PublicKey>();
        if (!tree.isObject() || tree.isEmpty() || tree.size() > 20) throw new IllegalArgumentException();
        for (var entry : tree.properties()) {
            if (!entry.getKey().matches("[A-Za-z0-9_-]{1,64}") || !entry.getValue().isString()) throw new IllegalArgumentException();
            keys.put(entry.getKey(), ComprobanteDigital.publicPem(entry.getValue().asString()));
        }
        return keys;
    }

    private static BaseJdbc db(Map<String,String> p) {
        var db = new BaseJdbc(p.get("dbUrl"), p.get("dbUser"), p.get("dbPassword"), 4);
        try {
            if (!SCHEMA.equals(db.sql.queryForObject("SELECT DATABASE()", String.class))
                    || !(p.get("dbUser") + "@localhost").equals(db.sql.queryForObject("SELECT CURRENT_USER()", String.class))) throw new IllegalStateException();
            var allowed = "MIGRAR".equals(p.get("accion"))
                    ? Set.of("SELECT", "INSERT", "UPDATE", "DELETE", "CREATE", "ALTER", "DROP", "INDEX", "REFERENCES", "CREATE VIEW")
                    : Set.of("SELECT", "INSERT", "UPDATE", "DELETE");
            for (var grant : db.sql.queryForList("SHOW GRANTS", String.class)) {
                if (grant.startsWith("GRANT USAGE ON *.* TO ")) continue;
                int on = grant.indexOf(" ON ");
                if (on < 0 || !grant.substring(on).startsWith(" ON `" + SCHEMA + "`.* TO ")
                        || !allowed.containsAll(Arrays.asList(grant.substring(6, on).split(", ")))) throw new IllegalStateException();
            }
            return db;
        } catch (RuntimeException invalid) { db.close(); throw invalid; }
    }

    static Map<String,Object> ejecutar(Map<String,String> p) throws Exception {
        validar(p);
        if ("MIGRAR".equals(p.get("accion"))) {
            try (var db = db(p)) { db.migrate("institucional"); }
            return Map.of("migrado", true, "schema", SCHEMA, "claims", 0, "acksSent", 0);
        }
        var json = EntregaCompleta.lector();
        var publicKeys = trust(Path.of(p.get("publicTrust")), json); var own = trust(Path.of(p.get("ownTrust")), json);
        if (own.values().stream().anyMatch(publicKeys.values()::contains)) throw new IllegalArgumentException();
        var signer = ComprobanteDigital.privatePem(p.get("privateKey"));
        var proof = new ComprobanteInstitucional.Contenido("1.0", "SIGER_DDN", "COMUNICACION_REGISTRADA", UUID.randomUUID(), UUID.randomUUID(), 1, "COM-000001", Instant.now(), "FORMULARIO_WEB_PUBLICO", "0".repeat(64), "0".repeat(64));
        if (!proof.equals(ComprobanteInstitucional.firmar(proof, p.get("keyId"), signer, json).verificar(own, json))) throw new IllegalArgumentException();
        try (var source = new FuenteFormulario(URI.create(ORIGIN), p.get("token"), Path.of(p.get("ca")), false)) {
            var connection = source.comprobarConexion();
            if ("SONDA".equals(p.get("accion"))) return Map.of("connection", connection, "technicalAuthenticationReady", connection.preparada(), "institutionalSigningTrustedOffline", true, "claims", 0, "acksSent", 0);
            if (!connection.preparada()) throw new IllegalStateException("Origen aún cerrado o autenticación no acreditada");
            try (var db = db(p); var exclusion = new ExclusionEnsayoUbuntu(db)) {
                exclusion.verificar();
                var fingerprint = VinculoOrigenEnsayo.huella(ORIGIN, p.get("caSHA256"), p.get("publicTrustSHA256"), p.get("keyId"), hash(Files.readAllBytes(Path.of(p.get("ownTrust")))), p.get("archivo"));
                VinculoOrigenEnsayo.comprobar(db, fingerprint);
                var controls = new ControlesOperacion(db).estado();
                if (!controls.aceptarNuevos() || !controls.recuperar() || !"NORMAL".equals(controls.contingencia())) throw new IllegalStateException("Ensayo local cerrado");
                try (var files = new ArchivoInstitucional(Path.of(p.get("archivo")))) {
                    var receiver = new RecepcionInstitucionalJdbc(db, json, publicKeys, p.get("keyId"), signer, own, "COM", 6, Clock.systemUTC());
                    var recovery = new RecuperarFormulario(source, receiver, files, exclusion::verificar);
                    int reconciled = recovery.conciliar(); var result = recovery.ejecutar();
                    return Map.of("resultado", result, "conciliadas", reconciled, "schema", SCHEMA, "origin", ORIGIN,
                            "ackPending", db.sql.queryForObject("SELECT COUNT(*) FROM institutional_inbox WHERE ack_pending=TRUE", Long.class));
                }
            }
        }
    }

    public static void main(String[] args) {
        // Sin mensajes de bibliotecas que puedan contener configuración privada.
        System.setProperty("logging.level.root", "OFF");
        if (org.slf4j.LoggerFactory.getILoggerFactory() instanceof ch.qos.logback.classic.LoggerContext context) context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).setLevel(ch.qos.logback.classic.Level.OFF);
        var p = new HashMap<String,String>(); byte[] bytes = null; int exit = 0;
        try {
            if (args.length != 0) throw new IllegalArgumentException();
            bytes = System.in.readNBytes(65537); if (bytes.length > 65536) throw new IllegalArgumentException();
            var factory = tools.jackson.core.json.JsonFactory.builder().enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
            var tree = JsonMapper.builder(factory).build().readTree(bytes);
            if (!tree.isObject()) throw new IllegalArgumentException();
            for (var entry : tree.properties()) { if (!entry.getValue().isString()) throw new IllegalArgumentException(); p.put(entry.getKey(), entry.getValue().asString()); }
            System.out.println(EntregaCompleta.lector().writeValueAsString(ejecutar(p)));
        } catch (Exception failure) {
            System.out.println("{\"ok\":false,\"code\":\"ENSAYO_U4_RECHAZADO\"}"); exit = 1;
        } finally { if (bytes != null) Arrays.fill(bytes, (byte) 0); p.clear(); }
        if (exit != 0) System.exit(exit);
    }
}
