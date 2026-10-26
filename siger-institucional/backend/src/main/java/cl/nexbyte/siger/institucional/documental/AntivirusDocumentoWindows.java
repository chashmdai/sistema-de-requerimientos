package cl.nexbyte.siger.institucional.documental;

import cl.nexbyte.siger.institucional.archivos.ArchivoPrivado;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import com.sun.jna.win32.*;
import com.sun.jna.platform.win32.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** ClamAV descartable: solo el archivo actual, AppContainer sin capacidades y Job de un proceso.
 * No ejecuta shells ni acepta comandos. No habilita el resto del perfil documental Windows.
 */
public final class AntivirusDocumentoWindows implements ProcesamientoDocumental.Antivirus {
    private static final long MEMORY=1536L*1024*1024, FREE=4L*1024*1024*1024, CPU=450_000_000L;
    private static final int FLAGS=0x2|0x8|0x100|0x2000;
    private final Path tools, definitions, jobs;
    private final Map<String,String> hashes;
    public record Resultado(int scannerExit,long peakPrivateBytes,long elapsedMillis,boolean appContainer,int capabilities,int processLimit,long memoryLimitBytes,long cpuLimitTicks,boolean cleanupComplete) {
        @Override public String toString(){return "ResultadoAntivirusWindows[sin contenido]";}
    }
    private volatile Resultado ultimo;
    public Resultado ultimoResultado(){return ultimo;}

    public AntivirusDocumentoWindows(Path tools,Path definitions,Path jobs,Map<String,String> hashes)throws IOException {
        if(!System.getProperty("os.name","").startsWith("Windows")||Native.POINTER_SIZE!=8)throw new IOException("Solo antivirus Windows x64 nativo");
        this.tools=tools.toAbsolutePath().normalize();this.definitions=definitions.toAbsolutePath().normalize();this.jobs=jobs.toAbsolutePath().normalize();
        this.hashes=Map.copyOf(hashes);
        if(this.tools.startsWith(this.definitions)||this.definitions.startsWith(this.tools)||this.tools.startsWith(this.jobs)||this.jobs.startsWith(this.tools)||this.jobs.startsWith(this.definitions)||this.definitions.startsWith(this.jobs))throw new IOException("Raíces independientes requeridas");
        for(var directory:List.of(this.tools,this.definitions,this.jobs)){
            ArchivoPrivado.validarAcceso(directory);
            if(!Files.isDirectory(directory,LinkOption.NOFOLLOW_LINKS))throw new IOException("Directorio privado requerido");
            for(Path p=directory;p!=null;p=p.getParent())if(Files.exists(p.resolve(".git")))throw new IOException("Worker fuera de Git requerido");
        }
        verificarHerramientas();
        // Un corte del proceso padre puede dejar evidencia pendiente. Nunca reutilizarla como análisis limpio.
        try(var children=Files.list(this.jobs)){if(children.anyMatch(p->p.getFileName().toString().startsWith("av-")))throw new IOException("Trabajo antivirus pendiente de conciliación privada");}
    }
    private static IOException error(String step){return new IOException("Antivirus aislado no disponible: "+step+" ("+Kernel32.INSTANCE.GetLastError()+")");}
    private static void check(boolean ok,String step)throws IOException{if(!ok)throw error(step);}
    private static String quote(Path p)throws IOException {String s=p.toString();if(s.indexOf('"')>=0||s.indexOf('\n')>=0||s.indexOf('\r')>=0)throw new IOException("Ruta no admitida");return '"'+s+'"';}
    private static String sha(Path file)throws IOException {
        try(var in=Files.newInputStream(file,LinkOption.NOFOLLOW_LINKS)){var md=MessageDigest.getInstance("SHA-256");byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)md.update(b,0,n);return HexFormat.of().formatHex(md.digest());}
        catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    private void verificarHerramientas()throws IOException {
        if(!hashes.containsKey("clamscan.exe"))throw new IOException("Manifiesto ClamAV requerido");
        try(var files=Files.walk(tools)){
            for(var p:files.toList()) {
                ArchivoPrivado.validarAcceso(p);
                if(!Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS))continue;
                var name=tools.relativize(p).toString();var expected=hashes.get(name);
                if(expected==null||!expected.matches("[a-f0-9]{64}")||!sha(ArchivoPrivado.validar(p,250L*1024*1024)).equals(expected))throw new IOException("Paquete antivirus distinto del manifiesto");
            }
        }
        for(var name:hashes.keySet())if(!name.matches("[A-Za-z0-9_-][A-Za-z0-9_.-]*(\\\\[A-Za-z0-9_-][A-Za-z0-9_.-]*)?")||!Files.isRegularFile(tools.resolve(name),LinkOption.NOFOLLOW_LINKS))throw new IOException("Manifiesto antivirus inválido");
    }
    @Override public boolean limpio(Path original)throws IOException {
        ultimo=null;
        // Un único mutex por identidad Windows, incluso con otras raíces/jobs/JVM.
        // Una cerradura dentro de jobs no protege las DACL compartidas de herramientas y bases.
        WinNT.HANDLE mutex=null;boolean owned=false;
        try {
            var descriptor=new PointerByReference();String current=usuario();
            check(Api.A.ConvertStringSecurityDescriptorToSecurityDescriptorW(new WString("D:P(A;;GA;;;SY)(A;;GA;;;BA)(A;;GA;;;"+current+")"),1,descriptor,null),"descriptor mutex");
            try{var attrs=new WinBase.SECURITY_ATTRIBUTES();attrs.lpSecurityDescriptor=descriptor.getValue();attrs.write();mutex=Api.K.CreateMutexW(attrs,false,new WString("Local\\Nexbyte.Siger.Antivirus."+current));check(mutex!=null,"mutex");}finally{Kernel32.INSTANCE.LocalFree(descriptor.getValue());}
            int waited=Kernel32.INSTANCE.WaitForSingleObject(mutex,0);owned=waited==0||waited==128;
            if(waited==128)throw new IOException("Antivirus abandonado; requiere conciliación privada");
            if(waited!=0)throw new IOException("Antivirus ocupado en otra instancia");
            ArchivoPrivado.validarAcceso(jobs);
                    try(var existing=Files.list(jobs)){if(existing.anyMatch(p->p.getFileName().toString().startsWith("av-")))throw new IOException("Trabajo antivirus pendiente de conciliación privada");}
                    verificarHerramientas();
                    var memory=new Memoria();memory.length=memory.size();check(Api.K.GlobalMemoryStatusEx(memory),"memoria");memory.read();
                    if(memory.availablePhysical<FREE)throw new IOException("Margen de memoria antivirus insuficiente");
                    var source=ArchivoPrivado.validar(original,FormatosDocumento.MAX);
                    if(Files.size(source)<1)throw new IOException("Archivo vacío no inspeccionable");
                    var job=Files.createDirectory(jobs.resolve("av-"+UUID.randomUUID()));ArchivoPrivado.proteger(job);
                    var input=job.resolve("input");
                    try(var in=Files.newInputStream(source,LinkOption.NOFOLLOW_LINKS);var out=Files.newOutputStream(input,StandardOpenOption.CREATE_NEW)){
                        ArchivoPrivado.proteger(input);long count=0;byte[] b=new byte[65536];int n;
                        while((n=in.read(b))!=-1){count+=n;if(count>FormatosDocumento.MAX)throw new IOException("Archivo excesivo");out.write(b,0,n);}
                    }
                    var result=ejecutar(job,input);
                    ultimo=result;
                    if(result.scannerExit()!=0&&result.scannerExit()!=1)throw new IOException("Inspección antivirus incompleta");
                    return result.scannerExit()==0;
        }finally{if(mutex!=null){boolean released=!owned||Api.K.ReleaseMutex(mutex);Kernel32.INSTANCE.CloseHandle(mutex);if(!released)throw new IOException("Mutex antivirus pendiente de conciliación");}}
    }
    private Resultado ejecutar(Path job,Path input)throws IOException {
        String name="Nexbyte.Siger.Antivirus."+UUID.randomUUID().toString().replace("-","");
        var sid=new PointerByReference();Pointer attributes=null;Memory capsMemory=null,env=null,handles=null;WinNT.HANDLE jobHandle=null,out=null,err=null;
        var process=new WinBase.PROCESS_INFORMATION();boolean initialized=false,assigned=false,profile=false,completed=false;String before=sha(input);long start=System.nanoTime();
        var work=Files.createDirectory(job.resolve("work"));ArchivoPrivado.proteger(work);
        var saved=new LinkedHashMap<Path,WinNT.SECURITY_DESCRIPTOR_RELATIVE>();
        // Mantener vivo el Memory de cada descriptor hasta después de restaurar.
        var descriptors=new ArrayList<Memory>();
        IOException failure=null;Resultado result=null;
        try{
            int hr=Api.U.CreateAppContainerProfile(new WString(name),new WString("SIGER AV"),new WString("Archivo privado único"),null,0,sid);
            if(hr!=0)throw new IOException("Perfil antivirus no disponible");profile=true;
            String identity=Advapi32Util.convertSidToStringSid(new WinNT.PSID(sid.getValue()));
            // Solo estas DACL se amplían; el archivo de entrada no es escribible por el worker.
            for(var p:List.of(tools,definitions,job,work,input)){
                var descriptor=Advapi32Util.getSecurityDescriptorForObject(p.toString(),1,false);descriptors.add(descriptor);
                saved.put(p,new WinNT.SECURITY_DESCRIPTOR_RELATIVE(descriptor));
            }
            String user=usuario();
            grant(tools,user,identity,"OICI","GRGX");grant(definitions,user,identity,"OICI","GRGX");
            grant(job,user,identity,"","0x20");grant(work,user,identity,"OICI","0x1301bf");grant(input,user,identity,"","GR");
            setSddl(work,"S:(ML;OICI;NW;;;LW)",0x10);
            // Journal fuera del espacio escribible del hijo; un corte exige conciliación, nunca limpieza a ciegas.
            Files.writeString(job.resolve("pending"),name+"\n"+identity+"\n",StandardCharsets.US_ASCII,StandardOpenOption.CREATE_NEW);
            var caps=new Capacidades();caps.sid=sid.getValue();caps.write();capsMemory=new Memory(caps.size());capsMemory.write(0,caps.getPointer().getByteArray(0,caps.size()),0,caps.size());
            var size=new LongByReference();Api.K.InitializeProcThreadAttributeList(null,2,0,size);
            attributes=new Memory(size.getValue());check(Api.K.InitializeProcThreadAttributeList(attributes,2,0,size),"atributos");initialized=true;
            check(Api.K.UpdateProcThreadAttribute(attributes,0,new BaseTSD.ULONG_PTR(0x20009),capsMemory,new BaseTSD.SIZE_T(caps.size()),null,null),"capacidades");
            jobHandle=Api.K.CreateJobObjectW(null,null);check(jobHandle!=null,"job");
            var limits=new Limites();limits.basic.flags=FLAGS;limits.basic.processTime=CPU;limits.basic.processes=1;limits.processMemory=new BaseTSD.SIZE_T(MEMORY);limits.write();
            check(Api.K.SetInformationJobObject(jobHandle,9,limits,limits.size()),"límites");
            var effective=new Limites();check(Api.K.QueryInformationJobObject(jobHandle,9,effective,effective.size(),null),"límites efectivos");effective.read();
            if(effective.basic.flags!=FLAGS||effective.basic.processTime!=CPU||effective.basic.processes!=1||effective.processMemory.longValue()!=MEMORY)throw new IOException("Límites antivirus no efectivos");
            var sa=new WinBase.SECURITY_ATTRIBUTES();sa.bInheritHandle=true;sa.write();
            out=Kernel32.INSTANCE.CreateFile(job.resolve("stdout").toString(),WinNT.GENERIC_WRITE,WinNT.FILE_SHARE_READ,sa,WinNT.CREATE_NEW,WinNT.FILE_ATTRIBUTE_NORMAL,null);
            err=Kernel32.INSTANCE.CreateFile(job.resolve("stderr").toString(),WinNT.GENERIC_WRITE,WinNT.FILE_SHARE_READ,sa,WinNT.CREATE_NEW,WinNT.FILE_ATTRIBUTE_NORMAL,null);
            check(!WinBase.INVALID_HANDLE_VALUE.equals(out)&&!WinBase.INVALID_HANDLE_VALUE.equals(err),"salidas");
            handles=new Memory(2L*Native.POINTER_SIZE);handles.setPointer(0,out.getPointer());handles.setPointer(Native.POINTER_SIZE,err.getPointer());
            check(Api.K.UpdateProcThreadAttribute(attributes,0,new BaseTSD.ULONG_PTR(0x20002),handles,new BaseTSD.SIZE_T(handles.size()),null,null),"handles");
            String block="LOCALAPPDATA="+work+"\0SystemRoot="+System.getenv("SystemRoot")+"\0TEMP="+work+"\0TMP="+work+"\0USERPROFILE="+work+"\0windir="+System.getenv("SystemRoot")+"\0\0";
            byte[] environment=block.getBytes(StandardCharsets.UTF_16LE);env=new Memory(environment.length);env.write(0,environment,0,environment.length);
            var startup=new Inicio();startup.info.cb=new WinDef.DWORD(startup.size());startup.info.dwFlags=0x100;startup.info.hStdOutput=out;startup.info.hStdError=err;startup.attributes=attributes;startup.write();
            var exe=tools.resolve("clamscan.exe");
            String command=quote(exe)+" --database="+quote(definitions)+" --tempdir="+quote(work)+" --max-filesize=25M --max-scansize=50M --max-files=2000 --max-recursion=16 --max-scantime=45000 --alert-exceeds-max=yes --no-summary --official-db-only=yes --bytecode-unsigned=no "+quote(input);
            char[] mutable=(command+'\0').toCharArray();
            check(Api.K.CreateProcessW(new WString(exe.toString()),mutable,null,null,true,0x80000|0x4|0x400|0x08000000,env,new WString(work.toString()),startup,process),"proceso suspendido");process.read();
            check(Api.K.AssignProcessToJobObject(jobHandle,process.hProcess),"asignación Job");assigned=true;
            verificarToken(process.hProcess);
            check(Api.K.ResumeThread(process.hThread)!=-1,"reanudar");
            while(Kernel32.INSTANCE.WaitForSingleObject(process.hProcess,50)==258){
                if(Thread.currentThread().isInterrupted())throw new IOException("Antivirus interrumpido");
                if((System.nanoTime()-start)/1_000_000>90_000)throw new IOException("Antivirus vencido");
                if(Files.size(job.resolve("stdout"))+Files.size(job.resolve("stderr"))>1024*1024)throw new IOException("Salida antivirus excesiva");
                try(var entries=Files.walk(work)) {long bytes=0;int count=0;var iterator=entries.iterator();while(iterator.hasNext()) {var p=iterator.next();if((Kernel32.INSTANCE.GetFileAttributes(p.toString())&0x400)!=0)throw new IOException("Reparse point en trabajo antivirus");if(++count>4000)throw new IOException("Temporales antivirus excesivos");if(Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS))bytes+=Files.size(p);if(bytes>768L*1024*1024)throw new IOException("Temporales antivirus excesivos");}}
            }
            var code=new IntByReference();check(Kernel32.INSTANCE.GetExitCodeProcess(process.hProcess,code),"resultado");
            var usage=new Limites();check(Api.K.QueryInformationJobObject(jobHandle,9,usage,usage.size(),null),"consumo");usage.read();
            if(!before.equals(sha(input)))throw new IOException("Entrada antivirus alterada");
            if(Files.size(job.resolve("stdout"))+Files.size(job.resolve("stderr"))>1024*1024)throw new IOException("Salida antivirus excesiva");
            // El código 0 sin un análisis del archivo actual tampoco es suficiente.
            if(code.getValue()==0&&!Files.readString(job.resolve("stdout"),StandardCharsets.UTF_8).contains("input: OK"))throw new IOException("Análisis antivirus sin confirmación");
            result=new Resultado(code.getValue(),usage.peakProcess.longValue(),(System.nanoTime()-start)/1_000_000,true,0,1,MEMORY,CPU,true);
            completed=true;
        }catch(Win32Exception nativeFailure){failure=new IOException("Frontera antivirus Win32 no disponible ("+nativeFailure.getErrorCode()+")");}
        catch(IOException e){failure=e;}
        finally{
            if(process.hProcess!=null){if(assigned&&jobHandle!=null)Api.K.TerminateJobObject(jobHandle,125);else Kernel32.INSTANCE.TerminateProcess(process.hProcess,125);if(Kernel32.INSTANCE.WaitForSingleObject(process.hProcess,5000)!=0)failure=new IOException("Proceso antivirus pendiente de conciliación");Kernel32.INSTANCE.CloseHandle(process.hProcess);}
            if(process.hThread!=null)Kernel32.INSTANCE.CloseHandle(process.hThread);
            if(jobHandle!=null)Kernel32.INSTANCE.CloseHandle(jobHandle);
            for(var h:Arrays.asList(out,err))if(h!=null&&!WinBase.INVALID_HANDLE_VALUE.equals(h))Kernel32.INSTANCE.CloseHandle(h);
            if(initialized)Api.K.DeleteProcThreadAttributeList(attributes);
            for(var entry:saved.entrySet())try{
                // SECURITY_DESCRIPTOR_CONTROL está en offset2. Preservar SE_DACL_PROTECTED,
                // sin convertir una raíz heredada en protegida al retirar la ACE temporal.
                boolean wasProtected=(entry.getValue().getPointer().getShort(2)&0x1000)!=0;
                check(Advapi32.INSTANCE.SetFileSecurity(entry.getKey().toString(),(wasProtected?0x80000000:0x20000000)|4,entry.getValue().getPointer()),"restaurar DACL");
            }catch(IOException e){failure=e;}
            if(profile&&Api.U.DeleteAppContainerProfile(new WString(name))!=0)failure=new IOException("Perfil antivirus pendiente de conciliación");
            if(sid.getValue()!=null)Api.A.FreeSid(sid.getValue());
            // No borrar evidencia ante un error o una frontera no conciliada.
            if(completed&&failure==null){try(var entries=Files.walk(job)){for(var p:entries.sorted(Comparator.reverseOrder()).toList()){ArchivoPrivado.validarAcceso(p);Files.delete(p);}}catch(IOException e){failure=new IOException("Trabajo antivirus pendiente de conciliación");}}
            for(var memory:descriptors)memory.close();for(var memory:Arrays.asList(capsMemory,env,handles))if(memory!=null)memory.close();if(attributes instanceof Memory memory)memory.close();
        }
        if(failure!=null)throw failure;
        return result;
    }
    private static String usuario()throws IOException {
        var token=new WinNT.HANDLEByReference();check(Advapi32.INSTANCE.OpenProcessToken(Kernel32.INSTANCE.GetCurrentProcess(),WinNT.TOKEN_QUERY,token),"identidad");
        try{return Advapi32Util.getTokenAccount(token.getValue()).sidString;}finally{Kernel32.INSTANCE.CloseHandle(token.getValue());}
    }
    private static void setSddl(Path p,String text,int flags)throws IOException {
        var descriptor=new PointerByReference();check(Api.A.ConvertStringSecurityDescriptorToSecurityDescriptorW(new WString(text),1,descriptor,null),"descriptor");
        try{check(Advapi32.INSTANCE.SetFileSecurity(p.toString(),flags,descriptor.getValue()),"DACL");}finally{Kernel32.INSTANCE.LocalFree(descriptor.getValue());}
    }
    private static void grant(Path path,String user,String sid,String inheritance,String rights)throws IOException {
        String own=Files.isDirectory(path)?"OICI":"";
        setSddl(path,"D:P(A;"+own+";FA;;;SY)(A;"+own+";FA;;;BA)(A;"+own+";FA;;;"+user+")(A;"+inheritance+";"+rights+";;;"+sid+")",0x80000004);
    }
    private static void verificarToken(WinNT.HANDLE process)throws IOException {
        var token=new WinNT.HANDLEByReference();check(Advapi32.INSTANCE.OpenProcessToken(process,WinNT.TOKEN_QUERY,token),"token");
        try(var data=new Memory(4096)) {var returned=new IntByReference();check(Api.A.GetTokenInformation(token.getValue(),29,data,4096,returned),"AppContainer");if(data.getInt(0)!=1)throw new IOException("Token no aislado");check(Api.A.GetTokenInformation(token.getValue(),30,data,4096,returned),"capacidades token");if(data.getInt(0)!=0)throw new IOException("Capacidades no permitidas");}finally{Kernel32.INSTANCE.CloseHandle(token.getValue());}
    }
    @Structure.FieldOrder({"length","load","totalPhysical","availablePhysical","totalPage","availablePage","totalVirtual","availableVirtual","extended"}) public static class Memoria extends Structure {public int length,load;public long totalPhysical,availablePhysical,totalPage,availablePage,totalVirtual,availableVirtual,extended;}
    @Structure.FieldOrder({"sid","capabilities","count","reserved"}) public static class Capacidades extends Structure {public Pointer sid,capabilities;public int count,reserved;}
    @Structure.FieldOrder({"processTime","jobTime","flags","min","max","processes","affinity","priority","scheduling"}) public static class Basicos extends Structure {public long processTime,jobTime;public int flags;public BaseTSD.SIZE_T min=new BaseTSD.SIZE_T(),max=new BaseTSD.SIZE_T();public int processes;public BaseTSD.ULONG_PTR affinity=new BaseTSD.ULONG_PTR();public int priority,scheduling;}
    @Structure.FieldOrder({"a","b","c","d","e","f"}) public static class Io extends Structure {public long a,b,c,d,e,f;}
    @Structure.FieldOrder({"basic","io","processMemory","jobMemory","peakProcess","peakJob"}) public static class Limites extends Structure {public Basicos basic=new Basicos();public Io io=new Io();public BaseTSD.SIZE_T processMemory=new BaseTSD.SIZE_T(),jobMemory=new BaseTSD.SIZE_T(),peakProcess=new BaseTSD.SIZE_T(),peakJob=new BaseTSD.SIZE_T();}
    @Structure.FieldOrder({"info","attributes"}) public static class Inicio extends Structure {public WinBase.STARTUPINFO info=new WinBase.STARTUPINFO();public Pointer attributes;}
    public interface Api extends StdCallLibrary {
        Api K=Native.load("kernel32",Api.class,W32APIOptions.UNICODE_OPTIONS),U=Native.load("userenv",Api.class,W32APIOptions.UNICODE_OPTIONS),A=Native.load("advapi32",Api.class,W32APIOptions.UNICODE_OPTIONS);
        boolean GlobalMemoryStatusEx(Memoria memory);int CreateAppContainerProfile(WString name,WString display,WString description,Pointer capabilities,int count,PointerByReference sid);int DeleteAppContainerProfile(WString name);
        WinNT.HANDLE CreateMutexW(WinBase.SECURITY_ATTRIBUTES attributes,boolean owner,WString name);boolean ReleaseMutex(WinNT.HANDLE mutex);
        boolean InitializeProcThreadAttributeList(Pointer list,int count,int flags,LongByReference size);boolean UpdateProcThreadAttribute(Pointer list,int flags,BaseTSD.ULONG_PTR attribute,Pointer value,BaseTSD.SIZE_T size,Pointer previous,Pointer returned);void DeleteProcThreadAttributeList(Pointer list);int ResumeThread(WinNT.HANDLE thread);
        WinNT.HANDLE CreateJobObjectW(Pointer attributes,WString name);boolean SetInformationJobObject(WinNT.HANDLE job,int info,Limites limits,int length);boolean QueryInformationJobObject(WinNT.HANDLE job,int info,Limites limits,int length,IntByReference returned);boolean AssignProcessToJobObject(WinNT.HANDLE job,WinNT.HANDLE process);boolean TerminateJobObject(WinNT.HANDLE job,int code);
        boolean CreateProcessW(WString app,char[] command,Pointer processAttributes,Pointer threadAttributes,boolean inherit,int flags,Pointer environment,WString cwd,Inicio startup,WinBase.PROCESS_INFORMATION process);
        boolean ConvertStringSecurityDescriptorToSecurityDescriptorW(WString text,int revision,PointerByReference descriptor,IntByReference size);
        Pointer FreeSid(Pointer sid);boolean GetTokenInformation(WinNT.HANDLE token,int info,Pointer data,int size,IntByReference returned);
    }
}
