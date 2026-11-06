package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.identidad.AutoridadesInstitucionales;import java.time.*;import java.util.*;

final class AccesoReservadoFixture {
 final RevisionExcepcionalFixture x;final AccesoReservado flow;final DatosNucleoTest d;final Clock clock;String gestion;List<AutoridadesInstitucionales.FacultadReserva> caps;
 AccesoReservadoFixture(DatosNucleoTest d,Clock clock)throws Exception{this.d=d;this.clock=clock;x=new RevisionExcepcionalFixture(d,clock);flow=x.f.core.accesosReservados();gestion=x.f.f.revisor;caps=List.of(cap(gestion,"GESTION_A","ACTIVA"));publicar();}
 AutoridadesInstitucionales.FacultadReserva cap(String actor,String id,String estado){return new AutoridadesInstitucionales.FacultadReserva(id,x.s.persona(actor),"UPR","DEMO_SEDE_A","AREA_A",null,x.f.start,x.f.end,"Respaldo sintético de Gestión para acceso documental","f".repeat(64),estado);}
 void publicar()throws Exception{long rev=d.db.sql.queryForObject("SELECT revision FROM authority_registry WHERE id=1",Long.class)+1;var r=x.common;x.f.importar(new AutoridadesInstitucionales.Registro16("1.6",rev,clock.instant(),x.f.end,r.vinculos(),r.mandatos(),r.areas(),r.ambitos(),r.cambiosArea(),r.materias(),r.sedesRegionales(),r.ambitosMateria(),r.principales(),x.s.funciones,x.caps,caps));}
 UUID proponer(UUID doc,boolean contenido,boolean descarga){return flow.proponer(x.f.a(x.owner),UUID.randomUUID(),x.cas,doc,new AccesoReservado.Proponer(1,x.reviewer,clock.instant(),clock.instant().plusSeconds(120),contenido,descarga,"Jefatura respalda revisión del antecedente reservado")).id();}
 void aprobar(UUID id){flow.decidir(x.f.a(gestion),UUID.randomUUID(),id,new AccesoReservado.Decidir(0,"Gestión respalda de manera independiente el acceso concreto"),"APROBAR");}
}
