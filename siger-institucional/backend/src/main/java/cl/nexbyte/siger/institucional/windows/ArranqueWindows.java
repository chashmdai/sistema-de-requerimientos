package cl.nexbyte.siger.institucional.windows;

import cl.nexbyte.siger.institucional.SigerInstitucionalApplication;
import cl.nexbyte.siger.institucional.archivos.PlataformaArchivos;
import cl.nexbyte.siger.institucional.robustez.ConfiguracionDatos;
import java.io.*;
import java.util.*;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.*;
import tools.jackson.databind.json.JsonMapper;

/** Arranque QA. Credenciales por stdin anónimo; recuperación manual solo por habilitación explícita. */
public final class ArranqueWindows {
    private ArranqueWindows(){}
    static Map<String,Object> configurar(Map<String,String> input){
        if(!PlataformaArchivos.windows()||!Set.of("PRELIMINAR_SIN_DOCUMENTAL","DOCUMENTAL_QA_CALIFICADO").contains(input.getOrDefault("siger.windows.modo","")))throw new IllegalArgumentException("Modo Windows QA no admitido");
        boolean documental="DOCUMENTAL_QA_CALIFICADO".equals(input.get("siger.windows.modo"));
        if(documental){String config=input.get("institucional.documental.windows-configuracion");if(config==null||!java.nio.file.Path.of(config).toAbsolutePath().normalize().toString().matches("C:\\\\req\\\\testdata\\\\documental-win-[a-f0-9]{32}\\\\windows\\.properties"))throw new IllegalArgumentException("Configuración documental QA requerida");}
        else if(input.containsKey("institucional.documental.windows-configuracion"))throw new IllegalArgumentException("Documental no habilitado");
        String url=input.get("institucional.db.url");ConfiguracionDatos.validar(url,false);
        if(!url.startsWith("jdbc:mysql://127.0.0.1:3306/siger_institucional_windows_qa?")||!"siger_windows_qa_app".equals(input.get("institucional.db.user")))throw new IllegalArgumentException("Solo aplicación QA");
        if(input.getOrDefault("institucional.db.password","").isBlank()||input.getOrDefault("server.ssl.key-store","").isBlank()||input.getOrDefault("server.ssl.key-store-password","").isBlank())throw new IllegalArgumentException("Configuración privada incompleta");
        String port=input.getOrDefault("server.port","8081");if(!Set.of("8080","8081").contains(port))throw new IllegalArgumentException("Puerto QA no admitido");
        for(var key:input.keySet())if(!PERMITIDOS.contains(key))throw new IllegalArgumentException("Propiedad no admitida");
        var p=new HashMap<String,Object>(input);
        p.putAll(Map.of("spring.profiles.active","jdbc-reception,nucleo-lab,windows-lab","spring.flyway.enabled","false","server.address","127.0.0.1","server.port",port,"server.ssl.enabled","true","server.ssl.key-store-type","PKCS12","server.servlet.session.cookie.secure","true","server.forward-headers-strategy","none","institucional.nucleo.habilitado","true","institucional.nucleo.entorno","LABORATORIO"));
        p.put("institucional.solicitudes.habilitadas","true");
        p.putAll(Map.of("datos.mysql-local","true","institucional.identidad.habilitada","true","institucional.documental.habilitada","false","institucional.formulario-completo.habilitado","false","institucional.formulario-completo.automatico","false","institucional.v2.habilitada","false","institucional.integracion.habilitada","false","institucional.notificaciones.habilitadas","true","institucional.notificaciones.correo.modo","SIMULADO","institucional.notificaciones.correo.automatico","false"));
        p.put("institucional.documental.habilitada",Boolean.toString(documental));
        boolean manual="true".equals(input.getOrDefault("siger.windows.formulario-manual","false"));
        boolean automatico="true".equals(input.getOrDefault("siger.windows.formulario-automatico","false"));
        for(String key:List.of("siger.windows.formulario-manual","siger.windows.formulario-automatico"))if(input.containsKey(key)&&!Set.of("true","false").contains(input.get(key)))throw new IllegalArgumentException("Habilitación inválida");
        if(manual&&automatico)throw new IllegalArgumentException("Seleccionar un solo modo de recuperación QA");
        if(manual||automatico){
            if(!documental||!"https://formulario.defensorianinez.gob.cl".equals(input.get("institucional.formulario-completo.origen-publico")))throw new IllegalArgumentException("Recuperación manual requiere documental QA y origen local aprobado");
            for(String key:List.of("institucional.formulario-completo.token","operacion.token"))if(!input.getOrDefault(key,"").matches("[A-Za-z0-9_-]{32,400}"))throw new IllegalArgumentException("Credencial técnica requerida");
            String root=input.get("institucional.configuracion-privada.directorio"),ca=input.get("institucional.formulario-completo.ca-certificado");
            if(root==null||ca==null||!java.nio.file.Path.of(root).isAbsolute()||!java.nio.file.Path.of(ca).normalize().equals(java.nio.file.Path.of(root).resolve("formulario-ca.pem").normalize()))throw new IllegalArgumentException("CA privada explícita requerida");
            try{if(Arrays.stream(java.net.InetAddress.getAllByName("formulario.defensorianinez.gob.cl")).anyMatch(a->!a.isLoopbackAddress()))throw new IllegalArgumentException("Destino fuera de loopback");}catch(java.net.UnknownHostException e){throw new IllegalArgumentException("Origen local no resuelto");}
            p.put("institucional.formulario-completo.habilitado","true");
            p.put("institucional.formulario-completo.automatico",Boolean.toString(automatico));
            p.put("institucional.formulario-completo.exclusivo-qa",Boolean.toString(automatico));
        }else for(String key:List.of("institucional.formulario-completo.origen-publico","institucional.formulario-completo.token","institucional.formulario-completo.ca-certificado","operacion.token"))if(input.containsKey(key))throw new IllegalArgumentException("Recuperación manual no habilitada");
        p.put("spring.main.banner-mode","off");return p;
    }
    private static final Set<String> PERMITIDOS=Set.of("siger.windows.modo","siger.windows.formulario-manual","siger.windows.formulario-automatico","institucional.formulario-completo.origen-publico","institucional.formulario-completo.token","institucional.formulario-completo.ca-certificado","operacion.token","institucional.documental.windows-configuracion","institucional.db.url","institucional.db.user","institucional.db.password","server.port","server.ssl.key-store","server.ssl.key-store-password","institucional.configuracion-privada.directorio","institucional.comprobante.key-id","institucional.comprobante.private-key","institucional.comprobante.own-trusted-keys","institucional.comprobante.trusted-keys","institucional.documentos.directorio","institucional.notificaciones.correo.allowlist","institucional.notificaciones.correo.campana");
    public static void main(String[] args)throws Exception{
        Map<String,Object> properties;
        try{
            if(args.length!=0)throw new IllegalArgumentException();
            byte[] bytes=System.in.readNBytes(65537);if(bytes.length>65536)throw new IllegalArgumentException();
            try{
                var factory=tools.jackson.core.json.JsonFactory.builder().enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION).streamReadConstraints(tools.jackson.core.StreamReadConstraints.builder().maxNestingDepth(2).maxStringLength(8192).build()).build();
                var tree=JsonMapper.builder(factory).build().readTree(bytes);if(!tree.isObject())throw new IllegalArgumentException();
                var input=new HashMap<String,String>();for(var entry:tree.properties()){if(!entry.getValue().isString())throw new IllegalArgumentException();input.put(entry.getKey(),entry.getValue().asString());}
                properties=configurar(input);
            }finally{Arrays.fill(bytes,(byte)0);}
        }catch(Exception failure){throw new IllegalStateException("Configuración Windows QA rechazada; valores omitidos");}
        var environment=new StandardEnvironment();environment.getPropertySources().addFirst(new MapPropertySource("windows-stdin-privado",properties));
        var app=new SpringApplication(SigerInstitucionalApplication.class);app.setEnvironment(environment);app.setAddCommandLineProperties(false);app.run();
    }
}
