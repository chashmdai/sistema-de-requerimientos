package cl.nexbyte.siger.institucional.identidad;

import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.*;
import cl.nexbyte.siger.institucional.robustez.*;
import java.time.*;import java.util.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Frontera local de identidad. Las facultades se resuelven desde asignaciones vigentes, nunca desde el cargo. */
public final class IdentidadInstitucional {
    private final BaseJdbc db;private final Clock clock;private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(12);
    public IdentidadInstitucional(BaseJdbc db,Clock clock){this.db=db;this.clock=clock;}
    public record Crear(String usuario,String nombre,String correo,String cargo,String unidad,String sede,String password){
        @Override public String toString(){return "Crear[reservado]";}
    }
    public record Perfil(String nombre,String correo,String cargo,String unidad,String sede,boolean activo,long revision){}
    public record Otorgar(String rol,String alcance,String unidad,String sede,Instant desde,Instant hasta,String area){public Otorgar(String rol,String alcance,String unidad,String sede,Instant desde,Instant hasta){this(rol,alcance,unidad,sede,desde,hasta,null);}}
    public record Cuenta(String id,String usuario,String nombre,String correo,String cargo,String unidad,String sede,boolean activo,long revision,List<Map<String,Object>> asignaciones,boolean cambioClavePendiente){}
    public record Clave(String password,long revision){@Override public String toString(){return "Clave[reservada]";}}
    public record CambioClave(String actual,String nueva){@Override public String toString(){return "CambioClave[reservado]";}}
    public static final class Principal extends User {
        private final long epoch;
        private final boolean cambioClavePendiente;
        Principal(String username,String hash,long epoch,boolean cambioClavePendiente){super(username,hash,List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_FUNCIONARIO_LAB")));this.epoch=epoch;this.cambioClavePendiente=cambioClavePendiente;}
        public long epoch(){return epoch;}
        public boolean cambioClavePendiente(){return cambioClavePendiente;}
    }
    private long now(){return clock.millis();}
    private static String str(Map<String,Object> r,String k){return r.get(k)==null?null:r.get(k).toString();}
    private static long num(Map<String,Object> r,String k){return ((Number)r.get(k)).longValue();}
    private static void text(String s,int max){if(s==null||s.isBlank()||s.length()>max||s.chars().anyMatch(Character::isISOControl))throw new FalloTecnico(400);}
    private Map<String,Object> row(String id){var rows=db.sql.queryForList("SELECT s.*,a.label,c.must_change_password FROM staff_account s JOIN core_actor a ON a.id=s.actor_id LEFT JOIN staff_credential_state c ON c.actor_id=s.actor_id WHERE s.actor_id=?",id);if(rows.isEmpty())throw new FalloTecnico(404);return rows.getFirst();}
    private boolean enabled(Map<String,Object> r){return Boolean.TRUE.equals(r.get("enabled"));}
    private void guard(){db.sql.queryForObject("SELECT id FROM identity_guard WHERE id=1 FOR UPDATE",Integer.class);}
    private void evento(String a,String subject,String code,String resource){db.sql.update("INSERT INTO identity_event(actor_id,subject_id,event_code,resource_id,at_ms) VALUES(?,?,?,?,?)",a,subject,code,resource,now());}
    private void ti(String a){if(!operador(a).ti())throw new FalloTecnico(403);}
    private void revision(Map<String,Object> r,long expected){if(num(r,"security_epoch")!=expected)throw new FalloTecnico(409,"VERSION_OBSOLETA");}
    public List<Acceso> accesos(String id){
        long at=now();
        var accesses=db.sql.queryForList("SELECT a.role_code,CASE WHEN g.area_id IS NULL THEN a.scope_code ELSE 'AREA' END AS scope_code,COALESCE(ar.unit_code,a.unit_code) AS unit_code,COALESCE(ar.site_id,a.site_id) AS site_id,g.area_id FROM staff_assignment a LEFT JOIN staff_assignment_area g ON g.assignment_id=a.id LEFT JOIN institution_area ar ON ar.id=g.area_id WHERE a.actor_id=? AND a.revoked_ms IS NULL AND a.valid_from<=? AND (a.valid_until IS NULL OR a.valid_until>?) AND (g.area_id IS NULL OR EXISTS(SELECT 1 FROM approved_assignment p JOIN access_instruction i ON i.id=p.instruction_id JOIN instruction_area ia ON ia.instruction_id=i.id WHERE p.assignment_id=a.id AND i.state='EJECUTADA' AND i.beneficiary_id=a.actor_id AND i.role_code=a.role_code AND i.valid_from=a.valid_from AND i.valid_until=a.valid_until AND ia.area_id=g.area_id)) AND (g.area_id IS NULL OR EXISTS(SELECT 1 FROM institution_area ar WHERE ar.id=g.area_id AND (a.role_code<>'DIRECTOR' OR ar.head_mode='DIRECCION') AND ar.valid_from<="+at+" AND ar.valid_until>"+at+")) AND (COALESCE(ar.site_id,a.site_id) IS NULL OR EXISTS(SELECT 1 FROM org_site_validity s WHERE s.site_id=COALESCE(ar.site_id,a.site_id) AND s.valid_from<=? AND (s.valid_until IS NULL OR s.valid_until>?))) AND (COALESCE(ar.unit_code,a.unit_code) IS NULL OR EXISTS(SELECT 1 FROM org_unit_validity u WHERE u.unit_code=COALESCE(ar.unit_code,a.unit_code) AND u.valid_from<=? AND (u.valid_until IS NULL OR u.valid_until>?))) ORDER BY a.id",id,at,at,at,at,at,at).stream().map(r->new Acceso(str(r,"role_code"),str(r,"scope_code"),str(r,"unit_code"),str(r,"site_id"),str(r,"area_id"))).toList();
        // Datos anteriores pueden contener solapamientos: no elegir un titular por orden o antigüedad.
        if(accesses.stream().anyMatch(x->x.rol().equals("DISTRIBUCION"))&&!distribuidorUnico(id,at))return accesses.stream().filter(x->!x.rol().equals("DISTRIBUCION")).toList();
        return accesses;
    }
    private boolean distribuidorUnico(String id,long at){
        var current=db.sql.queryForList("SELECT a.actor_id FROM staff_assignment a JOIN staff_account s ON s.actor_id=a.actor_id WHERE s.enabled=TRUE AND a.role_code='DISTRIBUCION' AND a.scope_code='INSTITUCION' AND a.revoked_ms IS NULL AND a.valid_from<=? AND (a.valid_until IS NULL OR a.valid_until>?)",at,at);
        return current.size()==1&&id.equals(current.getFirst().get("actor_id"));
    }
    /** ORG-02/11. Llamar bajo identity_guard; solo compara el presente/futuro de intervalos [desde,hasta). */
    private void distribucionDisponible(String except,long from,Long until){
        long start=Math.max(now(),from);if(until!=null&&until<=start)return;
        var args=new ArrayList<Object>();args.add(except);args.add(start);
        String end="";if(until!=null){end=" AND a.valid_from<?";args.add(until);}
        long overlaps=db.sql.queryForObject("SELECT COUNT(*) FROM staff_assignment a JOIN staff_account s ON s.actor_id=a.actor_id WHERE s.enabled=TRUE AND a.role_code='DISTRIBUCION' AND a.scope_code='INSTITUCION' AND a.revoked_ms IS NULL AND a.id<>? AND (a.valid_until IS NULL OR a.valid_until>?)"+end,Long.class,args.toArray());
        if(overlaps>0)throw new FalloTecnico(409,"DISTRIBUIDOR_CENTRAL_SOLAPADO");
    }
    private void validarActivacionDistribuidor(String actor){
        for(var grant:db.sql.queryForList("SELECT id,valid_from,valid_until FROM staff_assignment WHERE actor_id=? AND role_code='DISTRIBUCION' AND scope_code='INSTITUCION' AND revoked_ms IS NULL",actor))
            distribucionDisponible(str(grant,"id"),num(grant,"valid_from"),grant.get("valid_until")==null?null:num(grant,"valid_until"));
    }
    public Operador operador(String id){
        var r=row(id);if(!enabled(r))throw new FalloTecnico(401);
        var accesses=accesos(id);Rol primary=accesses.stream().filter(Acceso::jefe).findFirst().map(x->Rol.JEFATURA).orElseGet(()->accesses.stream().anyMatch(x->x.rol().equals("DISTRIBUCION"))?Rol.DISTRIBUCION:accesses.stream().anyMatch(x->x.rol().equals("PROFESIONAL"))?Rol.PROFESIONAL:accesses.stream().anyMatch(x->x.rol().equals("TI"))?Rol.TI:Rol.ADMINISTRATIVO);
        return new Operador(id,str(r,"label"),primary,str(r,"unit_code"),str(r,"site_id"),accesses,str(r,"email"),str(r,"cargo"),num(r,"security_epoch"));
    }
    public UserDetails autenticar(String username){
        var rows=db.sql.queryForList("SELECT s.*,c.must_change_password FROM staff_account s LEFT JOIN staff_credential_state c ON c.actor_id=s.actor_id WHERE s.username=?",username);
        if(rows.isEmpty()||!enabled(rows.getFirst()))throw new UsernameNotFoundException("Acceso no habilitado");
        var r=rows.getFirst();return new Principal(str(r,"actor_id"),str(r,"password_hash"),num(r,"security_epoch"),Boolean.TRUE.equals(r.get("must_change_password")));
    }
    public boolean vigente(Principal principal){try{var r=row(principal.getUsername());return enabled(r)&&num(r,"security_epoch")==principal.epoch();}catch(FalloTecnico e){return false;}}
    public List<Operador> operadores(){return db.sql.queryForList("SELECT actor_id FROM staff_account WHERE enabled=TRUE ORDER BY actor_id").stream().map(r->operador(str(r,"actor_id"))).toList();}
    public List<Region> regiones(){return db.sql.queryForList("SELECT * FROM org_region ORDER BY id").stream().map(r->new Region(str(r,"id"),str(r,"label"))).toList();}
    public List<Unidad> unidades(){return db.sql.queryForList("SELECT u.* FROM core_unit u JOIN org_unit_validity v ON v.unit_code=u.code WHERE v.valid_from<=? AND (v.valid_until IS NULL OR v.valid_until>?) ORDER BY u.code",now(),now()).stream().map(r->new Unidad(str(r,"code"),str(r,"label"))).toList();}
    public List<Sede> sedes(){return db.sql.queryForList("SELECT s.* FROM core_site s JOIN org_site_validity v ON v.site_id=s.id WHERE v.valid_from<=? AND (v.valid_until IS NULL OR v.valid_until>?) ORDER BY s.id",now(),now()).stream().map(r->new Sede(str(r,"id"),str(r,"label"))).toList();}
    public List<Presencia> presencias(){return db.sql.queryForList("SELECT * FROM core_unit_site ORDER BY unit_code,site_id").stream().filter(r->unidades().stream().anyMatch(u->u.codigo().equals(r.get("unit_code")))&&sedes().stream().anyMatch(s->s.id().equals(r.get("site_id")))).map(r->new Presencia(str(r,"unit_code"),str(r,"site_id"))).toList();}
    private void ubicacion(String unit,String site){
        if(unit==null&&site==null)return;
        if(unit==null||site==null||presencias().stream().noneMatch(p->p.unidad().equals(unit)&&p.sede().equals(site)))throw new FalloTecnico(400);
    }
    private void perfilValido(String name,String email,String cargo,String unit,String site){text(name,120);text(email,180);text(cargo,120);if(!email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"))throw new FalloTecnico(400);ubicacion(unit,site);}
    private String hash(String password){if(password==null||password.length()<16||password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72||password.chars().anyMatch(Character::isISOControl))throw new FalloTecnico(400);return "{bcrypt}"+encoder.encode(password);}
    public Cuenta crear(String admin,Crear c){ti(admin);var hash=hash(c.password());return db.transaction(()->{guard();ti(admin);return crearInterno(admin,c,hash);});}
    private Cuenta crearInterno(String admin,Crear c,String hash){
        if(c.usuario()==null||!c.usuario().matches("[a-z0-9][a-z0-9._-]{2,63}"))throw new FalloTecnico(400);
        perfilValido(c.nombre(),c.correo(),c.cargo(),c.unidad(),c.sede());String id=UUID.randomUUID().toString();
        db.sql.update("INSERT INTO core_actor(id,label,role_code) VALUES(?,?,'PROFESIONAL')",id,c.nombre());
        db.sql.update("INSERT INTO staff_account(actor_id,username,email,cargo,unit_code,site_id,enabled,security_epoch,password_hash,created_ms,updated_ms,updated_by) VALUES(?,?,?,?,?,?,TRUE,0,?,?,?,?)",id,c.usuario(),c.correo().toLowerCase(Locale.ROOT),c.cargo(),c.unidad(),c.sede(),hash,now(),now(),admin==null?id:admin);
        marcarCambioClave(id,true);
        evento(admin==null?id:admin,id,"CUENTA_CREADA",id);snapshot(admin==null?id:admin,id);return cuenta(id);
    }
    private void snapshot(String author,String id){var r=row(id);db.sql.update("INSERT INTO staff_profile_history(actor_id,label,email,cargo,unit_code,site_id,enabled,security_epoch,changed_by,changed_ms) VALUES(?,?,?,?,?,?,?,?,?,?)",id,r.get("label"),r.get("email"),r.get("cargo"),r.get("unit_code"),r.get("site_id"),r.get("enabled"),r.get("security_epoch"),author,now());}
    private Cuenta cuenta(String id){var r=row(id);return new Cuenta(id,str(r,"username"),str(r,"label"),str(r,"email"),str(r,"cargo"),str(r,"unit_code"),str(r,"site_id"),enabled(r),num(r,"security_epoch"),db.sql.queryForList("SELECT a.id,a.role_code,CASE WHEN ar.area_id IS NULL THEN a.scope_code ELSE 'AREA' END AS scope_code,COALESCE(ca.unit_code,a.unit_code) AS unit_code,COALESCE(ca.site_id,a.site_id) AS site_id,a.valid_from,a.valid_until,a.revoked_ms,a.granted_by,a.revoked_by,g.instruction_id,ar.area_id FROM staff_assignment a LEFT JOIN approved_assignment g ON g.assignment_id=a.id LEFT JOIN staff_assignment_area ar ON ar.assignment_id=a.id LEFT JOIN institution_area ca ON ca.id=ar.area_id WHERE a.actor_id=? ORDER BY a.valid_from,a.id",id).stream().map(AutoridadesInstitucionales::salida).toList(),Boolean.TRUE.equals(r.get("must_change_password")));}
    public List<Cuenta> cuentas(String admin){ti(admin);return db.sql.queryForList("SELECT actor_id FROM staff_account ORDER BY username LIMIT 500").stream().map(r->cuenta(str(r,"actor_id"))).toList();}
    private void ultimaTi(){long count=db.sql.queryForObject("SELECT COUNT(*) FROM staff_account s WHERE enabled=TRUE AND EXISTS(SELECT 1 FROM staff_assignment a WHERE a.actor_id=s.actor_id AND a.role_code='TI' AND a.scope_code='INSTITUCION' AND a.revoked_ms IS NULL AND a.valid_from<=? AND (a.valid_until IS NULL OR a.valid_until>?))",Long.class,now(),now());if(count==0)throw new FalloTecnico(409,"ULTIMO_TI");}
    public Cuenta actualizar(String admin,String id,Perfil p){ti(admin);perfilValido(p.nombre(),p.correo(),p.cargo(),p.unidad(),p.sede());return db.transaction(()->{guard();ti(admin);revision(row(id),p.revision());db.sql.update("UPDATE core_actor SET label=? WHERE id=?",p.nombre(),id);db.sql.update("UPDATE staff_account SET email=?,cargo=?,unit_code=?,site_id=?,enabled=?,security_epoch=security_epoch+1,updated_ms=?,updated_by=? WHERE actor_id=?",p.correo().toLowerCase(Locale.ROOT),p.cargo(),p.unidad(),p.sede(),p.activo(),now(),admin,id);if(p.activo())validarActivacionDistribuidor(id);ultimaTi();evento(admin,id,p.activo()?"PERFIL_HABILITADO":"CUENTA_DESACTIVADA",id);snapshot(admin,id);return cuenta(id);});}
    private void validar(Otorgar o){
        if(o==null||o.rol()==null||!Set.of("TI","DISTRIBUCION","DIRECTOR","COORDINADOR","COORDINADOR_REGIONAL","PROFESIONAL","ADMINISTRATIVO").contains(o.rol())||o.desde()==null||o.hasta()!=null&&!o.hasta().isAfter(o.desde()))throw new FalloTecnico(400);
        if("AREA".equals(o.alcance())){if(o.area()==null||!Set.of("DIRECTOR","COORDINADOR","PROFESIONAL","ADMINISTRATIVO").contains(o.rol()))throw new FalloTecnico(400);var ar=new CatalogoAreas(db,clock).exigir(o.area(),o.unidad(),o.sede());if(o.rol().equals("DIRECTOR")&&!ar.jefatura().equals("DIRECCION"))throw new FalloTecnico(403,"COORDINACION_PROPIA_REQUIERE_MANDATO");if(o.hasta()==null||o.hasta().isAfter(ar.hasta())||o.desde().isBefore(ar.desde()))throw new FalloTecnico(400);return;}
        if(o.area()!=null)throw new FalloTecnico(400);boolean global=Set.of("TI","DISTRIBUCION").contains(o.rol());
        if(global){if(!"INSTITUCION".equals(o.alcance())||o.unidad()!=null||o.sede()!=null)throw new FalloTecnico(400);return;}
        if(o.rol().equals("COORDINADOR_REGIONAL")){if(!"SEDE".equals(o.alcance())||o.unidad()!=null||sedes().stream().noneMatch(s->s.id().equals(o.sede())))throw new FalloTecnico(400);return;}
        if(!"UNIDAD_SEDE".equals(o.alcance()))throw new FalloTecnico(400);ubicacion(o.unidad(),o.sede());if(o.unidad()==null)throw new FalloTecnico(400);
    }
    public void validarConcesion(Otorgar o){validar(o);}
    public void exigirCuentaHabilitada(String id){var r=row(id);if(!enabled(r))throw new FalloTecnico(401);if(Boolean.TRUE.equals(r.get("must_change_password")))throw new FalloTecnico(403,"CAMBIO_CLAVE_REQUERIDO");}
    String otorgarRespaldado(String admin,String id,Otorgar o){
        if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Transacción de instrucción requerida");
        ti(admin);if(Objects.equals(admin,id))throw new FalloTecnico(403,"AUTOASIGNACION_NO_PERMITIDA");return otorgarInterno(admin,id,o);
    }
    private String otorgarInterno(String admin,String id,Otorgar o){validar(o);var target=row(id);if(o.rol().equals("DISTRIBUCION")&&enabled(target))distribucionDisponible("",o.desde().toEpochMilli(),o.hasta()==null?null:o.hasta().toEpochMilli());String grant=UUID.randomUUID().toString();db.sql.update("INSERT INTO staff_assignment(id,actor_id,role_code,scope_code,unit_code,site_id,valid_from,valid_until,granted_by) VALUES(?,?,?,?,?,?,?,?,?)",grant,id,o.rol(),o.area()==null?o.alcance():"UNIDAD_SEDE",o.area()==null?o.unidad():null,o.area()==null?o.sede():null,o.desde().toEpochMilli(),o.hasta()==null?null:o.hasta().toEpochMilli(),admin);db.sql.update("UPDATE staff_account SET security_epoch=security_epoch+1,updated_ms=?,updated_by=? WHERE actor_id=?",now(),admin,id);if(o.area()!=null)db.sql.update("INSERT INTO staff_assignment_area VALUES(?,?)",grant,o.area());evento(admin,id,"ROL_OTORGADO",grant);return grant;}
    public String otorgar(String admin,String id,Otorgar o){
        ti(admin);
        if(Objects.equals(admin,id)){
            // La denegación se conserva fuera de la transacción de concesión; no cambia epoch ni roles.
            db.transaction(()->{guard();ti(admin);evento(admin,id,"AUTOASIGNACION_DENEGADA",id);return null;});
            throw new FalloTecnico(403,"AUTOASIGNACION_NO_PERMITIDA");
        }
        validar(o);throw new FalloTecnico(409,"APROBACION_INSTITUCIONAL_REQUERIDA");
    }
    public void revocar(String admin,String id,String grant){ti(admin);db.transaction(()->{guard();ti(admin);if(!db.sql.queryForList("SELECT instruction_id FROM approved_assignment WHERE assignment_id=?",grant).isEmpty())throw new FalloTecnico(409,"REVOCACION_INSTITUCIONAL_REQUERIDA");revocarRespaldado(admin,id,grant);return null;});}
    void revocarRespaldado(String actor,String id,String grant){
        if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Transacción de revocación requerida");
        if(db.sql.update("UPDATE staff_assignment SET revoked_ms=?,revoked_by=? WHERE id=? AND actor_id=? AND revoked_ms IS NULL",now(),actor,grant,id)!=1)throw new FalloTecnico(404);db.sql.update("UPDATE staff_account SET security_epoch=security_epoch+1,updated_ms=?,updated_by=? WHERE actor_id=?",now(),actor,id);ultimaTi();evento(actor,id,"ROL_REVOCADO",grant);
    }
    private void marcarCambioClave(String id,boolean pendiente){
        if(db.sql.queryForList("SELECT actor_id FROM staff_credential_state WHERE actor_id=?",id).isEmpty())db.sql.update("INSERT INTO staff_credential_state VALUES(?,?)",id,pendiente);
        else db.sql.update("UPDATE staff_credential_state SET must_change_password=? WHERE actor_id=?",pendiente,id);
    }
    public void clave(String admin,String id,Clave c){ti(admin);String hash=hash(c.password());db.transaction(()->{guard();ti(admin);revision(row(id),c.revision());db.sql.update("UPDATE staff_account SET password_hash=?,security_epoch=security_epoch+1,updated_ms=?,updated_by=? WHERE actor_id=?",hash,now(),admin,id);marcarCambioClave(id,true);evento(admin,id,"CLAVE_REEMPLAZADA",id);return null;});}
    /** Cambio de la propia credencial: identidad/epoch del servidor, sin selector de cuenta ni caché de secretos. */
    public void cambiarClavePersonal(Principal principal,CambioClave cambio){
        if(principal==null||!vigente(principal))throw new FalloTecnico(401);
        if(cambio==null||cambio.actual()==null||cambio.actual().isEmpty()||cambio.actual().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new FalloTecnico(400);
        String nuevaHash=hash(cambio.nueva());
        db.transaction(()->{
            guard();if(!vigente(principal))throw new FalloTecnico(401);
            String actor=principal.getUsername(),anterior=str(row(actor),"password_hash");
            if(!anterior.startsWith("{bcrypt}")||!encoder.matches(cambio.actual(),anterior.substring(8)))throw new FalloTecnico(403,"CREDENCIAL_ACTUAL_INVALIDA");
            if(cambio.actual().equals(cambio.nueva()))throw new FalloTecnico(400,"CLAVE_NUEVA_REUTILIZADA");
            db.sql.update("UPDATE staff_account SET password_hash=?,security_epoch=security_epoch+1,updated_ms=?,updated_by=? WHERE actor_id=?",nuevaHash,now(),actor,actor);marcarCambioClave(actor,false);
            evento(actor,actor,"CLAVE_PERSONAL_CAMBIADA",actor);return null;
        });
    }
    public Cuenta bootstrap(Crear c){String hash=hash(c.password());return db.transaction(()->{guard();if(db.sql.queryForObject("SELECT COUNT(*) FROM staff_account",Integer.class)!=0)throw new FalloTecnico(409);var account=crearInterno(null,c,hash);otorgarInterno(account.id(),account.id(),new Otorgar("TI","INSTITUCION",null,null,clock.instant(),null));return cuenta(account.id());});}
}
