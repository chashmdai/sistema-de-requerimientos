package cl.nexbyte.siger.institucional.documental;

import cl.nexbyte.siger.institucional.archivos.*;
import com.sun.jna.*;import com.sun.jna.platform.win32.*;import com.sun.jna.ptr.*;
import java.io.*;import java.nio.channels.FileChannel;import java.nio.file.*;import java.time.*;import java.util.*;

/** Journal privado de ACL antes de mutarlas. Solo concilia trabajos propios con dueño terminado. */
final class DiarioTrabajoWindows {
    private static final String FILE="journal.properties";
    static Properties preparar(Path job,Path tools,Path defs,String name,String sid,Map<Path,WinNT.SECURITY_DESCRIPTOR_RELATIVE> saved)throws IOException{
        var p=new Properties();p.setProperty("version","1");p.setProperty("name",name);p.setProperty("sid",sid);p.setProperty("job",job.toString());p.setProperty("tools",tools.toString());p.setProperty("defs",defs==null?"":defs.toString());
        p.setProperty("owner",Long.toString(ProcessHandle.current().pid()));p.setProperty("start",Long.toString(ProcessHandle.current().info().startInstant().orElseThrow(()->new IOException("Identidad de proceso ausente")).toEpochMilli()));
        int i=0;for(var e:saved.entrySet()){var size=Advapi32.INSTANCE.GetSecurityDescriptorLength(e.getValue().getPointer());if(size<20||size>65536)throw new IOException("Descriptor fuera de perfil");p.setProperty("path."+i,e.getKey().toString());p.setProperty("acl."+i,Base64.getEncoder().encodeToString(e.getValue().getPointer().getByteArray(0,size)));i++;}p.setProperty("count",Integer.toString(i));guardar(job,p);return p;
    }
    static void hijo(Path job,Properties p,int pid)throws IOException{
        var child=ProcessHandle.of(Integer.toUnsignedLong(pid)).orElseThrow(()->new IOException("Hijo no localizable"));p.setProperty("child",Long.toString(child.pid()));p.setProperty("childStart",Long.toString(child.info().startInstant().orElseThrow(()->new IOException("Identidad de hijo ausente")).toEpochMilli()));guardar(job,p);
    }
    private static void guardar(Path job,Properties p)throws IOException{
        PlataformaArchivos.actual().validarCadena(job);Path temp=job.resolve("journal.next"),dest=job.resolve(FILE);
        try(var channel=FileChannel.open(temp,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);var out=java.nio.channels.Channels.newOutputStream(channel)){ArchivoPrivado.proteger(temp);p.store(out,"SIGER_DOCUMENTAL_WINDOWS_V1");out.flush();channel.force(true);}
        if(!Kernel32.INSTANCE.MoveFileEx(temp.toString(),dest.toString(),new WinDef.DWORD(1|8)))throw new IOException("No se pudo confirmar journal NTFS");
        var h=Kernel32.INSTANCE.CreateFile(job.toString(),WinNT.GENERIC_WRITE,WinNT.FILE_SHARE_READ|WinNT.FILE_SHARE_WRITE|WinNT.FILE_SHARE_DELETE,null,WinNT.OPEN_EXISTING,0x02200000,null);
        if(WinBase.INVALID_HANDLE_VALUE.equals(h))throw new IOException("Directorio de journal no disponible");try{if(!Kernel32.INSTANCE.FlushFileBuffers(h))throw new IOException("Journal sin barrera de directorio");}finally{Kernel32.INSTANCE.CloseHandle(h);}
    }
    static int recuperar(Path jobs,Path tools,Path defs)throws IOException{
        ArchivoPrivado.validarAcceso(jobs);int count=0;
        try(var entries=Files.list(jobs)){for(var job:entries.toList())if(job.getFileName().toString().startsWith("av-")){
            recuperarUno(job,tools,defs);count++;
        }}return count;
    }
    private static void recuperarUno(Path job,Path tools,Path defs)throws IOException{
        PlataformaArchivos.actual().validarCadena(job);if(!job.getFileName().toString().matches("av-[a-f0-9-]{36}"))throw new IOException("Trabajo desconocido; conciliar manualmente");
        var file=ArchivoPrivado.validar(job.resolve(FILE),512*1024);var p=new Properties();try(var in=Files.newInputStream(file)){p.load(in);}
        if(!"1".equals(p.getProperty("version"))||!job.toString().equals(p.getProperty("job"))||!tools.toString().equals(p.getProperty("tools"))||!Objects.toString(defs,"").equals(p.getProperty("defs")))throw new IOException("Journal de otro perfil");
        String name=p.getProperty("name"),sid=p.getProperty("sid");if(name==null||!name.matches("Nexbyte\\.Siger\\.Antivirus\\.[a-f0-9]{32}"))throw new IOException("Perfil desconocido");
        var derived=new PointerByReference();if(ProcesoDocumentoWindows.Api.U.DeriveAppContainerSidFromAppContainerName(new WString(name),derived)!=0)throw new IOException("SID no derivable");try{if(!Advapi32Util.convertSidToStringSid(new WinNT.PSID(derived.getValue())).equals(sid))throw new IOException("SID de journal no corresponde");}finally{ProcesoDocumentoWindows.Api.A.FreeSid(derived.getValue());}
        if(vivo(p,"owner","start"))throw new IOException("Trabajo de otro proceso vivo");
        if(p.getProperty("child")!=null&&vivo(p,"child","childStart")){
            try{ProcessHandle.of(Long.parseLong(p.getProperty("child"))).orElseThrow().onExit().get(5,java.util.concurrent.TimeUnit.SECONDS);}catch(Exception e){throw new IOException("Hijo sin conciliar; no se mata por PID");}
        }
        var expected=new LinkedHashSet<Path>(List.of(tools,job,job.resolve("work"),job.resolve("input")));if(defs!=null)expected.add(defs);
        int n;try{n=Integer.parseInt(p.getProperty("count"));}catch(Exception e){throw new IOException("Journal inválido");}if(n!=expected.size())throw new IOException("Journal incompleto");
        for(int i=0;i<n;i++){
            Path target=Path.of(p.getProperty("path."+i)).toAbsolutePath().normalize();if(!expected.remove(target))throw new IOException("Ruta de journal no autorizada");PlataformaArchivos.actual().validarCadena(target);
            byte[] bytes;try{bytes=Base64.getDecoder().decode(p.getProperty("acl."+i));}catch(Exception e){throw new IOException("Descriptor inválido");}if(bytes.length<20||bytes.length>65536)throw new IOException("Descriptor fuera de perfil");
            try(var memory=new Memory(bytes.length)){memory.write(0,bytes,0,bytes.length);if(!ProcesoDocumentoWindows.Api.A.IsValidSecurityDescriptor(memory))throw new IOException("Descriptor NTFS inválido");var sd=new WinNT.SECURITY_DESCRIPTOR_RELATIVE(memory);var acl=sd.getDiscretionaryACL();if(acl==null)throw new IOException("DACL requerida");for(var ace:acl.getACEs())if(ace instanceof WinNT.ACCESS_ALLOWED_ACE a&&!permitido(a.getSidString())&&!(a.getSidString().equals("S-1-3-0")&&(a.AceFlags&8)!=0))throw new IOException("Journal amplía permisos");boolean protect=(memory.getShort(2)&0x1000)!=0;ProcesoDocumentoWindows.aplicarDescriptor(target,memory,(protect?0x80000000:0x20000000)|4);}
        }
        int hr=ProcesoDocumentoWindows.Api.U.DeleteAppContainerProfile(new WString(name));if(hr!=0&&hr!=0x80070002&&hr!=0x80070490)throw new IOException("Perfil no conciliado");
        borrar(job);
    }
    private static boolean vivo(Properties p,String pid,String start)throws IOException{
        try{long id=Long.parseLong(p.getProperty(pid)),expected=Long.parseLong(p.getProperty(start));var proc=ProcessHandle.of(id);return proc.isPresent()&&proc.get().isAlive()&&proc.get().info().startInstant().map(t->t.toEpochMilli()==expected).orElse(true);}catch(Exception e){throw new IOException("Identidad de journal inválida");}
    }
    private static boolean permitido(String sid)throws IOException{
        if(Set.of("S-1-5-18","S-1-5-32-544").contains(sid))return true;var token=new WinNT.HANDLEByReference();if(!Advapi32.INSTANCE.OpenProcessToken(Kernel32.INSTANCE.GetCurrentProcess(),WinNT.TOKEN_QUERY,token))throw new IOException("Identidad Windows no disponible");try{return Advapi32Util.getTokenAccount(token.getValue()).sidString.equals(sid);}finally{Kernel32.INSTANCE.CloseHandle(token.getValue());}
    }
    static void borrar(Path job)throws IOException{
        try(var entries=Files.walk(job)){for(var p:entries.sorted(Comparator.reverseOrder()).toList()){PlataformaArchivos.actual().validarCadena(p);ArchivoPrivado.proteger(p);Files.delete(p);}}
    }
}
