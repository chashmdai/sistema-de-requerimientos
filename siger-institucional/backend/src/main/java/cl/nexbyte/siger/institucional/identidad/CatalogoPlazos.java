package cl.nexbyte.siger.institucional.identidad;

import cl.nexbyte.siger.institucional.robustez.*;
import java.time.*;import java.time.temporal.ChronoUnit;import java.util.*;

/** Parámetros acreditados, sin inventar jornada, feriados o cómputo de las 48 horas. */
public final class CatalogoPlazos {
 private CatalogoPlazos(){}
 public record Franja(int dia,String inicio,String fin){}
 public record Calendario(String id,String version,String zona,LocalDate desde,LocalDate hasta,List<Franja> jornada,List<LocalDate> feriados,String referencia,String sha256){}
 public record Politica(String id,String version,String objetivo,String modo,long segundos,String calendario,String versionCalendario,String alcance,String unidad,String sede,String area,String prioridad,Instant desde,Instant hasta,String estado,String referencia,String sha256){}
 public record Contexto(long autoridad,List<Calendario> calendarios,List<Politica> politicas){}
 public static int minuto(String v,boolean finalDia){
  if(finalDia&&"24:00".equals(v))return 1440;
  if(v==null||!v.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]"))throw new FalloTecnico(400,"JORNADA_INVALIDA");return Integer.parseInt(v.substring(0,2))*60+Integer.parseInt(v.substring(3));
 }
 private static void codigo(String v){AutoridadesInstitucionales.codigo(v);}
 private static void respaldo(String ref,String sha){AutoridadesInstitucionales.texto(ref,180);if(sha==null||!sha.matches("[a-f0-9]{64}"))throw new FalloTecnico(400);}
 public static void validar(List<Calendario> calendars,List<Politica> policies,Instant limite,List<AutoridadesInstitucionales.Area> areas){
  if(calendars==null||policies==null||calendars.size()>32||policies.size()>64)throw new FalloTecnico(400);var seen=new HashSet<String>();
  for(var c:calendars){
   if(c==null)throw new FalloTecnico(400);codigo(c.id());codigo(c.version());respaldo(c.referencia(),c.sha256());
   if(c.version().length()>32||!seen.add(c.id()+":"+c.version())||!"America/Santiago".equals(c.zona())||c.desde()==null||c.hasta()==null||!c.hasta().isAfter(c.desde())||ChronoUnit.DAYS.between(c.desde(),c.hasta())>36600||c.jornada()==null||c.jornada().isEmpty()||c.jornada().size()>64||c.feriados()==null||c.feriados().size()>1000||c.feriados().stream().anyMatch(Objects::isNull)||new HashSet<>(c.feriados()).size()!=c.feriados().size())throw new FalloTecnico(400,"CALENDARIO_INVALIDO");
   for(var d:c.feriados())if(d.isBefore(c.desde())||!d.isBefore(c.hasta()))throw new FalloTecnico(400,"FERIADO_FUERA_CALENDARIO");
   for(var f:c.jornada())if(f==null||f.dia()<1||f.dia()>7||minuto(f.inicio(),false)>=minuto(f.fin(),true))throw new FalloTecnico(400,"JORNADA_INVALIDA");
   for(int i=0;i<c.jornada().size();i++)for(int j=i+1;j<c.jornada().size();j++){var f=c.jornada().get(i);var g=c.jornada().get(j);if(f.dia()==g.dia()&&minuto(f.inicio(),false)<minuto(g.fin(),true)&&minuto(g.inicio(),false)<minuto(f.fin(),true))throw new FalloTecnico(400,"JORNADAS_SOLAPADAS");}
  }
  seen.clear();for(var p:policies){
   if(p==null)throw new FalloTecnico(400);codigo(p.id());codigo(p.version());respaldo(p.referencia(),p.sha256());AutoridadesInstitucionales.intervalo(p.desde(),p.hasta(),limite);
   if(p.version().length()>32||!seen.add(p.id()+":"+p.version())||!Set.of("REGISTRO_ATENCION","REGISTRO_GESTION").contains(p.objetivo())||!Set.of("CORRIDAS","HABILES").contains(p.modo())||p.segundos()!=172800||!Set.of("ACTIVA","SUSPENDIDA","REVOCADA").contains(p.estado())||!Set.of("INSTITUCION","UNIDAD_SEDE","AREA").contains(p.alcance()))throw new FalloTecnico(400,"POLITICA_REGISTRO_INVALIDA");
   if(p.modo().equals("CORRIDAS")){if(p.calendario()!=null||p.versionCalendario()!=null)throw new FalloTecnico(400);}else if(calendars.stream().noneMatch(c->c.id().equals(p.calendario())&&c.version().equals(p.versionCalendario())))throw new FalloTecnico(400,"CALENDARIO_NO_ACREDITADO");
   if(p.alcance().equals("INSTITUCION")){if(p.unidad()!=null||p.sede()!=null||p.area()!=null)throw new FalloTecnico(400);}else{codigo(p.unidad());codigo(p.sede());if(p.alcance().equals("AREA")){codigo(p.area());if(areas.stream().noneMatch(a->a.id().equals(p.area())&&a.unidad().equals(p.unidad())&&a.sede().equals(p.sede())&&!a.desde().isAfter(p.desde())&&!a.hasta().isBefore(p.hasta())))throw new FalloTecnico(400);}else if(p.area()!=null)throw new FalloTecnico(400);}
   if(p.prioridad()!=null)codigo(p.prioridad());
  }
  for(int i=0;i<policies.size();i++)for(int j=i+1;j<policies.size();j++){var a=policies.get(i);var b=policies.get(j);if(a.estado().equals("ACTIVA")&&b.estado().equals("ACTIVA")&&a.objetivo().equals(b.objetivo())&&Objects.equals(a.prioridad(),b.prioridad())&&a.desde().isBefore(b.hasta())&&b.desde().isBefore(a.hasta())&&interseccion(a,b))throw new FalloTecnico(409,"POLITICAS_TEMPORALES_SOLAPADAS");}
 }
 private static boolean interseccion(Politica a,Politica b){return a.alcance().equals("INSTITUCION")||b.alcance().equals("INSTITUCION")||Objects.equals(a.unidad(),b.unidad())&&Objects.equals(a.sede(),b.sede())&&(a.area()==null||b.area()==null||a.area().equals(b.area()));}
 public static String huella(Object v){return AutoridadesInstitucionales.sha(AutoridadesInstitucionales.lectorPrivado().writeValueAsBytes(v));}
 public static String huellaCalendario(Calendario c){return huella(Arrays.asList(c.id(),c.version(),c.zona(),c.desde(),c.hasta(),c.jornada().stream().sorted(Comparator.comparingInt(Franja::dia).thenComparing(Franja::inicio).thenComparing(Franja::fin)).toList(),c.feriados().stream().sorted().toList(),c.referencia(),c.sha256()));}
 public static String huellaPolitica(Politica p){return huella(Arrays.asList(p.id(),p.version(),p.objetivo(),p.modo(),p.segundos(),p.calendario(),p.versionCalendario(),p.alcance(),p.unidad(),p.sede(),p.area(),p.prioridad(),p.desde(),p.hasta(),p.referencia(),p.sha256()));}
 public static void instalar(BaseJdbc db,long revision,List<Calendario> calendars,List<Politica> policies){
  if(calendars.isEmpty()&&policies.isEmpty())return;if(!VersionEsquema.aplicada(db,"15.22"))throw new FalloTecnico(409,"ESQUEMA_TEMPORAL_REQUERIDO");
  for(var c:calendars)guardar(db,"temporal_calendar_version",c.id(),c.version(),huellaCalendario(c),c,revision,null,null);
  for(var p:policies){if(p.unidad()!=null&&db.sql.queryForObject("SELECT COUNT(*) FROM core_unit_site WHERE unit_code=? AND site_id=?",Long.class,p.unidad(),p.sede())!=1)throw new FalloTecnico(400,"AMBITO_TEMPORAL_INVALIDO");guardar(db,"temporal_policy_version",p.id(),p.version(),huellaPolitica(p),p,revision,p.calendario(),p.versionCalendario());}
 }
 private static void guardar(BaseJdbc db,String table,String id,String version,String sha,Object data,long revision,String calendar,String cv){
  var r=db.sql.queryForList("SELECT snapshot_sha256 FROM "+table+" WHERE id=? AND version_code=?",id,version);if(!r.isEmpty()){if(!sha.equals(r.getFirst().get("snapshot_sha256")))throw new FalloTecnico(409,"VERSION_TEMPORAL_INMUTABLE");return;}
  String json=AutoridadesInstitucionales.lectorPrivado().writeValueAsString(data);
  if(table.equals("temporal_calendar_version"))db.sql.update("INSERT INTO temporal_calendar_version VALUES(?,?,?,?,?)",id,version,sha,json,revision);else db.sql.update("INSERT INTO temporal_policy_version VALUES(?,?,?,?,?,?,?)",id,version,sha,json,revision,calendar,cv);
 }
}
