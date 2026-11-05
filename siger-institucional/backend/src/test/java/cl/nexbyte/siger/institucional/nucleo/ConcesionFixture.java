package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.identidad.*;
import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

/** Solo src/test: evidencias y firmante sintéticos. Toda concesión atraviesa solicitud/aprobación/ejecución reales. */
public final class ConcesionFixture {
    private static final Map<BaseJdbc,Contexto> CONTEXTOS=Collections.synchronizedMap(new WeakHashMap<>());
    public static final class Contexto {
        public final AutoridadesInstitucionales autoridades;
        public final ConcesionesInstitucionales concesiones;
        public final String revisor,claveRevisor;
        final BaseJdbc db;final IdentidadInstitucional ids;final Clock clock;final KeyPair firma;final tools.jackson.databind.json.JsonMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
        final Map<String,AutoridadesInstitucionales.Mandato> mandatos=new LinkedHashMap<>();
        Set<String> actores=Set.of();
        Contexto(BaseJdbc db,IdentidadInstitucional ids,String ti,Clock clock)throws Exception{
            this.db=db;this.ids=ids;this.clock=clock;this.firma=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            habilitar(ids,ti);
            String temporal=UUID.randomUUID()+"AZ";var cuenta=ids.crear(ti,new IdentidadInstitucional.Crear("zz.autoridad.fixture","Autoridad sintética de pruebas","autoridad.fixture@example.invalid","Sin facultad por cargo",null,null,temporal));
            claveRevisor=UUID.randomUUID()+"AZ";ids.cambiarClavePersonal((IdentidadInstitucional.Principal)ids.autenticar(cuenta.usuario()),new IdentidadInstitucional.CambioClave(temporal,claveRevisor));revisor=cuenta.id();
            autoridades=new AutoridadesInstitucionales(db,ids,json,clock,Map.of("fixture-autoridad",firma.getPublic()));concesiones=new ConcesionesInstitucionales(db,ids,autoridades,json,clock);
        }
        public synchronized void sincronizar(String ti,IdentidadInstitucional.Otorgar grant){
            ids.validarConcesion(grant);
            var cuentas=ids.cuentas(ti).stream().filter(IdentidadInstitucional.Cuenta::activo).toList();Set<String> actuales=new HashSet<>();cuentas.forEach(c->actuales.add(c.id()));
            String key=UUID.nameUUIDFromBytes((grant.rol()+"|"+grant.alcance()+"|"+grant.unidad()+"|"+grant.sede()).getBytes(StandardCharsets.UTF_8)).toString();
            boolean nueva=!mandatos.containsKey(key);Instant end=Instant.parse("2099-01-01T00:00:00Z"),start=Instant.parse("2000-01-01T00:00:00Z");
            mandatos.putIfAbsent(key,new AutoridadesInstitucionales.Mandato(key,"p-"+revisor,"SINTETICA_PRUEBAS",grant.rol(),grant.alcance(),grant.unidad(),grant.sede(),start,end,true,true,"EVIDENCIA_SINTETICA","a".repeat(64)));
            if(!nueva&&actores.equals(actuales))return;
            var bindings=cuentas.stream().map(c->new AutoridadesInstitucionales.Vinculo(c.id(),"p-"+c.id(),start,end,huella(db,c.id()))).toList();
            long next=db.sql.queryForObject("SELECT revision FROM authority_registry WHERE id=1",Long.class)+1;
            byte[] payload=json.writeValueAsBytes(new AutoridadesInstitucionales.Registro("1.0",next,clock.instant(),end,bindings,List.copyOf(mandatos.values())));
            try{var signer=Signature.getInstance("Ed25519");signer.initSign(firma.getPrivate());signer.update(AutoridadesInstitucionales.contenidoFirmado(payload));autoridades.incorporar(new AutoridadesInstitucionales.Sobre("fixture-autoridad",Base64.getEncoder().encodeToString(payload),Base64.getEncoder().encodeToString(signer.sign())));}
            catch(GeneralSecurityException e){throw new IllegalStateException(e);}
            actores=Set.copyOf(actuales);
        }
    }
    static void habilitar(IdentidadInstitucional ids,String actor){
        var cuenta=ids.operador(actor);var p=(IdentidadInstitucional.Principal)ids.autenticar(ids.cuentas(actor).stream().filter(a->a.id().equals(actor)).findFirst().orElseThrow().usuario());
        if(p.cambioClavePendiente()){
            String temporal=UUID.randomUUID()+"AZ";ids.clave(actor,actor,new IdentidadInstitucional.Clave(temporal,cuenta.versionAcceso()));
            p=(IdentidadInstitucional.Principal)ids.autenticar(ids.cuentas(actor).stream().filter(a->a.id().equals(actor)).findFirst().orElseThrow().usuario());ids.cambiarClavePersonal(p,new IdentidadInstitucional.CambioClave(temporal,UUID.randomUUID()+"AZ"));
        }
    }
    public static Contexto contexto(BaseJdbc db,IdentidadInstitucional ids,String ti,Clock clock){
        synchronized(CONTEXTOS){var old=CONTEXTOS.get(db);if(old!=null)return old;try{var created=new Contexto(db,ids,ti,clock);CONTEXTOS.put(db,created);return created;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    }
    static void cerrar(BaseJdbc db){CONTEXTOS.remove(db);}
    static String huella(BaseJdbc db,String actor){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(db.sql.queryForObject("SELECT password_hash FROM staff_account WHERE actor_id=?",String.class,actor).getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    public static String otorgar(BaseJdbc db,IdentidadInstitucional ids,String ti,String target,IdentidadInstitucional.Otorgar grant,Clock clock){
        var prepared=preparar(db,ids,ti,target,grant,clock);var c=contexto(db,ids,ti,clock);c.concesiones.ejecutar(ti,UUID.randomUUID(),prepared,new ConcesionesInstitucionales.Ejecucion(1));return db.sql.queryForObject("SELECT assignment_id FROM access_instruction WHERE id=?",String.class,prepared.toString());
    }
    public static UUID preparar(BaseJdbc db,IdentidadInstitucional ids,String ti,String target,IdentidadInstitucional.Otorgar grant,Clock clock){
        var c=contexto(db,ids,ti,clock);c.sincronizar(ti,grant);
        var until=grant.hasta()==null?Instant.parse("2080-01-01T00:00:00Z"):grant.hasta();
        var request=c.concesiones.solicitar(ti,UUID.randomUUID(),new ConcesionesInstitucionales.Solicitud(target,grant.rol(),grant.alcance(),grant.unidad(),grant.sede(),grant.desde(),until,"Instrucción sintética de regresión"));
        c.concesiones.decidir(c.revisor,UUID.randomUUID(),request.id(),new ConcesionesInstitucionales.Decision(0,"Aprobación sintética independiente"),true);return request.id();
    }
    public static void revocar(BaseJdbc db,IdentidadInstitucional ids,String ti,String target,String assignment,Clock clock){
        var links=db.sql.queryForList("SELECT instruction_id FROM approved_assignment WHERE assignment_id=?",assignment);
        if(links.isEmpty()){ids.revocar(ti,target,assignment);return;}
        var c=contexto(db,ids,ti,clock);var id=UUID.fromString(links.getFirst().get("instruction_id").toString());long revision=db.sql.queryForObject("SELECT revision FROM access_instruction WHERE id=?",Long.class,id.toString());c.concesiones.revocar(c.revisor,UUID.randomUUID(),id,new ConcesionesInstitucionales.Decision(revision,"Revocación sintética independiente"));
    }
}
