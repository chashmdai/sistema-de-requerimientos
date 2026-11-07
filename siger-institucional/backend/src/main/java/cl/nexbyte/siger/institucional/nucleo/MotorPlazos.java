package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.identidad.CatalogoPlazos;
import cl.nexbyte.siger.institucional.identidad.CatalogoPlazos.*;
import cl.nexbyte.siger.institucional.robustez.FalloTecnico;
import java.time.*;import java.util.*;

/** Cómputo reproducible de registro, nunca plazo de resolución ni pausa por espera/ausencia. */
public final class MotorPlazos {
 private MotorPlazos(){}
 public record Medicion(Instant vencimiento,long transcurridoMs,String resultado){}
 private record Intervalo(Instant inicio,Instant fin){}
 public static Medicion medir(Instant inicio,Instant registro,Politica politica,Calendario calendario){
  if(inicio==null||registro==null||registro.isBefore(inicio))throw new FalloTecnico(409,"FECHAS_NO_COMPARABLES");
  try{
   long limite=Math.multiplyExact(politica.segundos(),1000);if(limite<=0)throw new FalloTecnico(409);
   Instant deadline;long elapsed;
   if(politica.modo().equals("CORRIDAS")){deadline=inicio.plusMillis(limite);elapsed=Duration.between(inicio,registro).toMillis();}
   else if(politica.modo().equals("HABILES")){
    if(calendario==null||!calendario.id().equals(politica.calendario())||!calendario.version().equals(politica.versionCalendario()))throw new FalloTecnico(409,"CALENDARIO_NO_ACREDITADO");
    deadline=vencimiento(inicio,limite,calendario);elapsed=transcurrido(inicio,registro,calendario);
   }else throw new FalloTecnico(409,"COMPUTO_NO_ACREDITADO");
   return new Medicion(deadline,elapsed,elapsed>limite?"FUERA_PLAZO":"DENTRO_PLAZO");
  }catch(DateTimeException|ArithmeticException e){throw new FalloTecnico(409,"FECHAS_FUERA_COMPUTO");}
 }
 private static LocalDate fecha(Instant i,Calendario c){var d=i.atZone(ZoneId.of(c.zona())).toLocalDate();if(d.isBefore(c.desde())||!d.isBefore(c.hasta()))throw new FalloTecnico(409,"CALENDARIO_NO_CUBRE_FECHA");return d;}
 private static List<Intervalo> intervalos(LocalDate d,Calendario c){
  if(c.feriados().contains(d))return List.of();var zone=ZoneId.of(c.zona());var result=new ArrayList<Intervalo>();
  for(var f:c.jornada())if(f.dia()==d.getDayOfWeek().getValue()){
   var a=d.atStartOfDay().plusMinutes(CatalogoPlazos.minuto(f.inicio(),false));var b=d.atStartOfDay().plusMinutes(CatalogoPlazos.minuto(f.fin(),true));
   // Una jornada ambigua o inexistente necesita otro calendario explícito; no se elige offset por suposición.
   var offsetsA=zone.getRules().getValidOffsets(a);var offsetsB=zone.getRules().getValidOffsets(b);if(offsetsA.size()!=1||offsetsB.size()!=1)throw new FalloTecnico(409,"JORNADA_HORARIA_AMBIGUA");
   result.add(new Intervalo(a.toInstant(offsetsA.getFirst()),b.toInstant(offsetsB.getFirst())));
  }
  result.sort(Comparator.comparing(Intervalo::inicio));return result;
 }
 private static Instant vencimiento(Instant inicio,long faltante,Calendario c){
  for(var d=fecha(inicio,c);d.isBefore(c.hasta());d=d.plusDays(1))for(var s:intervalos(d,c)){
   var start=s.inicio().isBefore(inicio)?inicio:s.inicio();if(!s.fin().isAfter(start))continue;long available=Duration.between(start,s.fin()).toMillis();if(faltante<=available)return start.plusMillis(faltante);faltante=Math.subtractExact(faltante,available);
  }
  throw new FalloTecnico(409,"CALENDARIO_NO_CUBRE_VENCIMIENTO");
 }
 private static long transcurrido(Instant inicio,Instant fin,Calendario c){
  LocalDate ultimo=fecha(fin,c);long total=0;for(var d=fecha(inicio,c);!d.isAfter(ultimo);d=d.plusDays(1))for(var s:intervalos(d,c)){
   var a=s.inicio().isBefore(inicio)?inicio:s.inicio();var b=s.fin().isAfter(fin)?fin:s.fin();if(b.isAfter(a))total=Math.addExact(total,Duration.between(a,b).toMillis());
  }return total;
 }
}
