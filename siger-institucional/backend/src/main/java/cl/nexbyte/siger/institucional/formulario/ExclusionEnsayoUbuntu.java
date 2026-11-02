package cl.nexbyte.siger.institucional.formulario;

import cl.nexbyte.siger.institucional.robustez.BaseJdbc;
import java.sql.*;
import java.util.*;

/** Exclusión de ensayo: base dedicada y origen lógico, también entre custodias distintas. */
public final class ExclusionEnsayoUbuntu implements AutoCloseable {
    private static final List<String> KEYS = List.of("siger:u4:db:siger_u4_ddn_ubuntu", "siger:u4:origin:DDN-UBUNTU");
    private final Connection connection;

    public ExclusionEnsayoUbuntu(BaseJdbc db) {
        Connection candidate = null;
        try {
            candidate = Objects.requireNonNull(db.sql.getDataSource()).getConnection();
            try (var s = candidate.createStatement()) {
                s.setQueryTimeout(5);
                try (var r = s.executeQuery("SELECT DATABASE(),CURRENT_USER()")) {
                    if (!r.next() || !"siger_u4_ddn_ubuntu".equals(r.getString(1)) || !"siger_u4_ubuntu_app@localhost".equals(r.getString(2))) throw new SQLException();
                }
            }
            for (var key : KEYS) try (var s = candidate.prepareStatement("SELECT GET_LOCK(?,0)")) {
                s.setQueryTimeout(5); s.setString(1, key);
                try (var r = s.executeQuery()) { if (!r.next() || r.getInt(1) != 1) throw new SQLException(); }
            }
            connection = candidate;
        } catch (Exception failure) {
            if (candidate != null) release(candidate);
            throw new IllegalStateException("Exclusividad U4 no disponible");
        }
    }

    public synchronized void verificar() {
        try {
            for (var key : KEYS) try (var s = connection.prepareStatement("SELECT IS_USED_LOCK(?)=CONNECTION_ID()")) {
                s.setQueryTimeout(5); s.setString(1, key);
                try (var r = s.executeQuery()) { if (!r.next() || !r.getBoolean(1)) throw new SQLException(); }
            }
        } catch (SQLException lost) { throw new IllegalStateException("Exclusividad U4 perdida"); }
    }

    private static void release(Connection connection) {
        for (var key : KEYS) try (var s = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            s.setQueryTimeout(5); s.setString(1, key); s.execute();
        } catch (SQLException ignored) { /* Nunca libera locks de otras conexiones. */ }
        try { connection.close(); } catch (SQLException ignored) { }
    }
    public synchronized void close() { release(connection); }
}
