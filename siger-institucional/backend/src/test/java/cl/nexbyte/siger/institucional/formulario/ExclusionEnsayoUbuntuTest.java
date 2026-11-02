package cl.nexbyte.siger.institucional.formulario;

import cl.nexbyte.siger.institucional.robustez.BaseJdbc;
import java.sql.*;
import javax.sql.DataSource;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Dobles JDBC: valida propiedad/pérdida/liberación; no acredita GET_LOCK real en MySQL. */
class ExclusionEnsayoUbuntuTest {
    BaseJdbc db; Connection connection; ResultSet identity, acquired, owned; PreparedStatement release;
    @BeforeEach void open() throws Exception {
        db = new BaseJdbc("jdbc:h2:mem:lock_" + UUID.randomUUID(), "sa", "", 2);
        var ds = mock(DataSource.class); connection = mock(Connection.class); when(ds.getConnection()).thenReturn(connection); db.sql.setDataSource(ds);
        var statement = mock(Statement.class); identity = mock(ResultSet.class);
        when(connection.createStatement()).thenReturn(statement); when(statement.executeQuery(anyString())).thenReturn(identity);
        when(identity.next()).thenReturn(true); when(identity.getString(1)).thenReturn("siger_u4_ddn_ubuntu"); when(identity.getString(2)).thenReturn("siger_u4_ubuntu_app@localhost");
        var get = mock(PreparedStatement.class); acquired = mock(ResultSet.class); when(acquired.next()).thenReturn(true); when(acquired.getInt(1)).thenReturn(1); when(get.executeQuery()).thenReturn(acquired);
        when(connection.prepareStatement("SELECT GET_LOCK(?,0)")).thenReturn(get);
        var check = mock(PreparedStatement.class); owned = mock(ResultSet.class); when(owned.next()).thenReturn(true); when(owned.getBoolean(1)).thenReturn(true); when(check.executeQuery()).thenReturn(owned);
        when(connection.prepareStatement("SELECT IS_USED_LOCK(?)=CONNECTION_ID()")).thenReturn(check);
        release = mock(PreparedStatement.class); when(connection.prepareStatement("SELECT RELEASE_LOCK(?)")).thenReturn(release);
    }
    @AfterEach void close() { db.close(); }
    @Test void requiereAmbosLocksYLosLiberaAlCerrar() throws Exception {
        try (var lock = new ExclusionEnsayoUbuntu(db)) { lock.verificar(); }
        verify(connection, times(2)).prepareStatement("SELECT GET_LOCK(?,0)");
        verify(connection, times(2)).prepareStatement("SELECT IS_USED_LOCK(?)=CONNECTION_ID()");
        verify(release, times(2)).execute(); verify(connection).close();
    }
    @Test void perdidaDeUnLockFallaCerradoSinReAdquirir() throws Exception {
        try (var lock = new ExclusionEnsayoUbuntu(db)) {
            when(owned.getBoolean(1)).thenReturn(false);
            assertThrows(IllegalStateException.class, lock::verificar);
            verify(connection, times(2)).prepareStatement("SELECT GET_LOCK(?,0)");
        }
    }
    @Test void rechazoDeBaseQaOContencionCierraConexionSinConsumir() throws Exception {
        when(identity.getString(1)).thenReturn("siger_institucional_windows_qa");
        assertThrows(IllegalStateException.class, () -> new ExclusionEnsayoUbuntu(db)); verify(connection).close();
        verify(connection, never()).prepareStatement("SELECT GET_LOCK(?,0)");
    }
    @Test void segundoLockOcupadoLiberaElPrimero() throws Exception {
        when(acquired.getInt(1)).thenReturn(1, 0);
        assertThrows(IllegalStateException.class, () -> new ExclusionEnsayoUbuntu(db));
        verify(release, times(2)).execute(); verify(connection).close();
    }
}
