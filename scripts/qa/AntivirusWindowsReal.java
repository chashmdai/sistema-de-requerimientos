import cl.nexbyte.siger.institucional.archivos.ArchivoPrivado;
import cl.nexbyte.siger.institucional.documental.AntivirusDocumentoWindows;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import tools.jackson.databind.json.JsonMapper;

/** Solo fixtures sintéticos. No MySQL, correo, recepción pública ni documentos históricos. */
class AntivirusWindowsReal {
    public static void main(String[] args)throws Exception {
        var mapper=JsonMapper.builder().build();var root=Path.of(args[0]).toAbsolutePath().normalize();
        if(!root.toString().matches("C:\\\\req\\\\testdata\\\\av-java-[a-f0-9]{32}"))throw new IllegalArgumentException("Solo raíz QA nueva");
        ArchivoPrivado.validarAcceso(root);
        var manifest=mapper.readTree(Files.readAllBytes(Path.of("C:/req/evidencias/clamav-portable-2026-10-26.json")));
        Map<String,String> hashes=new HashMap<>();
        for(var file:manifest.path("files"))hashes.put(file.path("name").asString(),file.path("sha256").asString());
        var tools=Path.of("C:/req/bin/clamav-1.4.6");var defs=Path.of("C:/req/storage/antivirus-defs");
        var jobs=Files.createDirectory(root.resolve("jobs"));ArchivoPrivado.proteger(jobs);
        var diagnostics=Files.createDirectory(root.resolve("evidence"));ArchivoPrivado.proteger(diagnostics);
        var clean=root.resolve("clean.txt");Files.writeString(clean,"SINTETICO");ArchivoPrivado.proteger(clean);
        var eicar=root.resolve("control.txt");Files.write(eicar,Base64.getDecoder().decode("WDVPIVAlQEFQWzRcUFpYNTQoUF4pN0NDKTd9JEVJQ0FSLVNUQU5EQVJELUFOVElWSVJVUy1URVNULUZJTEUhJEgrSCo="));ArchivoPrivado.proteger(eicar);
        var evidence=new LinkedHashMap<String,Object>();var checks=new ArrayList<Object>();
        evidence.put("scope","JAVA_AV_WINDOWS_REAL_NO_RUNTIME_COMPLETO");evidence.put("root",root.toString());evidence.put("fullDocumentalRuntime",false);evidence.put("checks",checks);
        String phase="CONFIGURACION";
        AntivirusDocumentoWindows av=null;
        try {
            av=new AntivirusDocumentoWindows(tools,defs,jobs,hashes,diagnostics);
            phase="CLEAN";if(!av.limpio(clean))throw new AssertionError("Limpio rechazado");checks.add(Map.of("scenario",phase,"correct",true,"result",av.ultimoResultado()));
            phase="EICAR";if(av.limpio(eicar))throw new AssertionError("Control aceptado");checks.add(Map.of("scenario",phase,"correct",true,"result",av.ultimoResultado()));
            var empty=Files.createDirectory(root.resolve("defs-empty"));ArchivoPrivado.proteger(empty);
            av=new AntivirusDocumentoWindows(tools,empty,jobs,hashes,diagnostics);
            phase="UNAVAILABLE";boolean rejected=false;try{av.limpio(clean);}catch(IOException expected){rejected=true;}
            if(!rejected||av.ultimoResultado()==null||av.ultimoResultado().scannerExit()!=2)throw new AssertionError("Fallo AV no acreditado");
            checks.add(Map.of("scenario",phase,"correct",true,"result",av.ultimoResultado()));
            phase="REINTENTO";av=new AntivirusDocumentoWindows(tools,defs,jobs,hashes);
            try(var files=Files.list(jobs)){if(files.anyMatch(p->p.getFileName().toString().startsWith("av-")))throw new AssertionError("Trabajo no retirado");}
            evidence.put("state","TRES_ESCENARIOS_CORRECTOS");
        }catch(Throwable failure){evidence.put("state","NO_ACREDITADO");evidence.put("failurePhase",phase);evidence.put("failureClass",failure.getClass().getSimpleName());if(failure instanceof IOException)evidence.put("failure",failure.getMessage());
            if(av!=null&&av.ultimoResultado()!=null)evidence.put("lastScannerResult",av.ultimoResultado());
            if(failure instanceof AntivirusDocumentoWindows.MemoriaInsuficiente memory){evidence.put("state","BLOQUEADO_RECURSOS");evidence.put("availablePhysicalBytes",memory.disponible());evidence.put("requiredFreeBytes",memory.requerida());evidence.put("jvmFreePhysicalControlBytes",((com.sun.management.OperatingSystemMXBean)java.lang.management.ManagementFactory.getOperatingSystemMXBean()).getFreeMemorySize());evidence.put("jvmHeapCommittedBytes",Runtime.getRuntime().totalMemory());evidence.put("jvmHeapUsedBytes",Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());evidence.put("scannerExecuted",!checks.isEmpty());}
        }
        evidence.put("diagnosticDirectory",diagnostics.toString());
        Files.write(root.resolve("resultado.json"),mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(evidence));
        System.out.println(mapper.writeValueAsString(evidence));
        if(!"TRES_ESCENARIOS_CORRECTOS".equals(evidence.get("state")))System.exit(1);
    }
}
