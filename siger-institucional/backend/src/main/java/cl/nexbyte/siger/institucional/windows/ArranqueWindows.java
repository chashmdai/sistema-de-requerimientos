package cl.nexbyte.siger.institucional.windows;

import cl.nexbyte.siger.institucional.SigerInstitucionalApplication;
import cl.nexbyte.siger.institucional.archivos.PlataformaArchivos;
import cl.nexbyte.siger.institucional.robustez.ConfiguracionDatos;
import java.io.*;
import java.util.*;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.*;
import tools.jackson.databind.json.JsonMapper;

/** Arranque QA opt-in. Credenciales por stdin anónimo; transportes externos siempre cerrados. */
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
        p.putAll(Map.of("datos.mysql-local","true","institucional.identidad.habilitada","true","institucional.documental.habilitada","false","institucional.formulario-completo.habilitado","false","institucional.formulario-completo.automatico","false","institucional.v2.habilitada","false","institucional.integracion.habilitada","false","institucional.notificaciones.habilitadas","true","institucional.notificaciones.correo.modo","SIMULADO","institucional.notificaciones.correo.automatico","false"));
        p.put("institucional.documental.habilitada",Boolean.toString(documental));
        p.put("spring.main.banner-mode","off");return p;
    }
    private static final Set<String> PERMITIDOS=Set.of("siger.windows.modo","institucional.documental.windows-configuracion","institucional.db.url","institucional.db.user","institucional.db.password","server.port","server.ssl.key-store","server.ssl.key-store-password","institucional.configuracion-privada.directorio","institucional.comprobante.key-id","institucional.comprobante.private-key","institucional.comprobante.own-trusted-keys","institucional.comprobante.trusted-keys","institucional.documentos.directorio","institucional.notificaciones.correo.allowlist","institucional.notificaciones.correo.campana");
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
