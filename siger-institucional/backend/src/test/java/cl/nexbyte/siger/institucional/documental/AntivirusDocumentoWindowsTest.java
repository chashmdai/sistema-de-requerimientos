package cl.nexbyte.siger.institucional.documental;

import cl.nexbyte.siger.institucional.archivos.ArchivoPrivado;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.condition.*;
import static org.junit.jupiter.api.Assertions.*;

/** Guardas sin ejecutar un parser falso como antivirus real. La prueba real se registra aparte. */
@EnabledOnOs(OS.WINDOWS)
class AntivirusDocumentoWindowsTest {
    @TempDir Path temp;
    Path tools,defs,jobs;Map<String,String> hashes;
    @BeforeEach void preparar()throws Exception {
        ArchivoPrivado.proteger(temp);
        tools=Files.createDirectory(temp.resolve("tools"));defs=Files.createDirectory(temp.resolve("defs"));jobs=Files.createDirectory(temp.resolve("jobs"));
        for(var p:List.of(tools,defs,jobs))ArchivoPrivado.proteger(p);
        var exe=tools.resolve("clamscan.exe");Files.writeString(exe,"ARTEFACTO_SINTETICO_NO_EJECUTABLE");ArchivoPrivado.proteger(exe);
        hashes=Map.of("clamscan.exe",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(exe))));
    }
    @Test void manifiestoCompletoRechazaContenidoExtraYAlteraciones()throws Exception {
        new AntivirusDocumentoWindows(tools,defs,jobs,hashes);
        var extra=tools.resolve("credencial.txt");Files.writeString(extra,"SINTETICO");ArchivoPrivado.proteger(extra);
        assertThrows(IOException.class,()->new AntivirusDocumentoWindows(tools,defs,jobs,hashes));
        Files.delete(extra);Files.writeString(tools.resolve("clamscan.exe"),"ALTERADO");
        assertThrows(IOException.class,()->new AntivirusDocumentoWindows(tools,defs,jobs,hashes));
    }
    @Test void raicesAnidadasYManifiestoIncompletoSeRechazan()throws Exception {
        assertThrows(IOException.class,()->new AntivirusDocumentoWindows(tools,defs,jobs,Map.of()));
        var nested=Files.createDirectory(tools.resolve("defs"));ArchivoPrivado.proteger(nested);
        assertThrows(IOException.class,()->new AntivirusDocumentoWindows(tools,nested,jobs,hashes));
    }
    @Test void cortePendienteBloqueaYNoSeBorraComoSupuestoLimpio()throws Exception {
        var stale=Files.createDirectory(jobs.resolve("av-"+UUID.randomUUID()));ArchivoPrivado.proteger(stale);
        Files.writeString(stale.resolve("pending"),"SINTETICO");
        assertThrows(IOException.class,()->new AntivirusDocumentoWindows(tools,defs,jobs,hashes));
        assertTrue(Files.exists(stale.resolve("pending")));
    }
    @Test void paqueteAlteradoDespuesDeConfigurarSeRechazaAntesDeCrearProceso()throws Exception {
        var av=new AntivirusDocumentoWindows(tools,defs,jobs,hashes);
        Files.writeString(tools.resolve("clamscan.exe"),"ALTERADO");
        var input=temp.resolve("input");Files.writeString(input,"SINTETICO");ArchivoPrivado.proteger(input);
        var rejected=assertThrows(IOException.class,()->av.limpio(input));
        assertTrue(rejected.getMessage().contains("manifiesto"),"Debe superar el mutex nativo y rechazar el paquete alterado");assertNull(av.ultimoResultado());
        try(var files=Files.list(jobs)){assertFalse(files.anyMatch(p->p.getFileName().toString().startsWith("av-")));}
    }
}
