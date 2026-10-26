package cl.nexbyte.siger.institucional.documental;

import cl.nexbyte.siger.institucional.archivos.ArchivoPrivado;
import com.sun.jna.platform.win32.*;
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
    @Test void permisoTemporalSePropagaYSeRetiraDeHijosExistentes()throws Exception {
        var nested=Files.createDirectory(tools.resolve("nested"));
        var file=Files.writeString(nested.resolve("readonly.dll"),"SINTETICO");
        var token=new WinNT.HANDLEByReference();assertTrue(Advapi32.INSTANCE.OpenProcessToken(Kernel32.INSTANCE.GetCurrentProcess(),WinNT.TOKEN_QUERY,token));
        String user;try{user=Advapi32Util.getTokenAccount(token.getValue()).sidString;}finally{Kernel32.INSTANCE.CloseHandle(token.getValue());}
        // SID sintético sin perfil/proceso asociado; el test comprueba herencia NTFS real.
        String sid="S-1-15-2-7192201-1288223-1120133-555201-7713221-88811-99103";
        try(var saved=Advapi32Util.getSecurityDescriptorForObject(tools.toString(),1,false)){
            try{AntivirusDocumentoWindows.grant(tools,user,sid,"OICI","GRGX");
                assertTrue(Arrays.stream(Advapi32Util.getFileSecurity(file.toString(),false)).anyMatch(a->a instanceof WinNT.ACCESS_ALLOWED_ACE ace&&ace.getSidString().equals(sid)));
            }finally{boolean protectedAcl=(saved.getShort(2)&0x1000)!=0;AntivirusDocumentoWindows.aplicarDescriptor(tools,saved,(protectedAcl?0x80000000:0x20000000)|4);}
        }
        ArchivoPrivado.validarAcceso(tools);ArchivoPrivado.validarAcceso(nested);ArchivoPrivado.validarAcceso(file);
        assertFalse(Arrays.stream(Advapi32Util.getFileSecurity(file.toString(),false)).anyMatch(a->a instanceof WinNT.ACCESS_ALLOWED_ACE ace&&ace.getSidString().equals(sid)));
    }
}
