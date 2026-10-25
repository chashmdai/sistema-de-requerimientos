package cl.nexbyte.siger.institucional.documental;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Procesos descartables: mismo helper Landlock/seccomp existente, NPROC=1 y solo el archivo actual. */
public final class PdfAislado implements ProcesamientoDocumental.Renderizador {
    private final Path helper,temporales;
    private boolean verificado;
    public PdfAislado(Path helper,Path temporales){this.helper=helper.toAbsolutePath().normalize();this.temporales=temporales;}
    private byte[] ejecutar(Path job,List<String> arguments)throws IOException{
        if(Files.isSymbolicLink(helper)||!Files.isRegularFile(helper)||!Files.isExecutable(helper))throw new IOException("Aislamiento no disponible");
        var command=new ArrayList<String>(List.of("/usr/bin/prlimit","--nproc=1:1","--",helper.toString(),job.toString()));command.addAll(arguments);
        var builder=new ProcessBuilder(command).redirectInput(new File("/dev/null")).redirectOutput(job.resolve("stdout").toFile()).redirectError(job.resolve("stderr").toFile());
        builder.environment().clear();builder.environment().putAll(Map.of("PATH","/usr/bin:/bin","LANG","C.UTF-8","TMPDIR",job.toString(),"HOME",job.toString(),"PYTHONDONTWRITEBYTECODE","1"));
        var process=builder.start();try{
            if(!process.waitFor(20,TimeUnit.SECONDS)){process.descendants().forEach(p->p.destroyForcibly());process.destroyForcibly();throw new IOException("Representación vencida");}
            if(process.exitValue()!=0||Files.size(job.resolve("stdout"))>65536)throw new IOException("Representación no disponible");
            return Files.readAllBytes(job.resolve("stdout"));
        }catch(InterruptedException interrupted){process.destroyForcibly();Thread.currentThread().interrupt();throw new IOException("Representación interrumpida");}
    }
    private synchronized void verificarAislamiento()throws IOException{
        if(verificado)return;
        var outside=Files.createTempFile(temporales,"control-aislamiento-",".tmp");var job=Files.createTempDirectory(temporales,"aislamiento-");
        try{
            var code="import os,socket,sys\nfrom pathlib import Path\nassert os.getuid()!=0\ntry:Path(sys.argv[1]).read_bytes();raise SystemExit(2)\nexcept PermissionError:pass\ntry:socket.socket();raise SystemExit(3)\nexcept PermissionError:pass\ntry:os.fork();raise SystemExit(4)\nexcept OSError:pass\nprint('AISLADO')";
            if(!new String(ejecutar(job,List.of("/usr/bin/python3","-c",code,outside.toString())),StandardCharsets.US_ASCII).strip().equals("AISLADO"))throw new IOException("Aislamiento no disponible");
            verificado=true;
        }finally{borrarTrabajo(job);Files.deleteIfExists(outside);}
    }
    private Path trabajo(Path original)throws IOException{
        verificarAislamiento();if(Files.size(original)>150L*1024*1024)throw new IOException("Original fuera del perfil de representación");
        var job=Files.createTempDirectory(temporales,"pdf-");try{Files.copy(original,job.resolve("input"));return job;}catch(IOException e){borrarTrabajo(job);throw e;}
    }
    public int paginas(Path original)throws IOException{
        var job=trabajo(original);try{
            var text=new String(ejecutar(job,List.of("/usr/bin/pdfinfo",job.resolve("input").toString())),StandardCharsets.UTF_8);
            var match=java.util.regex.Pattern.compile("(?m)^Pages:\\s+([0-9]+)\\s*$").matcher(text);
            if(!match.find())throw new IOException("PDF sin páginas legibles");int count=Integer.parseInt(match.group(1));if(count<1||count>100)throw new IllegalArgumentException("PDF fuera del perfil multipágina");return count;
        }finally{borrarTrabajo(job);}
    }
    public byte[] pagina(Path original,int pagina)throws IOException{
        if(pagina<1||pagina>100)throw new IllegalArgumentException("Página inválida");var job=trabajo(original);try{
            ejecutar(job,List.of("/usr/bin/pdftoppm","-f",String.valueOf(pagina),"-l",String.valueOf(pagina),"-singlefile","-scale-to","1600","-png",job.resolve("input").toString(),job.resolve("page").toString()));
            var png=job.resolve("page.png");if(Files.isSymbolicLink(png)||!Files.isRegularFile(png)||Files.size(png)>6*1024*1024)throw new IOException("Página no disponible");return Files.readAllBytes(png);
        }finally{borrarTrabajo(job);}
    }
    private void borrarTrabajo(Path job)throws IOException{
        if(!job.normalize().startsWith(temporales.toAbsolutePath().normalize())||job.equals(temporales))throw new IOException("Trabajo inválido");
        try(var entries=Files.walk(job)){for(var p:entries.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
    }
}