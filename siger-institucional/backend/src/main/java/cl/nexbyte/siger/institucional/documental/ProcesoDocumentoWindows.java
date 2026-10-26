package cl.nexbyte.siger.institucional.documental;

import cl.nexbyte.siger.institucional.archivos.ArchivoPrivado;
import cl.nexbyte.siger.institucional.archivos.PlataformaArchivos;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import com.sun.jna.win32.*;
import com.sun.jna.platform.win32.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Solo operaciones documentales fijas: archivo actual, AppContainer sin capacidades y Job de un proceso. */
public final class ProcesoDocumentoWindows {
    public enum Operacion {
        AV("clamscan.exe"), PDF_CHECK("bin/qpdf.exe"), PDF_ENCRYPTED("bin/qpdf.exe"), PDF_JSON("bin/qpdf.exe"),
        PDF_INFO("bin/pdfinfo.exe"), PDF_PAGINA("bin/pdftoppm.exe"), MEDIA_INFO("bin/ffprobe.exe"),
        IMAGEN("bin/ffmpeg.exe"), MEDIA_DECODIFICAR("bin/ffmpeg.exe"), OFICINA("bin/java.exe");
        final String ejecutable; Operacion(String path){ejecutable=Path.of(path).toString();}
        long memoria(){return this==AV?1536L*1024*1024:512L*1024*1024;}
        long libre(){return this==AV?4L*1024*1024*1024:1024L*1024*1024;}
        long cpu(){return 450_000_000L;}
        long maxEntrada(){return this==AV?FormatosDocumento.MAX:150L*1024*1024;}
        long salida(){return this==AV?1024*1024:8L*1024*1024;}
    }
    public record Ejecucion(Resultado resultado,byte[] stdout,byte[] stderr,byte[] archivo){
        public Ejecucion{stdout=stdout.clone();stderr=stderr.clone();archivo=archivo==null?null:archivo.clone();}
        @Override public byte[] stdout(){return stdout.clone();} @Override public byte[] stderr(){return stderr.clone();} @Override public byte[] archivo(){return archivo==null?null:archivo.clone();}
        @Override public String toString(){return "EjecucionDocumental[privada]";}
    }
    private static final int FLAGS=0x2|0x8|0x100|0x2000;
    private final Path tools, definitions, jobs, evidencia;
    private final Map<String,String> hashes;
    public record Resultado(int scannerExit,long peakPrivateBytes,long elapsedMillis,boolean appContainer,int capabilities,int processLimit,long memoryLimitBytes,long cpuLimitTicks,boolean cleanupComplete,boolean jobAtCreation) {
        @Override public String toString(){return "ResultadoAntivirusWindows[sin contenido]";}
    }
    private volatile Resultado ultimo;
    public record Limpieza(boolean hijoTerminado,boolean aclRestauradas,boolean perfilRetirado,boolean temporalesRetirados){}
    private volatile Limpieza limpieza;
    public Limpieza ultimaLimpieza(){return limpieza;}
    public Resultado ultimoResultado(){return ultimo;}
    public static final class MemoriaInsuficiente extends IOException {
        private final long disponible;
        private final long requerida; MemoriaInsuficiente(long disponible,long requerida){super("Margen de memoria documental insuficiente");this.disponible=disponible;this.requerida=requerida;}
        public long disponible(){return disponible;}
        public long requerida(){return requerida;}
    }

    public ProcesoDocumentoWindows(Path tools,Path definitions,Path jobs,Map<String,String> hashes)throws IOException {
        this(tools,definitions,jobs,hashes,null);
    }
    /** Evidencia privada opt-in: solo salidas acotadas, nunca copia el original. */
    public ProcesoDocumentoWindows(Path tools,Path definitions,Path jobs,Map<String,String> hashes,Path evidencia)throws IOException {
        if(!System.getProperty("os.name","").startsWith("Windows")||Native.POINTER_SIZE!=8)throw new IOException("Solo antivirus Windows x64 nativo");
        this.tools=tools.toAbsolutePath().normalize();this.definitions=definitions==null?null:definitions.toAbsolutePath().normalize();this.jobs=jobs.toAbsolutePath().normalize();
        this.hashes=Map.copyOf(hashes);
        this.evidencia=evidencia==null?null:evidencia.toAbsolutePath().normalize();
        if(this.evidencia!=null){ArchivoPrivado.validarAcceso(this.evidencia);if(!Files.isDirectory(this.evidencia)||this.evidencia.startsWith(this.jobs)||this.jobs.startsWith(this.evidencia)||this.evidencia.startsWith(this.tools)||(this.definitions!=null&&this.evidencia.startsWith(this.definitions)))throw new IOException("Raíz de evidencia independiente requerida");for(var p=this.evidencia;p!=null;p=p.getParent())if(Files.exists(p.resolve(".git")))throw new IOException("Evidencia privada fuera de Git requerida");}
        if(this.tools.startsWith(this.jobs)||this.jobs.startsWith(this.tools)||(this.definitions!=null&&(this.tools.startsWith(this.definitions)||this.definitions.startsWith(this.tools)||this.jobs.startsWith(this.definitions)||this.definitions.startsWith(this.jobs))))throw new IOException("Raíces independientes requeridas");
        var directories=new ArrayList<Path>(List.of(this.tools,this.jobs));if(this.definitions!=null)directories.add(this.definitions);
        for(var directory:directories){
            PlataformaArchivos.actual().validarCadena(directory);
            if(!Files.isDirectory(directory,LinkOption.NOFOLLOW_LINKS))throw new IOException("Directorio privado requerido");
            for(Path p=directory;p!=null;p=p.getParent())if(Files.exists(p.resolve(".git")))throw new IOException("Worker fuera de Git requerido");
        }
        ArchivoPrivado.validarAcceso(this.jobs);
        try(var mutex=tomarMutex()){DiarioTrabajoWindows.recuperar(this.jobs,this.tools,this.definitions);for(var directory:directories)ArchivoPrivado.validarAcceso(directory);verificarHerramientas();}
    }
    private record Mutex(WinNT.HANDLE handle)implements AutoCloseable{public void close()throws IOException{boolean released=Api.K.ReleaseMutex(handle);Kernel32.INSTANCE.CloseHandle(handle);if(!released)throw new IOException("Mutex documental sin conciliar");}}
    private static Mutex tomarMutex()throws IOException{
        var descriptor=new PointerByReference();String current=usuario();check(Api.A.ConvertStringSecurityDescriptorToSecurityDescriptorW(new WString("D:P(A;;GA;;;SY)(A;;GA;;;BA)(A;;GA;;;"+current+")"),1,descriptor,null),"descriptor mutex");WinNT.HANDLE handle;
        try{var attrs=new WinBase.SECURITY_ATTRIBUTES();attrs.lpSecurityDescriptor=descriptor.getValue();attrs.write();handle=Api.K.CreateMutexW(attrs,false,new WString("Local\\Nexbyte.Siger.Antivirus."+current));check(handle!=null,"mutex");}finally{Kernel32.INSTANCE.LocalFree(descriptor.getValue());}
        int waited=Kernel32.INSTANCE.WaitForSingleObject(handle,0);if(waited==0||waited==128)return new Mutex(handle);Kernel32.INSTANCE.CloseHandle(handle);throw new IOException("Antivirus ocupado en otra instancia");
    }
    private static IOException error(String step){return new IOException("Antivirus aislado no disponible: "+step+" ("+Kernel32.INSTANCE.GetLastError()+")");}
    private static void check(boolean ok,String step)throws IOException{if(!ok)throw error(step);}
    private static String quote(Path p)throws IOException {String s=p.toString();if(s.indexOf('"')>=0||s.indexOf('\n')>=0||s.indexOf('\r')>=0)throw new IOException("Ruta no admitida");return '"'+s+'"';}
    private static String sha(Path file)throws IOException {
        try(var in=Files.newInputStream(file,LinkOption.NOFOLLOW_LINKS)){var md=MessageDigest.getInstance("SHA-256");byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)md.update(b,0,n);return HexFormat.of().formatHex(md.digest());}
        catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    private void verificarHerramientas()throws IOException {
        if(hashes.isEmpty())throw new IOException("Manifiesto de herramientas requerido");
        try(var files=Files.walk(tools)){
            for(var p:files.toList()) {
                ArchivoPrivado.validarAcceso(p);
                if(!Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS))continue;
                var name=tools.relativize(p).toString();var expected=hashes.get(name);
                if(expected==null||!expected.matches("[a-f0-9]{64}")||!sha(ArchivoPrivado.validar(p,250L*1024*1024)).equals(expected))throw new IOException("Paquete antivirus distinto del manifiesto");
            }
        }
        for(var name:hashes.keySet()){var relative=Path.of(name);if(relative.isAbsolute()||relative.normalize().startsWith("..")||!tools.resolve(relative).normalize().startsWith(tools)||!Files.isRegularFile(tools.resolve(relative),LinkOption.NOFOLLOW_LINKS))throw new IOException("Manifiesto de herramientas inválido");}
    }
    public Ejecucion procesar(Path original,Operacion op,String detalle)throws IOException {
        ultimo=null;
        // Un único mutex por identidad Windows, incluso con otras raíces/jobs/JVM.
        // Una cerradura dentro de jobs no protege las DACL compartidas de herramientas y bases.
        try(var mutex=tomarMutex()) {
            ArchivoPrivado.validarAcceso(jobs);
                    DiarioTrabajoWindows.recuperar(jobs,tools,definitions);
                    verificarHerramientas();
                    var memory=new Memoria();memory.length=memory.size();check(Api.K.GlobalMemoryStatusEx(memory),"memoria");memory.read();
                    if(memory.availablePhysical<op.libre())throw new MemoriaInsuficiente(memory.availablePhysical,op.libre());
                    if(!hashes.containsKey(op.ejecutable)||(op==Operacion.AV&&definitions==null))throw new IOException("Herramienta no configurada");
                    var source=ArchivoPrivado.validar(original,op.maxEntrada());
                    if(Files.size(source)<1)throw new IOException("Archivo vacío no inspeccionable");
                    var job=Files.createDirectory(jobs.resolve("av-"+UUID.randomUUID()));ArchivoPrivado.proteger(job);
                    var input=job.resolve("input");
                    try(var in=Files.newInputStream(source,LinkOption.NOFOLLOW_LINKS);var out=Files.newOutputStream(input,StandardOpenOption.CREATE_NEW)){
                        ArchivoPrivado.proteger(input);long count=0;byte[] b=new byte[65536];int n;
                        while((n=in.read(b))!=-1){count+=n;if(count>op.maxEntrada())throw new IOException("Archivo excesivo");out.write(b,0,n);}
                    }
                    var result=ejecutar(job,input,op,detalle);
                    ultimo=result.resultado();
                    return result;
        }
    }
    private Ejecucion ejecutar(Path job,Path input,Operacion op,String detalle)throws IOException {
        String name="Nexbyte.Siger.Antivirus."+UUID.randomUUID().toString().replace("-","");
        var sid=new PointerByReference();Pointer attributes=null;Memory capsMemory=null,env=null,handles=null,jobList=null;WinNT.HANDLE jobHandle=null,out=null,err=null;
        var process=new WinBase.PROCESS_INFORMATION();boolean initialized=false,assigned=false,profile=false,completed=false;String before=sha(input);long start=System.nanoTime();
        var work=Files.createDirectory(job.resolve("work"));ArchivoPrivado.proteger(work);
        var saved=new LinkedHashMap<Path,WinNT.SECURITY_DESCRIPTOR_RELATIVE>();
        // Mantener vivo el Memory de cada descriptor hasta después de restaurar.
        var descriptors=new ArrayList<Memory>();
        IOException failure=null;Ejecucion result=null;boolean childClosed=true,aclClosed=true,profileClosed=true;Properties journal=null;
        try{
            var derived=new PointerByReference();if(Api.U.DeriveAppContainerSidFromAppContainerName(new WString(name),derived)!=0)throw new IOException("No se pudo derivar perfil");String identity;try{identity=Advapi32Util.convertSidToStringSid(new WinNT.PSID(derived.getValue()));}finally{Api.A.FreeSid(derived.getValue());}
            // Solo estas DACL se amplían; el archivo de entrada no es escribible por el worker.
            var secured=new ArrayList<Path>(List.of(tools,job,work,input));if(definitions!=null)secured.add(definitions);
            for(var p:secured){
                var descriptor=Advapi32Util.getSecurityDescriptorForObject(p.toString(),1,false);descriptors.add(descriptor);
                saved.put(p,new WinNT.SECURITY_DESCRIPTOR_RELATIVE(descriptor));
            }
            journal=DiarioTrabajoWindows.preparar(job,tools,definitions,name,identity,saved);
            int hr=Api.U.CreateAppContainerProfile(new WString(name),new WString("SIGER documental"),new WString("Archivo privado único"),null,0,sid);if(hr!=0)throw new IOException("Perfil documental no disponible");profile=true;
            if(!identity.equals(Advapi32Util.convertSidToStringSid(new WinNT.PSID(sid.getValue()))))throw new IOException("Perfil no corresponde al journal");
            String user=usuario();
            grant(tools,user,identity,"OICI","GRGX");if(definitions!=null)grant(definitions,user,identity,"OICI","GRGX");
            grant(job,user,identity,"","0x20");grant(work,user,identity,"OICI","0x1301bf");grant(input,user,identity,"","GR");
            setSddl(work,"S:(ML;OICI;NW;;;LW)",0x10);
            // Journal fuera del espacio escribible del hijo; un corte exige conciliación, nunca limpieza a ciegas.
            Files.writeString(job.resolve("pending"),name+"\n"+identity+"\n",StandardCharsets.US_ASCII,StandardOpenOption.CREATE_NEW);
            var caps=new Capacidades();caps.sid=sid.getValue();caps.write();capsMemory=new Memory(caps.size());capsMemory.write(0,caps.getPointer().getByteArray(0,caps.size()),0,caps.size());
            var size=new LongByReference();Api.K.InitializeProcThreadAttributeList(null,3,0,size);
            attributes=new Memory(size.getValue());check(Api.K.InitializeProcThreadAttributeList(attributes,3,0,size),"atributos");initialized=true;
            check(Api.K.UpdateProcThreadAttribute(attributes,0,new BaseTSD.ULONG_PTR(0x20009),capsMemory,new BaseTSD.SIZE_T(caps.size()),null,null),"capacidades");
            jobHandle=Api.K.CreateJobObjectW(null,null);check(jobHandle!=null,"job");
            var limits=new Limites();limits.basic.flags=FLAGS;limits.basic.processTime=op.cpu();limits.basic.processes=1;limits.processMemory=new BaseTSD.SIZE_T(op.memoria());limits.write();
            check(Api.K.SetInformationJobObject(jobHandle,9,limits,limits.size()),"límites");
            var effective=new Limites();check(Api.K.QueryInformationJobObject(jobHandle,9,effective,effective.size(),null),"límites efectivos");effective.read();
            if(effective.basic.flags!=FLAGS||effective.basic.processTime!=op.cpu()||effective.basic.processes!=1||effective.processMemory.longValue()!=op.memoria())throw new IOException("Límites antivirus no efectivos");
            // Adjuntar al Job DURANTE CreateProcess elimina la ventana de orfandad
            // entre crear suspendido y una posterior llamada AssignProcessToJobObject.
            jobList=new Memory(Native.POINTER_SIZE);jobList.setPointer(0,jobHandle.getPointer());
            check(Api.K.UpdateProcThreadAttribute(attributes,0,new BaseTSD.ULONG_PTR(0x2000D),jobList,new BaseTSD.SIZE_T(jobList.size()),null,null),"Job al crear");
            var sa=new WinBase.SECURITY_ATTRIBUTES();sa.bInheritHandle=true;sa.write();
            out=Kernel32.INSTANCE.CreateFile(job.resolve("stdout").toString(),WinNT.GENERIC_WRITE,WinNT.FILE_SHARE_READ,sa,WinNT.CREATE_NEW,WinNT.FILE_ATTRIBUTE_NORMAL,null);
            err=Kernel32.INSTANCE.CreateFile(job.resolve("stderr").toString(),WinNT.GENERIC_WRITE,WinNT.FILE_SHARE_READ,sa,WinNT.CREATE_NEW,WinNT.FILE_ATTRIBUTE_NORMAL,null);
            check(!WinBase.INVALID_HANDLE_VALUE.equals(out)&&!WinBase.INVALID_HANDLE_VALUE.equals(err),"salidas");
            handles=new Memory(2L*Native.POINTER_SIZE);handles.setPointer(0,out.getPointer());handles.setPointer(Native.POINTER_SIZE,err.getPointer());
            check(Api.K.UpdateProcThreadAttribute(attributes,0,new BaseTSD.ULONG_PTR(0x20002),handles,new BaseTSD.SIZE_T(handles.size()),null,null),"handles");
            String block="LOCALAPPDATA="+work+"\0SystemRoot="+System.getenv("SystemRoot")+"\0TEMP="+work+"\0TMP="+work+"\0USERPROFILE="+work+"\0windir="+System.getenv("SystemRoot")+"\0\0";
            byte[] environment=block.getBytes(StandardCharsets.UTF_16LE);env=new Memory(environment.length);env.write(0,environment,0,environment.length);
            var startup=new Inicio();startup.info.cb=new WinDef.DWORD(startup.size());startup.info.dwFlags=0x100;startup.info.hStdOutput=out;startup.info.hStdError=err;startup.attributes=attributes;startup.write();
            var exe=tools.resolve(op.ejecutable);
            String command=comando(op,exe,input,work,detalle);
            char[] mutable=(command+'\0').toCharArray();
            check(Api.K.CreateProcessW(new WString(exe.toString()),mutable,null,null,true,0x80000|0x4|0x400|0x08000000,env,new WString(work.toString()),startup,process),"proceso suspendido");process.read();
            assigned=true;var inJob=new IntByReference();check(Api.K.IsProcessInJob(process.hProcess,jobHandle,inJob),"pertenencia Job");if(inJob.getValue()!=1)throw new IOException("Proceso no asociado al Job");
            DiarioTrabajoWindows.hijo(job,journal,process.dwProcessId.intValue());
            verificarToken(process.hProcess);
            check(Api.K.ResumeThread(process.hThread)!=-1,"reanudar");
            while(Kernel32.INSTANCE.WaitForSingleObject(process.hProcess,50)==258){
                if(Thread.currentThread().isInterrupted())throw new IOException("Antivirus interrumpido");
                if((System.nanoTime()-start)/1_000_000>90_000)throw new IOException("Antivirus vencido");
                if(Files.size(job.resolve("stdout"))+Files.size(job.resolve("stderr"))>op.salida())throw new IOException("Salida antivirus excesiva");
                try(var entries=Files.walk(work)) {long bytes=0;int count=0;var iterator=entries.iterator();while(iterator.hasNext()) {var p=iterator.next();if((Kernel32.INSTANCE.GetFileAttributes(p.toString())&0x400)!=0)throw new IOException("Reparse point en trabajo antivirus");if(++count>4000)throw new IOException("Temporales antivirus excesivos");if(Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS))bytes+=Files.size(p);if(bytes>768L*1024*1024)throw new IOException("Temporales antivirus excesivos");}}
            }
            var code=new IntByReference();check(Kernel32.INSTANCE.GetExitCodeProcess(process.hProcess,code),"resultado");
            var usage=new Limites();check(Api.K.QueryInformationJobObject(jobHandle,9,usage,usage.size(),null),"consumo");usage.read();
            if(!before.equals(sha(input)))throw new IOException("Entrada antivirus alterada");
            if(Files.size(job.resolve("stdout"))+Files.size(job.resolve("stderr"))>op.salida())throw new IOException("Salida antivirus excesiva");
            // El código 0 sin un análisis del archivo actual tampoco es suficiente.
            if(op==Operacion.AV&&code.getValue()==0&&!Files.readString(job.resolve("stdout"),StandardCharsets.UTF_8).contains("input: OK"))throw new IOException("Análisis antivirus sin confirmación");
            completed=true; // Un exit no limpio no es un proceso sin conciliar.
            byte[] generated=null;
            if(code.getValue()==0&&(op==Operacion.PDF_PAGINA||op==Operacion.IMAGEN)){
                var file=work.resolve("page.png");PlataformaArchivos.actual().validarCadena(file);
                if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)||Files.size(file)>6L*1024*1024)throw new IOException("Página no disponible");
                generated=Files.readAllBytes(file);
            }
            var meta=new Resultado(code.getValue(),usage.peakProcess.longValue(),(System.nanoTime()-start)/1_000_000,true,0,1,op.memoria(),op.cpu(),true,true);
            result=new Ejecucion(meta,Files.readAllBytes(job.resolve("stdout")),Files.readAllBytes(job.resolve("stderr")),generated);
        }catch(Win32Exception nativeFailure){failure=new IOException("Frontera antivirus Win32 no disponible ("+nativeFailure.getErrorCode()+")");}
        catch(IOException e){failure=e;}
        finally{
            boolean wasInterrupted=Thread.interrupted();
            if(process.hProcess!=null){if(assigned&&jobHandle!=null)Api.K.TerminateJobObject(jobHandle,125);else Kernel32.INSTANCE.TerminateProcess(process.hProcess,125);if(Kernel32.INSTANCE.WaitForSingleObject(process.hProcess,5000)!=0){childClosed=false;failure=new IOException("Proceso antivirus pendiente de conciliación");}Kernel32.INSTANCE.CloseHandle(process.hProcess);}
            if(process.hThread!=null)Kernel32.INSTANCE.CloseHandle(process.hThread);
            if(jobHandle!=null)Kernel32.INSTANCE.CloseHandle(jobHandle);
            for(var h:Arrays.asList(out,err))if(h!=null&&!WinBase.INVALID_HANDLE_VALUE.equals(h))Kernel32.INSTANCE.CloseHandle(h);
            if(initialized)Api.K.DeleteProcThreadAttributeList(attributes);
            for(var entry:saved.entrySet())try{
                // SECURITY_DESCRIPTOR_CONTROL está en offset2. Preservar SE_DACL_PROTECTED,
                // sin convertir una raíz heredada en protegida al retirar la ACE temporal.
                boolean wasProtected=(entry.getValue().getPointer().getShort(2)&0x1000)!=0;
                aplicarDescriptor(entry.getKey(),entry.getValue().getPointer(),(wasProtected?0x80000000:0x20000000)|4);
            }catch(IOException e){failure=e;aclClosed=false;}
            if(profile&&Api.U.DeleteAppContainerProfile(new WString(name))!=0){profileClosed=false;failure=new IOException("Perfil antivirus pendiente de conciliación");}
            if(sid.getValue()!=null)Api.A.FreeSid(sid.getValue());
            if(childClosed&&aclClosed&&profileClosed&&evidencia!=null){try{
                var target=Files.createDirectory(evidencia.resolve(job.getFileName()));ArchivoPrivado.proteger(target);
                for(var file:List.of("stdout","stderr")){var source=job.resolve(file);if(!Files.exists(source))continue;PlataformaArchivos.actual().validarCadena(source);var dest=target.resolve(file);try(var in=Files.newInputStream(source,LinkOption.NOFOLLOW_LINKS);var output=Files.newOutputStream(dest,StandardOpenOption.CREATE_NEW)){ArchivoPrivado.proteger(dest);byte[] buffer=new byte[65536];long remaining=op.salida();int n;while(remaining>0&&(n=in.read(buffer,0,(int)Math.min(buffer.length,remaining)))!=-1){output.write(buffer,0,n);remaining-=n;}}}
            }catch(IOException e){failure=new IOException("No se pudo conservar evidencia privada antivirus");}}
            boolean tempClosed=false;if(childClosed&&aclClosed&&profileClosed){try{DiarioTrabajoWindows.borrar(job);tempClosed=true;}catch(IOException e){failure=new IOException("Trabajo antivirus pendiente de conciliación");}}
            limpieza=new Limpieza(childClosed,aclClosed,profileClosed,tempClosed);
            for(var memory:descriptors)memory.close();for(var memory:Arrays.asList(capsMemory,env,handles,jobList))if(memory!=null)memory.close();if(attributes instanceof Memory memory)memory.close();
            if(wasInterrupted)Thread.currentThread().interrupt();
        }
        if(failure!=null)throw failure;
        return result;
    }
    private String comando(Operacion op,Path exe,Path input,Path work,String detalle)throws IOException{
        var a=new ArrayList<String>();a.add(exe.toString());
        switch(op){
            case AV -> a.addAll(List.of("--database="+definitions,"--tempdir="+work,"--max-filesize=25M","--max-scansize=50M","--max-files=2000","--max-recursion=16","--max-scantime=45000","--alert-exceeds-max=yes","--no-summary","--official-db-only=yes","--bytecode-unsigned=no",input.toString()));
            case PDF_CHECK -> a.addAll(List.of("--check",input.toString()));
            case PDF_ENCRYPTED -> a.addAll(List.of("--is-encrypted",input.toString()));
            case PDF_JSON -> a.addAll(List.of("--json","--json-stream-data=none",input.toString()));
            case PDF_INFO -> a.add(input.toString());
            case PDF_PAGINA -> {int page;try{page=Integer.parseInt(detalle);}catch(Exception e){throw new IOException("Página inválida");}if(page<1||page>100)throw new IOException("Página inválida");a.addAll(List.of("-f",detalle,"-l",detalle,"-singlefile","-scale-to","1600","-png",input.toString(),work.resolve("page").toString()));}
            case MEDIA_INFO -> a.addAll(List.of("-v","error","-protocol_whitelist","file,pipe","-threads","1","-show_entries","format=format_name,duration:stream=codec_type,codec_name,width,height,channels,sample_rate","-of","json",input.toString()));
            case IMAGEN -> a.addAll(List.of("-v","error","-xerror","-protocol_whitelist","file,pipe","-threads","1","-i",input.toString(),"-frames:v","1","-vf","scale=1024:1024:force_original_aspect_ratio=decrease","-threads","1",work.resolve("page.png").toString()));
            case MEDIA_DECODIFICAR -> a.addAll(List.of("-v","error","-xerror","-protocol_whitelist","file,pipe","-threads","1","-i",input.toString(),"-map","0","-threads","1","-f","null","-"));
            case OFICINA -> {if(!Set.of("txt","docx","xlsx","pptx","odt").contains(detalle))throw new IOException("Formato inválido");a.addAll(List.of("-Xmx128m","-XX:-UsePerfData","-Djava.io.tmpdir="+work,"-m","cl.siger.documental/cl.nexbyte.siger.institucional.documental.worker.InspectorOficina",input.toString(),detalle));}
        }
        var quoted=new ArrayList<String>();for(var value:a){if(value.contains("\"")||value.contains("\n")||value.contains("\r"))throw new IOException("Argumento no admitido");quoted.add("\""+value+"\"");}return String.join(" ",quoted);
    }
    private static String usuario()throws IOException {
        var token=new WinNT.HANDLEByReference();check(Advapi32.INSTANCE.OpenProcessToken(Kernel32.INSTANCE.GetCurrentProcess(),WinNT.TOKEN_QUERY,token),"identidad");
        try{return Advapi32Util.getTokenAccount(token.getValue()).sidString;}finally{Kernel32.INSTANCE.CloseHandle(token.getValue());}
    }
    private static void setSddl(Path p,String text,int flags)throws IOException {
        var descriptor=new PointerByReference();check(Api.A.ConvertStringSecurityDescriptorToSecurityDescriptorW(new WString(text),1,descriptor,null),"descriptor");
        try{aplicarDescriptor(p,descriptor.getValue(),flags);}finally{Kernel32.INSTANCE.LocalFree(descriptor.getValue());}
    }
    static void aplicarDescriptor(Path p,Pointer descriptor,int flags)throws IOException {
        Pointer dacl=null,sacl=null;var present=new IntByReference();var defaulted=new IntByReference();var acl=new PointerByReference();
        if((flags&4)!=0){check(Api.A.GetSecurityDescriptorDacl(descriptor,present,acl,defaulted),"leer DACL");if(present.getValue()!=1||acl.getValue()==null)throw new IOException("DACL privada requerida");dacl=acl.getValue();}
        if((flags&0x10)!=0){check(Api.A.GetSecurityDescriptorSacl(descriptor,present,acl,defaulted),"leer etiqueta");if(present.getValue()!=1||acl.getValue()==null)throw new IOException("Etiqueta requerida");sacl=acl.getValue();}
        // SetFileSecurity NO propaga ACE a los hijos existentes. SetNamedSecurityInfo sí,
        // tanto al conceder lectura como al retirar la ACE al terminar el proceso.
        int code=Advapi32.INSTANCE.SetNamedSecurityInfo(p.toString(),1,flags,null,null,dacl,sacl);
        if(code!=0)throw new IOException("ACL antivirus no disponible ("+code+")");
    }
    static void grant(Path path,String user,String sid,String inheritance,String rights)throws IOException {
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
        int DeriveAppContainerSidFromAppContainerName(WString name,PointerByReference sid);boolean IsValidSecurityDescriptor(Pointer descriptor);
        WinNT.HANDLE CreateMutexW(WinBase.SECURITY_ATTRIBUTES attributes,boolean owner,WString name);boolean ReleaseMutex(WinNT.HANDLE mutex);
        boolean InitializeProcThreadAttributeList(Pointer list,int count,int flags,LongByReference size);boolean UpdateProcThreadAttribute(Pointer list,int flags,BaseTSD.ULONG_PTR attribute,Pointer value,BaseTSD.SIZE_T size,Pointer previous,Pointer returned);void DeleteProcThreadAttributeList(Pointer list);int ResumeThread(WinNT.HANDLE thread);
        WinNT.HANDLE CreateJobObjectW(Pointer attributes,WString name);boolean SetInformationJobObject(WinNT.HANDLE job,int info,Limites limits,int length);boolean QueryInformationJobObject(WinNT.HANDLE job,int info,Limites limits,int length,IntByReference returned);boolean IsProcessInJob(WinNT.HANDLE process,WinNT.HANDLE job,IntByReference result);boolean TerminateJobObject(WinNT.HANDLE job,int code);
        boolean CreateProcessW(WString app,char[] command,Pointer processAttributes,Pointer threadAttributes,boolean inherit,int flags,Pointer environment,WString cwd,Inicio startup,WinBase.PROCESS_INFORMATION process);
        boolean ConvertStringSecurityDescriptorToSecurityDescriptorW(WString text,int revision,PointerByReference descriptor,IntByReference size);
        Pointer FreeSid(Pointer sid);boolean GetTokenInformation(WinNT.HANDLE token,int info,Pointer data,int size,IntByReference returned);
        boolean GetSecurityDescriptorDacl(Pointer descriptor,IntByReference present,PointerByReference acl,IntByReference defaulted);boolean GetSecurityDescriptorSacl(Pointer descriptor,IntByReference present,PointerByReference acl,IntByReference defaulted);
    }
}
