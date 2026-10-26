import cl.nexbyte.siger.institucional.archivos.ArchivoPrivado;
import cl.nexbyte.siger.institucional.documental.AntivirusDocumentoWindows;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Dos JVM y raíces distintas. Solo paquete sintético NO ejecutable; no inicia clamscan. */
class MutexAntivirusWindows {
    static void validar(Path root)throws Exception{if(!root.toString().matches("C:\\\\req\\\\testdata\\\\av-mutex-[a-f0-9]{32}"))throw new IOException("Solo QA");ArchivoPrivado.validarAcceso(root);}
    static AntivirusDocumentoWindows adapter(Path root,String name)throws Exception{
        var tools=root.resolve("tools");var defs=root.resolve("defs");var jobs=root.resolve(name);
        for(var p:List.of(tools,defs,jobs)){Files.createDirectories(p);ArchivoPrivado.proteger(p);}
        var exe=tools.resolve("clamscan.exe");if(!Files.exists(exe))Files.writeString(exe,"NO_EJECUTABLE_SINTETICO");ArchivoPrivado.proteger(exe);
        var hashes=Map.of("clamscan.exe",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(exe))));
        return new AntivirusDocumentoWindows(tools,defs,jobs,hashes);
    }
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]).toAbsolutePath().normalize();validar(root);
        var input=root.resolve("input");
        if(args.length==2){var av=adapter(root,args[1]);try{av.limpio(input);throw new AssertionError("No rechazo mutex");}catch(IOException e){if(!e.getMessage().contains("ocupado"))throw new AssertionError("Rechazo distinto de mutex");}if(av.ultimoResultado()!=null)throw new AssertionError("Scanner no autorizado");System.out.println("OCUPADO_SIN_SCANNER");return;}
        Files.writeString(input,"SINTETICO");ArchivoPrivado.proteger(input);adapter(root,"seed");
        var token=new WinNT.HANDLEByReference();if(!Advapi32.INSTANCE.OpenProcessToken(Kernel32.INSTANCE.GetCurrentProcess(),WinNT.TOKEN_QUERY,token))throw new IOException("Token");
        String sid;try{sid=Advapi32Util.getTokenAccount(token.getValue()).sidString;}finally{Kernel32.INSTANCE.CloseHandle(token.getValue());}
        var mutex=AntivirusDocumentoWindows.Api.K.CreateMutexW(null,false,new WString("Local\\Nexbyte.Siger.Antivirus."+sid));if(mutex==null||Kernel32.INSTANCE.WaitForSingleObject(mutex,0)!=0)throw new IOException("Mutex de control ocupado");
        try{for(var name:List.of("jobs-a","jobs-b")){
            var command=List.of(Path.of(System.getProperty("java.home"),"bin","java.exe").toString(),"--enable-native-access=ALL-UNNAMED","-XX:-UsePerfData","-Xmx256m","-Djava.io.tmpdir="+root.resolve("tmp"),"-cp",System.getProperty("java.class.path"),"MutexAntivirusWindows",root.toString(),name);
            var builder=new ProcessBuilder(command).redirectOutput(root.resolve(name+"-stdout").toFile()).redirectError(root.resolve(name+"-stderr").toFile());builder.environment().clear();builder.environment().put("SystemRoot",System.getenv("SystemRoot"));
            var p=builder.start();try{if(!p.waitFor(15,java.util.concurrent.TimeUnit.SECONDS)){p.destroyForcibly();p.waitFor();throw new IOException("JVM QA vencida");}if(p.exitValue()!=0||!Files.readString(root.resolve(name+"-stdout")).strip().equals("OCUPADO_SIN_SCANNER"))throw new AssertionError("Rechazo entre JVM no probado");}finally{if(p.isAlive()){p.destroyForcibly();p.waitFor();}}
        }}finally{AntivirusDocumentoWindows.Api.K.ReleaseMutex(mutex);Kernel32.INSTANCE.CloseHandle(mutex);}
        // Después de liberar: el mismo adaptador supera el mutex y detecta alteración del paquete.
        var av=adapter(root,"after");Files.writeString(root.resolve("tools/clamscan.exe"),"ALTERADO");
        try{av.limpio(input);throw new AssertionError("Paquete alterado aceptado");}catch(IOException e){if(!e.getMessage().contains("manifiesto"))throw new AssertionError("Mutex no liberado");}
        var result="{\"scope\":\"MUTEX_AV_JAVA_DOS_JVM_SINTETICO\",\"peers\":2,\"distinctRoots\":true,\"rejectedBeforeScanner\":true,\"released\":true,\"realScannerExecuted\":false}";
        Files.writeString(root.resolve("resultado.json"),result);System.out.println(result);
    }
}
