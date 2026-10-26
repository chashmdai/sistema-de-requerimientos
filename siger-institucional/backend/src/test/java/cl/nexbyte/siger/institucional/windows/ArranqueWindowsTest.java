package cl.nexbyte.siger.institucional.windows;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import static org.junit.jupiter.api.Assertions.*;
@EnabledOnOs(OS.WINDOWS)
class ArranqueWindowsTest {
    static Map<String,String> base(){var p=new HashMap<String,String>();p.put("siger.windows.modo","PRELIMINAR_SIN_DOCUMENTAL");p.put("institucional.db.url","jdbc:mysql://127.0.0.1:3306/siger_institucional_windows_qa?sslMode=VERIFY_IDENTITY");p.put("institucional.db.user","siger_windows_qa_app");p.put("institucional.db.password","valor-sintetico-no-credencial");p.put("server.ssl.key-store","file:/fixture.p12");p.put("server.ssl.key-store-password","valor-sintetico");return p;}
    @Test void recibeConfiguracionSinHabilitarCorreoORecuperacion(){var p=ArranqueWindows.configurar(base());for(String key:List.of("institucional.notificaciones.correo.automatico","institucional.documental.habilitada","institucional.formulario-completo.habilitado","institucional.v2.habilitada","institucional.integracion.habilitada","spring.flyway.enabled"))assertEquals("false",p.get(key));assertEquals("127.0.0.1",p.get("server.address"));assertEquals("true",p.get("server.ssl.enabled"));assertEquals("SIMULADO",p.get("institucional.notificaciones.correo.modo"));}
    @Test void rechazaHistoricoMigradorOtroPuertoYDesactivarTLS(){for(var entry:List.of(Map.entry("institucional.db.url",base().get("institucional.db.url").replace("windows_qa","windows_lab")),Map.entry("institucional.db.url",base().get("institucional.db.url").replace("VERIFY_IDENTITY","DISABLED")),Map.entry("institucional.db.user","siger_windows_qa_migrator"),Map.entry("server.port","443"),Map.entry("siger.windows.modo","COMPLETO"))){var p=base();p.put(entry.getKey(),entry.getValue());assertThrows(IllegalArgumentException.class,()->ArranqueWindows.configurar(p));}}
    @Test void rechazaPropiedadesQueActivenTransportesOCambienPerfiles(){for(String key:List.of("spring.profiles.active","server.address","institucional.formulario-completo.automatico","institucional.notificaciones.correo.modo","institucional.identidad.bootstrap.archivo")){var p=base();p.put(key,"no-admitido");assertThrows(IllegalArgumentException.class,()->ArranqueWindows.configurar(p));}}
}
