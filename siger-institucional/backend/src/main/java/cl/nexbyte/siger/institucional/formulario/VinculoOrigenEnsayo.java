package cl.nexbyte.siger.institucional.formulario;

import cl.nexbyte.siger.institucional.robustez.BaseJdbc;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Un origen inmutable por base de ensayo; se invoca bajo exclusión MySQL antes de conciliar. */
public final class VinculoOrigenEnsayo {
    private VinculoOrigenEnsayo() {}

    public static String huella(String origin, String caHash, String publicTrustHash, String keyId,
                                String ownTrustHash, String archivo) {
        for (var hash : List.of(caHash, publicTrustHash, ownTrustHash))
            if (!hash.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Huella requerida");
        if (!keyId.matches("[A-Za-z0-9_-]{1,64}")) throw new IllegalArgumentException("KeyId inválido");
        // Cada componente se codifica con longitud: sin delimitadores ambiguos ni secretos.
        var tuple = new StringBuilder();
        for (var value : List.of("DDN-UBUNTU", origin, caHash, publicTrustHash, keyId, ownTrustHash, archivo))
            tuple.append(value.length()).append(':').append(value);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(tuple.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    public static void comprobar(BaseJdbc db, String fingerprint) {
        if (!fingerprint.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Vínculo inválido");
        db.transaction(() -> {
            var rows = db.sql.queryForList("SELECT origin_ref,binding_sha256 FROM consumer_origin_binding WHERE id=1 FOR UPDATE");
            if (rows.isEmpty()) {
                for (var table : List.of("institutional_inbox", "communication_registry", "institution_document"))
                    if (db.sql.queryForObject("SELECT COUNT(*) FROM " + table, Long.class) != 0)
                        throw new IllegalStateException("No vincular datos preexistentes a otro origen");
                db.sql.update("INSERT INTO consumer_origin_binding(id,origin_ref,binding_sha256,created_ms) VALUES(1,'DDN-UBUNTU',?,?)", fingerprint, System.currentTimeMillis());
            } else if (!"DDN-UBUNTU".equals(rows.getFirst().get("origin_ref")) || !fingerprint.equals(rows.getFirst().get("binding_sha256"))) {
                throw new IllegalStateException("Cambio de origen, confianza, firmante o custodia rechazado");
            }
            return null;
        });
    }
}
