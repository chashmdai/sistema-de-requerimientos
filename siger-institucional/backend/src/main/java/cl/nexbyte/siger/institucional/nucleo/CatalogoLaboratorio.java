package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.robustez.*;
import java.util.*;

/** Matriz mínima de laboratorio, no organigrama ni matriz de permisos DDN aprobados. */
public final class CatalogoLaboratorio {
    public enum Rol { DISTRIBUCION, JEFATURA, PROFESIONAL }
    public record Operador(String id,String nombre,Rol rol,String unidad,String sede) {
        public boolean distribuidor(){return rol==Rol.DISTRIBUCION;}
        public boolean jefatura(){return rol==Rol.JEFATURA;}
        public boolean alcance(String u,String s){return Objects.equals(unidad,u)&&Objects.equals(sede,s);}
    }
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
    public Operador operador(String id){return OPERADORES.stream().filter(u->u.id().equals(id)).findFirst().orElseThrow(()->new FalloTecnico(403));}
    public List<Operador> responsables(String unidad,String sede){return OPERADORES.stream().filter(u->u.alcance(unidad,sede)).toList();}
    public void instalar(BaseJdbc db){db.transaction(()->{
        for(var u:UNIDADES)if(db.sql.queryForList("SELECT code FROM core_unit WHERE code=?",u.codigo()).isEmpty())db.sql.update("INSERT INTO core_unit VALUES(?,?)",u.codigo(),u.nombre());
        for(var s:SEDES)if(db.sql.queryForList("SELECT id FROM core_site WHERE id=?",s.id()).isEmpty())db.sql.update("INSERT INTO core_site VALUES(?,?)",s.id(),s.nombre());
        for(var p:PRESENCIAS)if(db.sql.queryForList("SELECT unit_code FROM core_unit_site WHERE unit_code=? AND site_id=?",p.unidad(),p.sede()).isEmpty())db.sql.update("INSERT INTO core_unit_site VALUES(?,?)",p.unidad(),p.sede());
        for(var a:OPERADORES)if(db.sql.queryForList("SELECT id FROM core_actor WHERE id=?",a.id()).isEmpty())db.sql.update("INSERT INTO core_actor VALUES(?,?,?,?,?)",a.id(),a.nombre(),a.rol().name(),a.unidad(),a.sede());return null;
    });}
}
