package cl.nexbyte.siger.institucional.windows;

import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import static org.junit.jupiter.api.Assertions.*;

class EnsayoFormularioUbuntuTest {
    static Map<String,String> migration() { return new HashMap<>(Map.of("accion", "MIGRAR", "dbUrl", "jdbc:mysql://127.0.0.1:3306/siger_u4_ddn_ubuntu?sslMode=VERIFY_IDENTITY", "dbUser", "siger_u4_ubuntu_migrator", "dbPassword", "sintetico")); }
    @Test void nuncaAceptaQaHistoricoRootTlsDebilODestinoRemoto() {
        for (var change : List.of(Map.entry("dbUrl", migration().get("dbUrl").replace("siger_u4_ddn_ubuntu", "siger_institucional_windows_qa")),
                Map.entry("dbUrl", migration().get("dbUrl").replace("siger_u4_ddn_ubuntu", "siger_institucional_windows_lab")),
                Map.entry("dbUrl", migration().get("dbUrl").replace("VERIFY_IDENTITY", "DISABLED")),
                Map.entry("dbUrl", migration().get("dbUrl").replace("127.0.0.1", "192.168.100.14")),
                Map.entry("dbUser", "root"), Map.entry("dbUser", "siger_windows_qa_app"))) {
            var p = migration(); p.put(change.getKey(), change.getValue());
            assertThrows(IllegalArgumentException.class, () -> EnsayoFormularioUbuntu.validarDb(p));
        }
    }
    @Test void appYMigradorNoSonIntercambiables() {
        assertDoesNotThrow(() -> EnsayoFormularioUbuntu.validarDb(migration()));
        var p = migration(); p.put("accion", "CICLO");
        assertThrows(IllegalArgumentException.class, () -> EnsayoFormularioUbuntu.validarDb(p));
        p.put("dbUser", "siger_u4_ubuntu_app"); assertDoesNotThrow(() -> EnsayoFormularioUbuntu.validarDb(p));
    }
    @Test @EnabledOnOs(OS.WINDOWS) void rechazaPropiedadesAdicionalesYAccionesAntesDeLeerSecretos() {
        var p = migration(); p.put("server.address", "0.0.0.0");
        assertThrows(IllegalArgumentException.class, () -> EnsayoFormularioUbuntu.validar(p));
        p.remove("server.address"); p.put("accion", "AUTOMATICO");
        assertThrows(IllegalArgumentException.class, () -> EnsayoFormularioUbuntu.validar(p));
    }
}
