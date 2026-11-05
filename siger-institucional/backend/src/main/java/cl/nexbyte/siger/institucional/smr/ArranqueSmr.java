package cl.nexbyte.siger.institucional.smr;

import cl.nexbyte.siger.institucional.SigerInstitucionalApplication;
import cl.nexbyte.siger.institucional.archivos.*;
import cl.nexbyte.siger.institucional.robustez.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.*;
import tools.jackson.databind.json.JsonMapper;

/** Entrada institucional independiente. Solo configuración explícita por stdin privado. */
public final class ArranqueSmr {
    private ArranqueSmr(){}
    private static final Set<String> KEYS=Set.of("institucional.db.url","institucional.db.user","institucional.db.password","institucional.db.principal",
        "server.address","server.port","server.ssl.key-store","server.ssl.key-store-password",
        "institucional.configuracion-privada.directorio","institucional.documentos.directorio",
        "institucional.comprobante.key-id","institucional.comprobante.private-key","institucional.comprobante.trusted-keys","institucional.comprobante.own-trusted-keys",
        "institucional.formulario-completo.origen-publico","institucional.formulario-completo.token","institucional.formulario-completo.ca-certificado",
        "institucional.formulario-completo.habilitado","institucional.smr.instalacion-id","institucional.smr.sintetico","institucional.smr.bootstrap.archivo","institucional.smr.autorizacion.archivo");
    public static Map<String,Object> configurar(Map<String,String> input){
        if(!KEYS.containsAll(input.keySet()))throw new IllegalArgumentException("Propiedad SMR no admitida");
        var p=new HashMap<String,Object>(input);
        p.putAll(Map.of("spring.profiles.active","smr,jdbc-reception","spring.flyway.enabled","false","spring.main.banner-mode","off",
            "institucional.smr.habilitado","true","institucional.nucleo.habilitado","false","institucional.identidad.habilitada","true",
            "institucional.documental.habilitada","false","institucional.v2.habilitada","false","institucional.integracion.habilitada","false"));
        p.putAll(Map.of("institucional.notificaciones.habilitadas","false","institucional.solicitudes.habilitadas","false",
            "institucional.notificaciones.correo.modo","SIMULADO","institucional.notificaciones.correo.automatico","false",
            "institucional.formulario-completo.automatico",input.getOrDefault("institucional.formulario-completo.habilitado","false"),
            "server.ssl.enabled","true","server.ssl.key-store-type","PKCS12","server.servlet.session.cookie.secure","true",
            "server.servlet.session.cookie.name","SMR"+input.getOrDefault("institucional.smr.instalacion-id","").replace("-",""),"server.forward-headers-strategy","none"));
        p.putIfAbsent("server.address","127.0.0.1");p.putIfAbsent("server.port","8443");
        p.put("institucional.db.pool","4");p.put("server.tomcat.threads.max","24");p.put("server.tomcat.threads.min-spare","2");
        return p;
    }
    public static void validar(Environment e){
        if(!e.matchesProfiles("smr")||!e.matchesProfiles("jdbc-reception")||e.matchesProfiles("nucleo-lab","windows-lab","local-integration-demo")
            ||e.getProperty("institucional.nucleo.habilitado",Boolean.class,false)||!e.getProperty("institucional.identidad.habilitada",Boolean.class,false)
            ||!e.getProperty("server.ssl.enabled",Boolean.class,false)||!e.getProperty("server.servlet.session.cookie.secure",Boolean.class,false)
            ||e.getProperty("institucional.identidad.legado-pruebas",Boolean.class,false))throw new IllegalStateException("Modo SMR incompatible");
        for(String flag:List.of("institucional.v2.habilitada","institucional.integracion.habilitada","institucional.solicitudes.habilitadas","institucional.notificaciones.habilitadas","institucional.documental.habilitada"))
            if(e.getProperty(flag,Boolean.class,false))throw new IllegalStateException("Módulo fuera de SMR");
        String url=e.getRequiredProperty("institucional.db.url");ConfiguracionDatos.validar(url,false);
        var uri=URI.create(url.substring(5));
        if(!uri.getPath().matches("/smr_[a-z0-9_]{1,48}")||!e.getRequiredProperty("institucional.db.user").matches("smr_[a-z0-9_]{1,22}_app"))throw new IllegalStateException("Base/identidad SMR exclusiva requerida");
        if(!e.getProperty("institucional.db.principal",e.getRequiredProperty("institucional.db.user")+"@localhost").matches(java.util.regex.Pattern.quote(e.getRequiredProperty("institucional.db.user"))+"@[A-Za-z0-9._-]+"))throw new IllegalStateException("Principal MySQL explícito requerido");
        UUID.fromString(e.getRequiredProperty("institucional.smr.instalacion-id"));
        var origin=URI.create(e.getRequiredProperty("institucional.formulario-completo.origen-publico"));
        if(!"https".equals(origin.getScheme())||origin.getHost()==null||origin.getUserInfo()!=null||origin.getQuery()!=null||origin.getFragment()!=null||!origin.getPath().isEmpty())throw new IllegalStateException("Origen HTTPS sin path requerido");
        if(e.getProperty("institucional.smr.sintetico",Boolean.class,false)&&!Set.of("localhost","127.0.0.1").contains(origin.getHost()))throw new IllegalStateException("Sintético solo con productor loopback");
        if(!e.getRequiredProperty("institucional.comprobante.key-id").matches("[A-Za-z0-9_-]{1,64}"))throw new IllegalStateException("Identidad ACK requerida");
        int port=e.getProperty("server.port",Integer.class,0);if(port<1024||port>65535||port==8080||port==8081)throw new IllegalStateException("Puerto SMR inválido");
        for(String k:List.of("institucional.db.password","server.ssl.key-store-password"))if(e.getProperty(k,"").isBlank())throw new IllegalStateException("Configuración privada requerida");
        for(String k:List.of("institucional.formulario-completo.habilitado","institucional.smr.sintetico"))if(!Set.of("true","false").contains(e.getProperty(k,"false")))throw new IllegalStateException("Booleano SMR inválido");
        try{
            for(String k:List.of("institucional.comprobante.private-key","institucional.comprobante.trusted-keys","institucional.comprobante.own-trusted-keys"))ConfiguracionPrivada.archivo(e,e.getRequiredProperty(k),65536);
            var store=Path.of(e.getRequiredProperty("institucional.documentos.directorio")).toAbsolutePath().normalize();
            for(var p=store;p!=null;p=p.getParent())if(Files.exists(p.resolve(".git")))throw new IllegalStateException("Custodia dentro de Git");
            ArchivoPrivado.validarAcceso(store);
        }catch(java.io.IOException ex){throw new IllegalStateException("Custodia/configuración privada SMR rechazada");}
    }
    private static Map<String,String> leer(){
        try{
            byte[] bytes=System.in.readNBytes(65537);try{if(bytes.length>65536)throw new IllegalArgumentException();
                var f=tools.jackson.core.json.JsonFactory.builder().enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
                var tree=JsonMapper.builder(f).build().readTree(bytes);if(!tree.isObject())throw new IllegalArgumentException();
                var map=new HashMap<String,String>();for(var entry:tree.properties()){if(!entry.getValue().isString())throw new IllegalArgumentException();map.put(entry.getKey(),entry.getValue().asString());}return map;
            }finally{Arrays.fill(bytes,(byte)0);}
        }catch(Exception ex){throw new IllegalStateException("Configuración SMR rechazada; valores omitidos");}
    }
    /** El consentimiento de custodia/cola es externo; nunca se fabrica ni se deriva del Bearer. */
    public static void verificarAutorizacion(Environment e){
        try{
            Path path=ArchivoPrivado.validar(Path.of(e.getRequiredProperty("institucional.smr.autorizacion.archivo")),8192);
            var a=JsonMapper.builder().build().readTree(Files.readAllBytes(path));
            for(String key:List.of("custodiaEnEstePc","todaColaElegible","receptorUnico","respaldoVerificado","confianzaAckInstalada","pullUbuntuAbierto"))if(!a.path(key).isBoolean()||!a.path(key).asBoolean())throw new IllegalArgumentException();
            if(a.path("referencia").asString().isBlank()||!java.time.Instant.parse(a.path("ventanaHastaUtc").asString()).isAfter(java.time.Instant.now()))throw new IllegalArgumentException();
            var own=Path.of(e.getRequiredProperty("institucional.comprobante.own-trusted-keys"));
            if(!java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(own))).equals(a.path("ackTrustSha256").asString()))throw new IllegalArgumentException();
        }catch(Exception ex){throw new FalloTecnico(503,"CUSTODIA_COLA_NO_AUTORIZADA");}
    }
    public static void main(String[] args){
        if(args.length!=0)throw new IllegalArgumentException();
        var env=new StandardEnvironment();env.getPropertySources().addFirst(new MapPropertySource("smr-stdin-privado",configurar(leer())));
        validar(env);var app=new SpringApplication(SigerInstitucionalApplication.class);app.setEnvironment(env);app.setAddCommandLineProperties(false);app.run();
    }
    public static void migrar(){
        var p=leer();if(!KEYS.containsAll(p.keySet()))throw new IllegalArgumentException();
        String url=p.get("institucional.db.url"),user=p.get("institucional.db.user");ConfiguracionDatos.validar(url,false);
        if(!URI.create(url.substring(5)).getPath().matches("/smr_[a-z0-9_]{1,48}")||!user.matches("smr_[a-z0-9_]{1,22}_migrator"))throw new IllegalArgumentException("Migración solo SMR");
        try(var db=new BaseJdbc(url,user,p.get("institucional.db.password"),1)){db.migrate("institucional");}
    }
    public static void sonda(){
        var p=leer();try(var source=new cl.nexbyte.siger.institucional.formulario.FuenteFormulario(URI.create(p.get("institucional.formulario-completo.origen-publico")),p.getOrDefault("institucional.formulario-completo.token","SONDA_SIN_CREDENCIAL_VALIDA_000000000000"),p.containsKey("institucional.formulario-completo.ca-certificado")?Path.of(p.get("institucional.formulario-completo.ca-certificado")):null,false)){
            var result=p.containsKey("institucional.formulario-completo.token")?source.comprobarConexion():source.comprobarSinCredencial();System.out.println(JsonMapper.builder().build().writeValueAsString(result));
        }catch(Exception ex){throw new IllegalStateException("Sonda SMR no acreditada; revisar DNS/TLS/salida sin revelar valores");}
    }
}
