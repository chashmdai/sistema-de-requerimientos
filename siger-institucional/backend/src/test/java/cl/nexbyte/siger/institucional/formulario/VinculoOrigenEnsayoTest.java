package cl.nexbyte.siger.institucional.formulario;

import cl.nexbyte.siger.institucional.robustez.BaseJdbc;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class VinculoOrigenEnsayoTest {
    BaseJdbc db;
    @BeforeEach void open() { db = new BaseJdbc("jdbc:h2:mem:u4_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "", 2); db.migrate("institucional"); }
    @AfterEach void close() { db.close(); }
    static String fingerprint(String origin, String ca, String trust, String signer, String own, String files) {
        return VinculoOrigenEnsayo.huella(origin, ca.repeat(64), trust.repeat(64), signer, own.repeat(64), files);
    }
    static String original() { return fingerprint("https://formulario.example:25443", "a", "b", "siger-real", "c", "almacen-u4"); }
    @Test void vinculoPersisteYReinicioConservaIdentidad() {
        VinculoOrigenEnsayo.comprobar(db, original()); VinculoOrigenEnsayo.comprobar(db, original());
        assertEquals(1, db.sql.queryForObject("SELECT COUNT(*) FROM consumer_origin_binding", Integer.class));
    }
    @Test void rechazaCambioDeOrigenCaTrustFirmanteOAlmacenAntesDeConciliar() {
        VinculoOrigenEnsayo.comprobar(db, original());
        for (var changed : List.of(fingerprint("https://otro.example", "a", "b", "siger-real", "c", "almacen-u4"),
                fingerprint("https://formulario.example:25443", "d", "b", "siger-real", "c", "almacen-u4"),
                fingerprint("https://formulario.example:25443", "a", "d", "siger-real", "c", "almacen-u4"),
                fingerprint("https://formulario.example:25443", "a", "b", "otro", "c", "almacen-u4"),
                fingerprint("https://formulario.example:25443", "a", "b", "siger-real", "d", "almacen-u4"),
                fingerprint("https://formulario.example:25443", "a", "b", "siger-real", "c", "almacen-wsl")))
            assertThrows(IllegalStateException.class, () -> VinculoOrigenEnsayo.comprobar(db, changed));
        assertEquals(original(), db.sql.queryForObject("SELECT binding_sha256 FROM consumer_origin_binding", String.class));
    }
    @Test void noAdoptaInboxAnteriorAunqueNoExistanPendientes() {
        db.sql.update("INSERT INTO institutional_inbox(source,id,content_hash,body,receipt,ack_pending,created_ms) VALUES('FORMULARIO_PUBLICO_DDN',? ,?,'{}','{}',FALSE,0)", UUID.randomUUID().toString(), "a".repeat(64));
        assertThrows(IllegalStateException.class, () -> VinculoOrigenEnsayo.comprobar(db, original()));
        assertEquals(0, db.sql.queryForObject("SELECT COUNT(*) FROM consumer_origin_binding", Integer.class));
        assertEquals(1, db.sql.queryForObject("SELECT COUNT(*) FROM institutional_inbox", Integer.class));
    }
}
