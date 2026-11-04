package cl.nexbyte.siger.institucional.smr;

import cl.nexbyte.siger.institucional.identidad.*;
import cl.nexbyte.siger.institucional.nucleo.*;
import cl.nexbyte.siger.institucional.robustez.*;
import cl.nexbyte.siger.institucional.archivos.*;
import java.net.URI;import java.nio.file.*;import java.security.*;import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;

@Configuration @ConditionalOnProperty(name="institucional.smr.habilitado",havingValue="true")
public class SmrConfiguration {
    @Bean CatalogoLaboratorio catalogoSmr(IdentidadInstitucional identity){return new CatalogoLaboratorio(identity);}
    @Bean AccesoSmr accesoSmr(BaseJdbc db,IdentidadInstitucional identity){return new AccesoSmr(db,identity);}
    @Bean Boolean validarRuntimeSmr(BaseJdbc db,Environment e)throws Exception{
        String schema=URI.create(e.getRequiredProperty("institucional.db.url").substring(5)).getPath().substring(1),user=e.getRequiredProperty("institucional.db.user");
        if(!schema.equals(db.sql.queryForObject("SELECT DATABASE()",String.class))||!e.getProperty("institucional.db.principal",user+"@localhost").equals(db.sql.queryForObject("SELECT CURRENT_USER()",String.class)))throw new IllegalStateException("Identidad de base SMR incorrecta");
        for(String grant:db.sql.queryForList("SHOW GRANTS",String.class))if(!grant.startsWith("GRANT USAGE ON *.* TO ")){
            int on=grant.indexOf(" ON ");if(on<0||!grant.substring(on).startsWith(" ON `"+schema+"`.* TO ")||!Set.of("SELECT","INSERT","UPDATE","DELETE").containsAll(Arrays.asList(grant.substring(6,on).split(", "))))throw new IllegalStateException("Runtime SMR requiere DML exclusivo");
        }
        if(db.sql.queryForObject("SELECT @@innodb_flush_log_at_trx_commit",Integer.class)!=1)throw new IllegalStateException("Durabilidad MySQL requerida");
        String origin=e.getRequiredProperty("institucional.formulario-completo.origen-publico");
        String fingerprint=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(("SMR_ORIGEN_1\0"+origin+"\0"+e.getRequiredProperty("institucional.smr.instalacion-id")).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        db.transaction(()->{var rows=db.sql.queryForList("SELECT origin_ref,binding_sha256 FROM consumer_origin_binding WHERE id=1 FOR UPDATE");
            if(rows.isEmpty()){
                if(db.sql.queryForObject("SELECT COUNT(*) FROM institutional_inbox",Long.class)!=0)throw new IllegalStateException("Datos sin vínculo SMR");
                db.sql.update("INSERT INTO consumer_origin_binding VALUES(1,?,?,?)",origin,fingerprint,System.currentTimeMillis());
            }else if(!origin.equals(rows.getFirst().get("origin_ref"))||!fingerprint.equals(rows.getFirst().get("binding_sha256")))throw new IllegalStateException("Cambio de origen/instalación SMR rechazado");return null;});
        return true;
    }
    public record Bootstrap(IdentidadInstitucional.Crear cuenta,boolean lectura,boolean operacion){}
    @Bean public org.springframework.boot.ApplicationRunner bootstrapSmr(IdentidadInstitucional identity,AccesoSmr access,BaseJdbc db,Environment e){return args->{
        String name=e.getProperty("institucional.smr.bootstrap.archivo");if(name==null)return;
        Path p=ConfiguracionPrivada.ruta(e,name),done=p.resolveSibling(p.getFileName()+".aplicado");
        if(Files.notExists(p)&&Files.exists(done)){ArchivoPrivado.validar(done,16384);return;}
        ArchivoPrivado.validar(p,16384);byte[] bytes=Files.readAllBytes(p);var data=tools.jackson.databind.json.JsonMapper.builder().build().readValue(bytes,Bootstrap.class);
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));Arrays.fill(bytes,(byte)0);
        db.transaction(()->{
            if(db.sql.queryForObject("SELECT COUNT(*) FROM identity_event WHERE event_code='SMR_BOOTSTRAP_APLICADO' AND resource_id=?",Integer.class,hash)>0)return null;
            var account=identity.bootstrap(data.cuenta());if(data.lectura())access.cambiar(account.id(),account.id(),"LECTURA",true);if(data.operacion())access.cambiar(account.id(),account.id(),"OPERACION",true);
            db.sql.update("INSERT INTO identity_event(actor_id,subject_id,event_code,resource_id,at_ms) VALUES(?,?,'SMR_BOOTSTRAP_APLICADO',?,?)",account.id(),account.id(),hash,System.currentTimeMillis());return null;
        });
        Files.move(p,done,StandardCopyOption.ATOMIC_MOVE);
        Files.writeString(done,"{\"applied\":true}");
    };}
}
