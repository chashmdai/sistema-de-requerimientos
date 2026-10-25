package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.robustez.*;
import java.util.*;

/** Matriz mínima de laboratorio, no organigrama ni matriz de permisos DDN aprobados. */
public final class CatalogoLaboratorio {
    public enum Rol { DISTRIBUCION, JEFATURA, PROFESIONAL, TI, ADMINISTRATIVO, DIRECTOR, COORDINADOR, COORDINADOR_REGIONAL }
    public record Acceso(String rol,String alcance,String unidad,String sede) {
        public boolean jefe(){return Set.of("JEFATURA","DIRECTOR","COORDINADOR","COORDINADOR_REGIONAL").contains(rol);}
        public boolean cubre(String u,String s){return alcance.equals("INSTITUCION") || (Objects.equals(sede,s)&&(unidad==null||Objects.equals(unidad,u)));}
    }
    public record Operador(String id,String nombre,Rol rol,String unidad,String sede,List<Acceso> accesos,String correo,String cargo,long versionAcceso) {
        public Operador(String id,String nombre,Rol rol,String unidad,String sede){this(id,nombre,rol,unidad,sede,List.of(new Acceso(rol.name(),rol==Rol.DISTRIBUCION?"INSTITUCION":"UNIDAD_SEDE",unidad,sede)),null,null,0);}
        public boolean distribuidor(){return accesos.stream().anyMatch(x->x.rol().equals("DISTRIBUCION")&&x.alcance().equals("INSTITUCION"));}
        public boolean ti(){return accesos.stream().anyMatch(x->x.rol().equals("TI")&&x.alcance().equals("INSTITUCION"));}
        public boolean jefatura(){return accesos.stream().anyMatch(Acceso::jefe);}
        public boolean jefeEn(String u,String s){return accesos.stream().anyMatch(x->x.jefe()&&x.cubre(u,s));}
        public boolean profesionalEn(String u,String s){return accesos.stream().anyMatch(x->x.rol().equals("PROFESIONAL")&&x.cubre(u,s));}
        public boolean alcance(String u,String s){return jefeEn(u,s)||profesionalEn(u,s);}
        public boolean distribuyeSede(String s){return distribuidor() || s!=null&&accesos.stream().anyMatch(x->x.rol().equals("COORDINADOR_REGIONAL")&&Objects.equals(s,x.sede()));}
    }
    private final cl.nexbyte.siger.institucional.identidad.IdentidadInstitucional identidad;
    public CatalogoLaboratorio(){this(null);}
    public CatalogoLaboratorio(cl.nexbyte.siger.institucional.identidad.IdentidadInstitucional identidad){this.identidad=identidad;}
    public boolean individual(){return identidad!=null;}
    public List<Unidad> unidades(){return identidad==null?UNIDADES:identidad.unidades();}
    public List<Sede> sedes(){return identidad==null?SEDES:identidad.sedes();}
    public List<Presencia> presencias(){return identidad==null?PRESENCIAS:identidad.presencias();}
    public List<Operador> operadores(){return identidad==null?OPERADORES:identidad.operadores();}
    public record Region(String id,String nombre){}
    public List<Region> regiones(){return identidad==null?List.of():identidad.regiones();}
    public record Unidad(String codigo,String nombre){}
    public record Sede(String id,String nombre){}
    public record Presencia(String unidad,String sede){}
    public static final List<Unidad> UNIDADES=List.of(new Unidad("UPD","Promoción y Difusión de Derechos"),new Unidad("UPR","Protección de Derechos y Representación Judicial"),new Unidad("UEP","Estudios, Publicaciones y Estadísticas"),new Unidad("UGI","Gestión Institucional"));
    public static final List<Sede> SEDES=List.of(new Sede("DEMO_SEDE_A","Sede sintética A · laboratorio"),new Sede("DEMO_SEDE_B","Sede sintética B · laboratorio"));
    public static final List<Presencia> PRESENCIAS=List.of(new Presencia("UPR","DEMO_SEDE_A"),new Presencia("UPD","DEMO_SEDE_A"),new Presencia("UEP","DEMO_SEDE_B"),new Presencia("UGI","DEMO_SEDE_B"));
    public static final List<Operador> OPERADORES;
    static {
        var users=new ArrayList<Operador>();users.add(new Operador("demo.distribucion","Distribución sintética central",Rol.DISTRIBUCION,null,null));
        for(var p:PRESENCIAS){String suffix=p.unidad().toLowerCase(Locale.ROOT)+"."+(p.sede().endsWith("A")?"a":"b");
            users.add(new Operador("demo.jefatura."+suffix,"Jefatura sintética "+p.unidad(),Rol.JEFATURA,p.unidad(),p.sede()));
            users.add(new Operador("demo.profesional."+suffix,"Profesional sintético "+p.unidad(),Rol.PROFESIONAL,p.unidad(),p.sede()));
        }
        users.add(new Operador("demo.profesional2.upr.a","Segundo profesional sintético UPR",Rol.PROFESIONAL,"UPR","DEMO_SEDE_A"));OPERADORES=List.copyOf(users);
    }
    public Operador operador(String id){if(identidad!=null)return identidad.operador(id);return OPERADORES.stream().filter(u->u.id().equals(id)).findFirst().orElseThrow(()->new FalloTecnico(403));}
    public List<Operador> responsables(String unidad,String sede){return operadores().stream().filter(u->u.alcance(unidad,sede)).toList();}
    public void instalar(BaseJdbc db){db.transaction(()->{
        for(var u:UNIDADES)if(db.sql.queryForList("SELECT code FROM core_unit WHERE code=?",u.codigo()).isEmpty())db.sql.update("INSERT INTO core_unit VALUES(?,?)",u.codigo(),u.nombre());
        for(var s:SEDES)if(db.sql.queryForList("SELECT id FROM core_site WHERE id=?",s.id()).isEmpty())db.sql.update("INSERT INTO core_site VALUES(?,?)",s.id(),s.nombre());
        for(var p:PRESENCIAS)if(db.sql.queryForList("SELECT unit_code FROM core_unit_site WHERE unit_code=? AND site_id=?",p.unidad(),p.sede()).isEmpty())db.sql.update("INSERT INTO core_unit_site VALUES(?,?)",p.unidad(),p.sede());
        for(var u:UNIDADES)if(db.sql.queryForList("SELECT unit_code FROM org_unit_validity WHERE unit_code=?",u.codigo()).isEmpty())db.sql.update("INSERT INTO org_unit_validity VALUES(?,0,NULL,TRUE)",u.codigo());
        for(var v:SEDES)if(db.sql.queryForList("SELECT site_id FROM org_site_validity WHERE site_id=?",v.id()).isEmpty())db.sql.update("INSERT INTO org_site_validity VALUES(?,0,NULL,TRUE)",v.id());
        for(var a:OPERADORES)if(db.sql.queryForList("SELECT id FROM core_actor WHERE id=?",a.id()).isEmpty())db.sql.update("INSERT INTO core_actor VALUES(?,?,?,?,?)",a.id(),a.nombre(),a.rol().name(),a.unidad(),a.sede());return null;
    });}
}
