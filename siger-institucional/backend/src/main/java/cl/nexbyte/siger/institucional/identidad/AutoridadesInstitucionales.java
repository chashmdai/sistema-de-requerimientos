package cl.nexbyte.siger.institucional.identidad;

import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Raíz de autoridad separada de los roles administrados por TI. Sin confianza explícita no admite importaciones. */
public final class AutoridadesInstitucionales {
    public record Vinculo(String actor,String persona,Instant desde,Instant hasta,String autenticacion){@Override public String toString(){return "VinculoAutoridad[reservado]";}}
    public record Mandato(String id,String persona,String funcion,String rol,String alcance,String unidad,String sede,
                          Instant desde,Instant hasta,boolean aprobar,boolean revocar,String referencia,String sha256){}
    public record Registro(String version,long revision,Instant emitida,Instant expira,List<Vinculo> vinculos,List<Mandato> mandatos){@Override public String toString(){return "RegistroAutoridad[reservado]";}}
    public record Area(String id,String unidad,String sede,String nombre,String jefatura,Instant desde,Instant hasta){}
    public record AmbitoMandato(String mandato,String area){}
    public record CambioArea(String id,String persona,String tipo,String unidad,String sede,String origen,String destino,Instant desde,Instant hasta,String referencia,String sha256){}
    public record Registro11(String version,long revision,Instant emitida,Instant expira,List<Vinculo> vinculos,List<Mandato> mandatos,List<Area> areas,List<AmbitoMandato> ambitos,List<CambioArea> cambiosArea){@Override public String toString(){return "RegistroAutoridad11[reservado]";}}
    public record AmbitoMateria(String mandato,String materia){}
    public record PrincipalReq(UUID requerimiento,String unidad,String sede,String area,Instant desde,Instant hasta,String estado,String referencia,String sha256){}
    public record Registro13(String version,long revision,Instant emitida,Instant expira,List<Vinculo> vinculos,List<Mandato> mandatos,List<Area> areas,List<AmbitoMandato> ambitos,List<CambioArea> cambiosArea,List<CatalogoMaterias.Materia> materias,List<CatalogoMaterias.SedeRegional> sedesRegionales,List<AmbitoMateria> ambitosMateria,List<PrincipalReq> principales){@Override public String toString(){return "RegistroAutoridad13[reservado]";}}
    public record Registro12(String version,long revision,Instant emitida,Instant expira,List<Vinculo> vinculos,List<Mandato> mandatos,List<Area> areas,List<AmbitoMandato> ambitos,List<CambioArea> cambiosArea,List<CatalogoMaterias.Materia> materias,List<CatalogoMaterias.SedeRegional> sedesRegionales,List<AmbitoMateria> ambitosMateria){@Override public String toString(){return "RegistroAutoridad12[reservado]";}}
    public record AmbitoFuncion(String rol,String alcance,String unidad,String sede,String area){}
    public record Funcion(String id,String codigoFuncion,String ruta,String autoriza,String sede,String titular,List<String> subrogantes,List<String> autoridades,List<AmbitoFuncion> ambitos,Instant desde,Instant hasta,String referencia,String sha256,String estado){@Override public String toString(){return "DesignacionFuncional[reservada]";}}
    public record Registro14(String version,long revision,Instant emitida,Instant expira,List<Vinculo> vinculos,List<Mandato> mandatos,List<Area> areas,List<AmbitoMandato> ambitos,List<CambioArea> cambiosArea,List<CatalogoMaterias.Materia> materias,List<CatalogoMaterias.SedeRegional> sedesRegionales,List<AmbitoMateria> ambitosMateria,List<PrincipalReq> principales,List<Funcion> funciones){@Override public String toString(){return "RegistroAutoridad14[reservado]";}}
    public record ContextoFunciones(String persona,List<Funcion> funciones,long revision){}
    private record Leido(Registro base,List<Area> areas,Map<String,String> ambitos,List<CambioArea> cambios,List<CatalogoMaterias.Materia> materias,List<CatalogoMaterias.SedeRegional> sedes,Map<String,String> ambitosMateria,List<Funcion> funciones){
        Leido(Registro r,List<Area> a,Map<String,String> m,List<CambioArea> c){this(r,a,m,c,List.of(),List.of(),Map.of(),List.of());}
        Leido(Registro r,List<Area> a,Map<String,String> m,List<CambioArea> c,List<CatalogoMaterias.Materia> mat,List<CatalogoMaterias.SedeRegional> s,Map<String,String> am){this(r,a,m,c,mat,s,am,List.of());}
    }
    public record Sobre(String keyId,String payload,String firma){@Override public String toString(){return "SobreAutoridad[reservado]";}}
    private static final byte[] DOMAIN="SIGER_AUTORIDAD_1\0".getBytes(StandardCharsets.UTF_8);
    private final BaseJdbc db;
    private final IdentidadInstitucional ids;
    private final JsonMapper json;
    private final Clock clock;
    private final Map<String,PublicKey> trust;
    public static JsonMapper lectorPrivado(){return JsonMapper.builder(tools.jackson.core.json.JsonFactory.builder()
        .enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION)
        .streamReadConstraints(tools.jackson.core.StreamReadConstraints.builder().maxNestingDepth(12).maxStringLength(44000).maxNumberLength(30).build()).build()).enable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();}
    public AutoridadesInstitucionales(BaseJdbc db,IdentidadInstitucional ids,JsonMapper json,Clock clock,Map<String,PublicKey> trust){
        this.db=db;this.ids=ids;this.json=json;this.clock=clock;this.trust=Map.copyOf(trust);
        actualizarFunciones();
    }
    public SubroganciasInstitucionales subrogancias(){return new SubroganciasInstitucionales(db,ids,this,json,clock);}
    private void actualizarFunciones(){
        try{ids.funciones(db.sql.queryForObject("SELECT COUNT(*) FROM institution_function_scope",Long.class)>0?subrogancias():null);}
        catch(org.springframework.jdbc.BadSqlGrammarException e){
            // Las pruebas de actualización pueden construir identidad antes de V15.12.
            // Ningún registro 1.4 es aceptable sin sus tablas.
            String state=e.getSQLException().getSQLState();if(state==null||!state.startsWith("42"))throw e;
        }
    }
    static String str(Map<String,Object> r,String key){return r.get(key)==null?null:r.get(key).toString();}
    static long num(Map<String,Object> r,String key){return ((Number)r.get(key)).longValue();}
    static Map<String,Object> salida(Map<String,Object> row){var result=new LinkedHashMap<String,Object>();row.forEach((k,v)->result.put(k.toLowerCase(Locale.ROOT),v));return result;}
    static void texto(String value,int max){if(value==null||value.isBlank()||value.length()>max||value.chars().anyMatch(Character::isISOControl))throw new FalloTecnico(400);}
    static void codigo(String value){if(value==null||!value.matches("[A-Za-z0-9_.-]{1,64}"))throw new FalloTecnico(400);}
    static String sha(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    public static byte[] contenidoFirmado(byte[] payload){byte[] result=new byte[DOMAIN.length+payload.length];System.arraycopy(DOMAIN,0,result,0,DOMAIN.length);System.arraycopy(payload,0,result,DOMAIN.length,payload.length);return result;}
    void guard(){db.sql.queryForObject("SELECT id FROM identity_guard WHERE id=1 FOR UPDATE",Integer.class);}
    long revision(){return db.sql.queryForObject("SELECT revision FROM authority_registry WHERE id=1",Long.class);}
    void evento(String actor,String resource,String action,String code,Object snapshot){
        db.sql.update("INSERT INTO authority_event(actor_id,resource_id,action_code,result_code,at_ms,snapshot_doc) VALUES(?,?,?,?,?,?)",actor,resource,action,code,clock.millis(),snapshot==null?null:json.writeValueAsString(snapshot));
    }
    /** Solo canal privado/configuración; no hay endpoint HTTP que configure confianza o importe designaciones. */
    public long incorporar(Sobre sobre){
        if(sobre==null||sobre.payload()==null||sobre.payload().length()>44000||sobre.firma()==null||sobre.firma().length()>128)throw new FalloTecnico(400);
        codigo(sobre.keyId());var key=trust.get(sobre.keyId());if(key==null)throw new FalloTecnico(403,"FIRMANTE_AUTORIDAD_NO_CONFIABLE");
        byte[] bytes;
        Registro registro;Leido ampliado;
        try{
            bytes=Base64.getDecoder().decode(sobre.payload());if(bytes.length>32768)throw new FalloTecnico(400);
            var verifier=Signature.getInstance("Ed25519");verifier.initVerify(key);verifier.update(contenidoFirmado(bytes));
            if(!verifier.verify(Base64.getDecoder().decode(sobre.firma())))throw new FalloTecnico(403,"FIRMA_AUTORIDAD_INVALIDA");
            ampliado=decodificar(bytes);registro=ampliado.base();
        }catch(FalloTecnico e){throw e;}catch(GeneralSecurityException|RuntimeException e){throw new FalloTecnico(400);}
        validar(registro);String hash=sha(bytes);
        long installed=db.transaction(()->{
            guard();long current=revision();
            if(registro.revision()==current){var old=db.sql.queryForMap("SELECT payload_sha256 FROM authority_batch WHERE revision=?",current);if(hash.equals(old.get("payload_sha256")))return current;throw new FalloTecnico(409,"REGISTRO_AUTORIDAD_CONFLICTIVO");}
            if(registro.revision()!=current+1)throw new FalloTecnico(409,"VERSION_OBSOLETA");
            for(var v:registro.vinculos()){
                ids.operador(v.actor());
                if(db.sql.queryForObject("SELECT COUNT(*) FROM authority_binding WHERE actor_id=? AND person_id<>?",Long.class,v.actor(),v.persona())>0)throw new FalloTecnico(409,"IDENTIDAD_NO_REASIGNABLE");
            }
            db.sql.update("INSERT INTO authority_batch VALUES(?,?,?,?,?,?,?,?)",registro.revision(),hash,sobre.keyId(),sobre.payload(),sobre.firma(),registro.emitida().toEpochMilli(),registro.expira().toEpochMilli(),clock.millis());
            for(var v:registro.vinculos())db.sql.update("INSERT INTO authority_binding VALUES(?,?,?,?,?)",registro.revision(),v.actor(),v.persona(),v.desde().toEpochMilli(),v.hasta().toEpochMilli());
            for(var area:ampliado.areas()){
                var previous=db.sql.queryForList("SELECT * FROM institution_area WHERE id=?",area.id());
                if(!previous.isEmpty()&&(!area.unidad().equals(previous.getFirst().get("unit_code"))||!area.sede().equals(previous.getFirst().get("site_id"))))throw new FalloTecnico(409,"AREA_NO_REASIGNABLE");
                if(previous.isEmpty())db.sql.update("INSERT INTO institution_area VALUES(?,?,?,?,?,?,?,?)",area.id(),area.unidad(),area.sede(),area.nombre(),area.jefatura(),area.desde().toEpochMilli(),area.hasta().toEpochMilli(),registro.revision());
                else{boolean securityChange=!area.jefatura().equals(previous.getFirst().get("head_mode"))||area.desde().toEpochMilli()!=num(previous.getFirst(),"valid_from")||area.hasta().toEpochMilli()!=num(previous.getFirst(),"valid_until");db.sql.update("UPDATE institution_area SET label=?,head_mode=?,valid_from=?,valid_until=?,authority_revision=? WHERE id=?",area.nombre(),area.jefatura(),area.desde().toEpochMilli(),area.hasta().toEpochMilli(),registro.revision(),area.id());if(securityChange)db.sql.update("UPDATE staff_account SET security_epoch=security_epoch+1 WHERE actor_id IN (SELECT a.actor_id FROM staff_assignment a JOIN staff_assignment_area s ON s.assignment_id=a.id WHERE s.area_id=?)",area.id());}
            }
            importarMaterias(ampliado);
            importarPrincipales(bytes,registro);
            subrogancias().registrarAmbitos(ampliado.funciones(),registro.revision());
            // Instalar el resolver antes de liberar identity_guard: no hay una ventana
            // entre COMMIT del primer ámbito y activación de su política en memoria.
            // Si esta transacción revierte, el resolver observa cero ámbitos y conserva la compatibilidad.
            actualizarFunciones();
            for(var cambio:ampliado.cambios()){
                var catalogo=new CatalogoAreas(db,clock);var destino=catalogo.exigir(cambio.destino(),cambio.unidad(),cambio.sede());
                if(cambio.desde().isBefore(destino.desde())||cambio.hasta().isAfter(destino.hasta()))throw new FalloTecnico(400);
                if(cambio.origen()!=null){var origen=catalogo.exigir(cambio.origen(),cambio.unidad(),cambio.sede());if(cambio.desde().isBefore(origen.desde())||cambio.hasta().isAfter(origen.hasta()))throw new FalloTecnico(400);}
            }
            for(var m:registro.mandatos()){
                String area=ampliado.ambitos().get(m.id()),materia=ampliado.ambitosMateria().get(m.id());var grant=new IdentidadInstitucional.Otorgar(m.rol(),m.alcance(),m.unidad(),m.sede(),m.desde(),m.hasta(),area,materia);if(materia!=null&&!m.aprobar()&&m.revocar())ids.validarRevocacionMateria(grant);else ids.validarConcesion(grant);
                if(registro.vinculos().stream().noneMatch(v->v.persona().equals(m.persona())))throw new FalloTecnico(400);
                db.sql.update("INSERT INTO authority_mandate VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",registro.revision(),m.id(),m.persona(),m.funcion(),m.rol(),m.alcance(),m.unidad(),m.sede(),m.desde().toEpochMilli(),m.hasta().toEpochMilli(),m.aprobar(),m.revocar(),m.referencia(),m.sha256());
                if(area!=null)db.sql.update("INSERT INTO authority_area_mandate VALUES(?,?,?)",registro.revision(),m.id(),area);if(materia!=null)db.sql.update("INSERT INTO authority_matter_mandate VALUES(?,?,?)",registro.revision(),m.id(),materia);
            }
            db.sql.update("UPDATE authority_registry SET revision=? WHERE id=1",registro.revision());evento(null,Long.toString(registro.revision()),"REGISTRO_IMPORTADO","FIRMA_VERIFICADA",Map.of("sha256",hash,"keyId",sobre.keyId()));return registro.revision();
        });
        actualizarFunciones();return installed;
    }
    private void validar(Registro r){
        if(r==null||!Set.of("1.0","1.1","1.2","1.3","1.4").contains(r.version())||r.revision()<1||r.emitida()==null||r.expira()==null||r.emitida().isAfter(clock.instant())||!r.expira().isAfter(clock.instant())||r.vinculos()==null||r.mandatos()==null||r.vinculos().size()>128||r.mandatos().size()>32)throw new FalloTecnico(400);
        intervalo(r.emitida(),r.expira(),r.expira());Set<String> actors=new HashSet<>(),mandates=new HashSet<>();
        for(var v:r.vinculos()){
            if(v==null)throw new FalloTecnico(400);codigo(v.actor());codigo(v.persona());intervalo(v.desde(),v.hasta(),r.expira());if(v.autenticacion()==null||!v.autenticacion().matches("[a-f0-9]{64}")||!actors.add(v.actor()))throw new FalloTecnico(400);
        }
        for(var m:r.mandatos()){
            if(m==null)throw new FalloTecnico(400);codigo(m.id());codigo(m.persona());texto(m.funcion(),80);texto(m.referencia(),180);
            if(m.sha256()==null||!m.sha256().matches("[a-f0-9]{64}")||!mandates.add(m.id())||!m.aprobar()&&!m.revocar())throw new FalloTecnico(400);
            intervalo(m.desde(),m.hasta(),r.expira());
        }
    }
    private Leido decodificar(byte[] bytes){
        var reader=lectorPrivado();String version=reader.readTree(bytes).path("version").asString();
        if(version.equals("1.0")){var r=reader.readValue(bytes,Registro.class);validar(r);if(r.mandatos().stream().anyMatch(m->Set.of("AREA","MATERIA").contains(m.alcance())))throw new FalloTecnico(400);return new Leido(r,List.of(),Map.of(),List.of());}
        if(version.equals("1.2"))return decodificar12(reader.readValue(bytes,Registro12.class));
        if(version.equals("1.4")){
            var r=reader.readValue(bytes,Registro14.class);
            validarPrincipales(new Registro13(r.version(),r.revision(),r.emitida(),r.expira(),r.vinculos(),r.mandatos(),r.areas(),r.ambitos(),r.cambiosArea(),r.materias(),r.sedesRegionales(),r.ambitosMateria(),r.principales()));
            var base=decodificar12(new Registro12(r.version(),r.revision(),r.emitida(),r.expira(),r.vinculos(),r.mandatos(),r.areas(),r.ambitos(),r.cambiosArea(),r.materias(),r.sedesRegionales(),r.ambitosMateria()));
            validarFunciones(r);
            return new Leido(base.base(),base.areas(),base.ambitos(),base.cambios(),base.materias(),base.sedes(),base.ambitosMateria(),r.funciones());
        }
        if(version.equals("1.3")){var r=reader.readValue(bytes,Registro13.class);validarPrincipales(r);return decodificar12(new Registro12(r.version(),r.revision(),r.emitida(),r.expira(),r.vinculos(),r.mandatos(),r.areas(),r.ambitos(),r.cambiosArea(),r.materias(),r.sedesRegionales(),r.ambitosMateria()));}
        if(!version.equals("1.1"))throw new FalloTecnico(400);var r=reader.readValue(bytes,Registro11.class);var result=decodificar11(r);if(r.mandatos().stream().anyMatch(m->m.alcance().equals("MATERIA")))throw new FalloTecnico(400);return result;
    }
    private Leido decodificar11(Registro11 r){var base=new Registro(r.version(),r.revision(),r.emitida(),r.expira(),r.vinculos(),r.mandatos());validar(base);
        if(r.areas()==null||r.areas().size()>32||r.ambitos()==null||r.ambitos().size()>32)throw new FalloTecnico(400);var areaIds=new HashSet<String>();var scopes=new HashMap<String,String>();
        for(var a:r.areas()){codigo(a.id());codigo(a.unidad());codigo(a.sede());texto(a.nombre(),120);intervalo(a.desde(),a.hasta(),r.expira());if(!Set.of("COORDINACION","DIRECCION").contains(a.jefatura())||!areaIds.add(a.id()))throw new FalloTecnico(400);}
        for(var a:r.ambitos()){codigo(a.mandato());codigo(a.area());if(scopes.putIfAbsent(a.mandato(),a.area())!=null||r.mandatos().stream().noneMatch(m->m.id().equals(a.mandato())&&m.alcance().equals("AREA")))throw new FalloTecnico(400);}
        if(r.mandatos().stream().anyMatch(m->m.alcance().equals("AREA")&&!scopes.containsKey(m.id()))||r.cambiosArea()==null||r.cambiosArea().size()>32)throw new FalloTecnico(400);
        var changes=new HashSet<String>();for(var c:r.cambiosArea()){codigo(c.id());codigo(c.persona());codigo(c.unidad());codigo(c.sede());if(c.origen()!=null)codigo(c.origen());codigo(c.destino());texto(c.referencia(),180);intervalo(c.desde(),c.hasta(),r.expira());if(!Set.of("DIRECCION","DELEGACION_EXPRESA").contains(c.tipo())||c.sha256()==null||!c.sha256().matches("[a-f0-9]{64}")||!changes.add(c.id())||Objects.equals(c.origen(),c.destino())||r.vinculos().stream().noneMatch(v->v.persona().equals(c.persona())))throw new FalloTecnico(400);}
        return new Leido(base,r.areas(),Map.copyOf(scopes),r.cambiosArea());
    }
    private Leido decodificar12(Registro12 r){
        var common=decodificar11(new Registro11(r.version(),r.revision(),r.emitida(),r.expira(),r.vinculos(),r.mandatos(),r.areas(),r.ambitos(),r.cambiosArea()));
        if(r.materias()==null||r.sedesRegionales()==null||r.ambitosMateria()==null||r.materias().size()>32||r.sedesRegionales().size()>32||r.ambitosMateria().size()>32)throw new FalloTecnico(400);
        var seen=new HashSet<String>();for(var m:r.materias()){codigo(m.id());texto(m.nombre(),120);if(!Set.of("UPR","UPD").contains(m.competencia())||!Set.of("ACTIVA","SUSPENDIDA").contains(m.estado())||!seen.add(m.id()))throw new FalloTecnico(400);intervalo(m.desde(),m.hasta(),r.expira());}
        seen.clear();for(var s:r.sedesRegionales()){codigo(s.id());if(!Set.of("ACTIVA","SUSPENDIDA").contains(s.estado())||!seen.add(s.id()))throw new FalloTecnico(400);intervalo(s.desde(),s.hasta(),r.expira());}
        var scopes=new HashMap<String,String>();for(var a:r.ambitosMateria()){codigo(a.mandato());codigo(a.materia());if(scopes.putIfAbsent(a.mandato(),a.materia())!=null||r.mandatos().stream().noneMatch(m->m.id().equals(a.mandato())&&m.alcance().equals("MATERIA")))throw new FalloTecnico(400);}
        if(r.mandatos().stream().anyMatch(m->m.alcance().equals("MATERIA")&&!scopes.containsKey(m.id())))throw new FalloTecnico(400);
        return new Leido(common.base(),common.areas(),common.ambitos(),common.cambios(),r.materias(),r.sedesRegionales(),Map.copyOf(scopes));
    }
    private void validarFunciones(Registro14 r){
        if(r.funciones()==null||r.funciones().size()>32)throw new FalloTecnico(400);
        Set<String> idsFuncion=new HashSet<>();
        for(var f:r.funciones()){
            if(f==null)throw new FalloTecnico(400);codigo(f.id());codigo(f.codigoFuncion());codigo(f.sede());codigo(f.titular());
            texto(f.referencia(),180);intervalo(f.desde(),f.hasta(),r.expira());
            if(!idsFuncion.add(f.id())||!Set.of("REGIONAL","CENTRAL").contains(f.ruta())||!Set.of("ACTIVA","SUSPENDIDA").contains(f.estado())||f.sha256()==null||!f.sha256().matches("[a-f0-9]{64}")||f.subrogantes()==null||f.subrogantes().isEmpty()||f.subrogantes().size()>32||f.autoridades()==null||f.autoridades().isEmpty()||f.autoridades().size()>32||f.ambitos()==null||f.ambitos().isEmpty()||f.ambitos().size()>32)throw new FalloTecnico(400);
            if(!Objects.equals(f.autoriza(),f.ruta().equals("REGIONAL")?"JEFATURA_SEDES_REGIONALES":"JEFATURA_GESTION"))throw new FalloTecnico(400);
            boolean regional=r.sedesRegionales().stream().anyMatch(s->s.id().equals(f.sede())&&s.estado().equals("ACTIVA")&&!s.desde().isAfter(f.desde())&&!s.hasta().isBefore(f.hasta()));
            if(f.ruta().equals("REGIONAL")!=regional)throw new FalloTecnico(400,"RUTA_FUNCIONAL_NO_ACREDITADA");
            var personas=new HashSet<String>();personas.add(f.titular());
            for(var p:f.subrogantes()){codigo(p);if(!personas.add(p))throw new FalloTecnico(400);}
            for(var p:f.autoridades()){codigo(p);if(!personas.add(p))throw new FalloTecnico(400);}
            for(var p:personas)if(r.vinculos().stream().noneMatch(v->v.persona().equals(p)))throw new FalloTecnico(400,"PERSONA_FUNCIONAL_NO_ACREDITADA");
            var ambitos=new HashSet<AmbitoFuncion>();
            for(var a:f.ambitos()){
                if(a==null||!Set.of("COORDINADOR","COORDINADOR_REGIONAL","DIRECTOR").contains(a.rol())||!f.sede().equals(a.sede())||!ambitos.add(a))throw new FalloTecnico(400);
                if(f.ruta().equals("REGIONAL")&&a.rol().equals("DIRECTOR"))throw new FalloTecnico(400);
                if(a.area()!=null){
                    if(!a.alcance().equals("AREA")||a.rol().equals("COORDINADOR_REGIONAL")||r.areas().stream().noneMatch(ar->ar.id().equals(a.area())&&ar.unidad().equals(a.unidad())&&ar.sede().equals(a.sede())&&!ar.desde().isAfter(f.desde())&&!ar.hasta().isBefore(f.hasta())&&(!a.rol().equals("DIRECTOR")||ar.jefatura().equals("DIRECCION"))))throw new FalloTecnico(400);
                }else if(a.rol().equals("COORDINADOR_REGIONAL")){
                    if(!a.alcance().equals("SEDE")||a.unidad()!=null||!f.ruta().equals("REGIONAL"))throw new FalloTecnico(400);
                }else if(!a.alcance().equals("UNIDAD_SEDE")||a.unidad()==null)throw new FalloTecnico(400);
            }
        }
        for(var f:r.funciones())for(var g:r.funciones())if(f!=g&&f.estado().equals("ACTIVA")&&g.estado().equals("ACTIVA")&&f.desde().isBefore(g.hasta())&&g.desde().isBefore(f.hasta())&&(f.codigoFuncion().equals(g.codigoFuncion())||f.ambitos().stream().anyMatch(a->g.ambitos().stream().anyMatch(b->Objects.equals(a.unidad(),b.unidad())&&Objects.equals(a.sede(),b.sede())&&Objects.equals(a.area(),b.area())))))throw new FalloTecnico(409,"FUNCIONES_SOLAPADAS");
    }
    public ContextoFunciones contextoFunciones(String actor){
        ids.exigirCuentaHabilitada(actor);var r=vigenteCompleto();String persona=persona(actor,r.base());
        String fingerprint=sha(db.sql.queryForObject("SELECT password_hash FROM staff_account WHERE actor_id=?",String.class,actor).getBytes(StandardCharsets.UTF_8));
        if(r.base().vinculos().stream().noneMatch(v->v.actor().equals(actor)&&v.autenticacion().equals(fingerprint)))throw new FalloTecnico(409,"ACREDITACION_CREDENCIAL_OBSOLETA");
        return new ContextoFunciones(persona,r.funciones(),r.base().revision());
    }
    private void validarPrincipales(Registro13 r){
        if(r.principales()==null||r.principales().size()>32)throw new FalloTecnico(400);var seen=new HashSet<UUID>();
        for(var p:r.principales()){if(p==null||p.requerimiento()==null||!seen.add(p.requerimiento())||!Set.of("ACTIVA","SUSPENDIDA").contains(p.estado())||p.sha256()==null||!p.sha256().matches("[a-f0-9]{64}"))throw new FalloTecnico(400);codigo(p.unidad());codigo(p.sede());if(p.area()!=null)codigo(p.area());texto(p.referencia(),180);intervalo(p.desde(),p.hasta(),r.expira());}
    }
    private void importarPrincipales(byte[] bytes,Registro base){
        if(!Set.of("1.3","1.4").contains(base.version()))return;
        var principales=base.version().equals("1.4")?lectorPrivado().readValue(bytes,Registro14.class).principales():lectorPrivado().readValue(bytes,Registro13.class).principales();
        for(var p:principales){
            if(db.sql.queryForObject("SELECT COUNT(*) FROM core_requirement WHERE id=?",Long.class,p.requerimiento().toString())!=1)throw new FalloTecnico(409,"REQ_NO_ACREDITABLE");
            var old=db.sql.queryForList("SELECT unit_code,site_id,area_id FROM authority_requirement_principal WHERE requirement_id=? ORDER BY batch_revision DESC LIMIT 1",p.requerimiento().toString());
            if(!old.isEmpty()&&(!p.unidad().equals(old.getFirst().get("unit_code"))||!p.sede().equals(old.getFirst().get("site_id"))||!Objects.equals(p.area(),old.getFirst().get("area_id"))))throw new FalloTecnico(409,"TRANSFERENCIA_REQ_REQUIERE_ACEPTACION");
            if(p.estado().equals("ACTIVA")){
                ids.validarConcesion(new IdentidadInstitucional.Otorgar(p.area()==null?"JEFATURA":"COORDINADOR",p.area()==null?"UNIDAD_SEDE":"AREA",p.unidad(),p.sede(),p.desde(),p.hasta(),p.area()));
                if(p.area()==null&&db.sql.queryForObject("SELECT COUNT(*) FROM institution_area WHERE unit_code=? AND site_id=?",Long.class,p.unidad(),p.sede())>0)throw new FalloTecnico(409,"AREA_REQUERIDA");
            }
            db.sql.update("INSERT INTO authority_requirement_principal VALUES(?,?,?,?,?,?,?,?,?,?)",base.revision(),p.requerimiento().toString(),p.unidad(),p.sede(),p.area(),p.desde().toEpochMilli(),p.hasta().toEpochMilli(),p.estado(),p.referencia(),p.sha256());
        }
    }
    private void importarMaterias(Leido r){
        for(var m:r.materias()){
            var old=db.sql.queryForList("SELECT * FROM institution_matter WHERE id=?",m.id());if(!old.isEmpty()&&!m.competencia().equals(old.getFirst().get("functional_unit")))throw new FalloTecnico(409,"COMPETENCIA_NO_REASIGNABLE");
            if(old.isEmpty())db.sql.update("INSERT INTO institution_matter VALUES(?,?,?,?,?,?,?)",m.id(),m.nombre(),m.competencia(),m.desde().toEpochMilli(),m.hasta().toEpochMilli(),m.estado(),r.base().revision());
            else{var o=old.getFirst();boolean changed=!m.estado().equals(o.get("status"))||m.desde().toEpochMilli()!=num(o,"valid_from")||m.hasta().toEpochMilli()!=num(o,"valid_until");db.sql.update("UPDATE institution_matter SET label=?,valid_from=?,valid_until=?,status=?,authority_revision=? WHERE id=?",m.nombre(),m.desde().toEpochMilli(),m.hasta().toEpochMilli(),m.estado(),r.base().revision(),m.id());if(changed)db.sql.update("UPDATE staff_account SET security_epoch=security_epoch+1 WHERE actor_id IN (SELECT a.actor_id FROM staff_assignment a JOIN staff_assignment_matter x ON x.assignment_id=a.id WHERE x.matter_id=?)",m.id());}
        }
        for(var s:r.sedes()){
            var old=db.sql.queryForList("SELECT * FROM matter_regional_site WHERE site_id=?",s.id());
            if(old.isEmpty())db.sql.update("INSERT INTO matter_regional_site VALUES(?,?,?,?,?)",s.id(),s.desde().toEpochMilli(),s.hasta().toEpochMilli(),s.estado(),r.base().revision());
            else{var o=old.getFirst();boolean changed=!s.estado().equals(o.get("status"))||s.desde().toEpochMilli()!=num(o,"valid_from")||s.hasta().toEpochMilli()!=num(o,"valid_until");db.sql.update("UPDATE matter_regional_site SET valid_from=?,valid_until=?,status=?,authority_revision=? WHERE site_id=?",s.desde().toEpochMilli(),s.hasta().toEpochMilli(),s.estado(),r.base().revision(),s.id());if(changed)db.sql.update("UPDATE staff_account SET security_epoch=security_epoch+1 WHERE actor_id IN (SELECT i.beneficiary_id FROM access_instruction i JOIN instruction_matter x ON x.instruction_id=i.id WHERE i.site_id=?)",s.id());}
        }
    }
    public List<CambioArea> cambiosArea(String actor){
        ids.exigirCuentaHabilitada(actor);var r=vigenteCompleto();String p=persona(actor,r.base());String fingerprint=sha(db.sql.queryForObject("SELECT password_hash FROM staff_account WHERE actor_id=?",String.class,actor).getBytes(StandardCharsets.UTF_8));
        if(r.base().vinculos().stream().noneMatch(v->v.actor().equals(actor)&&v.autenticacion().equals(fingerprint)))throw new FalloTecnico(409,"ACREDITACION_CREDENCIAL_OBSOLETA");
        return r.cambios().stream().filter(c->c.persona().equals(p)&&!c.desde().isAfter(clock.instant())&&c.hasta().isAfter(clock.instant())).toList();
    }
    public void autorizarCambioArea(String actor,String unidad,String sede,String origen,String destino,String caso,boolean auditar){
        var c=cambiosArea(actor).stream().filter(x->x.unidad().equals(unidad)&&x.sede().equals(sede)&&Objects.equals(x.origen(),origen)&&x.destino().equals(destino)).findFirst().orElseThrow(()->new FalloTecnico(403,"CAMBIO_AREA_NO_ACREDITADO"));
        if(auditar){if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Transacción requerida");evento(actor,caso,"CAMBIO_AREA_AUTORIZADO","ACREDITADO",Map.of("revisionRegistro",revision(),"facultad",c.id(),"tipo",c.tipo(),"referencia",c.referencia(),"sha256",c.sha256()));}
    }
    static void intervalo(Instant desde,Instant hasta,Instant limite){try{if(desde==null||hasta==null||!hasta.isAfter(desde)||hasta.toEpochMilli()<=desde.toEpochMilli()||hasta.isAfter(limite))throw new FalloTecnico(400);}catch(ArithmeticException e){throw new FalloTecnico(400);}}
    void actor(String actor){ids.exigirCuentaHabilitada(actor);persona(actor);}
    private Leido vigenteCompleto(){
        var rows=db.sql.queryForList("SELECT r.* FROM authority_batch r JOIN authority_registry c ON c.revision=r.revision AND c.id=1");
        try{
            if(rows.size()!=1)throw new IllegalArgumentException();var row=rows.getFirst();var key=trust.get(str(row,"key_id"));if(key==null)throw new IllegalArgumentException();
            byte[] bytes=Base64.getDecoder().decode(str(row,"signed_payload"));if(bytes.length>32768||!sha(bytes).equals(str(row,"payload_sha256")))throw new IllegalArgumentException();
            var verifier=Signature.getInstance("Ed25519");verifier.initVerify(key);verifier.update(contenidoFirmado(bytes));if(!verifier.verify(Base64.getDecoder().decode(str(row,"signature_text"))))throw new IllegalArgumentException();
            var r=decodificar(bytes);if(r.base().revision()!=num(row,"revision"))throw new IllegalArgumentException();return r;
        }catch(GeneralSecurityException|RuntimeException e){throw new FalloTecnico(409,"IDENTIDAD_INSTITUCIONAL_NO_ACREDITADA");}
    }
    private Registro vigente(){return vigenteCompleto().base();}
    private String persona(String actor,Registro r){return r.vinculos().stream().filter(v->v.actor().equals(actor)&&!v.desde().isAfter(clock.instant())&&v.hasta().isAfter(clock.instant())).map(Vinculo::persona).findFirst().orElseThrow(()->new FalloTecnico(409,"IDENTIDAD_INSTITUCIONAL_NO_ACREDITADA"));}
    public String persona(String actor){
        return persona(actor,vigente());
    }
    List<Map<String,Object>> mandatos(String actor){
        var expanded=vigenteCompleto();var r=expanded.base();String person=persona(actor,r);
        // TI administra contraseñas locales, pero no puede heredar la autoridad del funcionario al restablecerla.
        var credential=db.sql.queryForObject("SELECT password_hash FROM staff_account WHERE actor_id=?",String.class,actor);
        String fingerprint=sha(credential.getBytes(StandardCharsets.UTF_8));
        if(r.mandatos().stream().anyMatch(m->m.persona().equals(person))&&r.vinculos().stream().noneMatch(v->v.actor().equals(actor)&&v.autenticacion().equals(fingerprint)))throw new FalloTecnico(409,"ACREDITACION_CREDENCIAL_OBSOLETA");
        return r.mandatos().stream().filter(m->m.persona().equals(person)&&!m.desde().isAfter(clock.instant())&&m.hasta().isAfter(clock.instant())).sorted(Comparator.comparing(Mandato::id)).map(m->{
            Map<String,Object> row=new LinkedHashMap<>();row.put("id",m.id());row.put("batch_revision",r.revision());row.put("person_id",m.persona());row.put("function_code",m.funcion());row.put("role_code",m.rol());row.put("scope_code",m.alcance());row.put("unit_code",m.unidad());row.put("site_id",m.sede());row.put("area_id",expanded.ambitos().get(m.id()));row.put("matter_id",expanded.ambitosMateria().get(m.id()));row.put("valid_from",m.desde().toEpochMilli());row.put("valid_until",m.hasta().toEpochMilli());row.put("can_approve",m.aprobar());row.put("can_revoke",m.revocar());row.put("basis_ref",m.referencia());row.put("basis_sha256",m.sha256());return row;
        }).toList();
    }
    Map<String,Object> mandato(String actor,IdentidadInstitucional.Otorgar grant,boolean revoke){
        return mandatos(actor).stream().filter(m->Boolean.TRUE.equals(m.get(revoke?"can_revoke":"can_approve"))&&cubre(m,grant)).findFirst().orElseThrow(()->new FalloTecnico(403,"AUTORIDAD_NO_ACREDITADA"));
    }
    private boolean cubre(Map<String,Object> m,IdentidadInstitucional.Otorgar grant){
        return Objects.equals(m.get("role_code"),grant.rol())&&Objects.equals(m.get("scope_code"),grant.alcance())&&Objects.equals(m.get("unit_code"),grant.unidad())&&Objects.equals(m.get("site_id"),grant.sede())&&Objects.equals(m.get("area_id"),grant.area())&&Objects.equals(m.get("matter_id"),grant.materia())
            &&num(m,"valid_from")<=Math.max(clock.millis(),grant.desde().toEpochMilli())&&num(m,"valid_until")>=grant.hasta().toEpochMilli();
    }
    void comprobarDecision(String approver,long batch,String mandateId,IdentidadInstitucional.Otorgar grant){
        try{actor(approver);var m=mandato(approver,grant,false);
            if(batch!=revision()||!mandateId.equals(m.get("id")))throw new FalloTecnico(409,"APROBACION_NO_VIGENTE");
        }catch(FalloTecnico e){if(Set.of(401,403,404,409).contains(e.status))throw new FalloTecnico(409,"APROBACION_NO_VIGENTE");throw e;}
    }
    public Map<String,Object> contexto(String actor){
        ids.exigirCuentaHabilitada(actor);String state="ACREDITADA";List<Map<String,Object>> mandates=List.of();
        try{mandates=mandatos(actor);}catch(FalloTecnico e){if(e.status!=409)throw e;state="NO_ACREDITADA";}
        return Map.of("revisionRegistro",revision(),"identidad",state,"mandatos",mandates.stream().map(AutoridadesInstitucionales::salida).toList());
    }
}
