package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.identidad.*;
import java.time.*;import java.util.*;

/** Configuración nominativa sintética, firmada e independiente de los roles TI. */
final class SubroganciasFixture {
    final AreasFixture f;final AutoridadesInstitucionales.Registro11 common;
    final String aliasSub,aliasAutoridad,aliasTitular;final SubroganciasInstitucionales flow;
    final List<AutoridadesInstitucionales.Vinculo> bindings;
    List<AutoridadesInstitucionales.Funcion> funciones;
    final List<AutoridadesInstitucionales.Mandato> mandatosExtra=new ArrayList<>();
    SubroganciasFixture(DatosNucleoTest d,Clock clock)throws Exception{
        f=new AreasFixture(d,clock);f.otorgar(f.headA,"PROFESIONAL","AREA_A");
        aliasSub=f.user("alias.subrogante");aliasAutoridad=f.user("alias.autoridad");aliasTitular=f.user("alias.titular");
        common=d.mapper.readValue(Base64.getDecoder().decode(d.db.sql.queryForObject("SELECT signed_payload FROM authority_batch ORDER BY revision DESC LIMIT 1",String.class)),AutoridadesInstitucionales.Registro11.class);
        bindings=f.ids.cuentas(f.ti).stream().filter(IdentidadInstitucional.Cuenta::activo).map(c->new AutoridadesInstitucionales.Vinculo(c.id(),persona(c.id()),f.start,f.end,ConcesionFixture.huella(d.db,c.id()))).toList();
        funciones=List.of(funcion("DESIG_A","COORD_A",f.headA,List.of(f.profA),List.of(f.f.revisor),"AREA_A","ACTIVA"));publicar();flow=f.f.autoridades.subrogancias();
    }
    String persona(String actor){return "p-"+(actor.equals(aliasSub)?f.profA:actor.equals(aliasAutoridad)?f.f.revisor:actor.equals(aliasTitular)?f.headA:actor);}
    AutoridadesInstitucionales.Funcion funcion(String id,String codigo,String titular,List<String> subs,List<String> autoridades,String area,String estado){return new AutoridadesInstitucionales.Funcion(id,codigo,"REGIONAL","JEFATURA_SEDES_REGIONALES","DEMO_SEDE_A",persona(titular),subs.stream().map(this::persona).toList(),autoridades.stream().map(this::persona).toList(),List.of(new AutoridadesInstitucionales.AmbitoFuncion("COORDINADOR","AREA","UPR","DEMO_SEDE_A",area)),f.start,f.end,"Designación sintética sin valor nominal real","c".repeat(64),estado);}
    void publicar()throws Exception{long rev=f.d.db.sql.queryForObject("SELECT revision FROM authority_registry WHERE id=1",Long.class)+1;var mandates=new ArrayList<>(common.mandatos());mandates.addAll(mandatosExtra);f.importar(new AutoridadesInstitucionales.Registro14("1.4",rev,f.clock.instant(),f.end,bindings,mandates,common.areas(),common.ambitos(),common.cambiosArea(),List.of(),List.of(new CatalogoMaterias.SedeRegional("DEMO_SEDE_A",f.start,f.end,"ACTIVA")),List.of(),List.of(),funciones));}
    SubroganciasInstitucionales.Solicitud solicitud(Instant start,Instant end,Instant review,UUID previous){return new SubroganciasInstitucionales.Solicitud("DESIG_A",start,end,review,"Ausencia sintética fundada",previous);}
    UUID solicitar(){return flow.solicitar(f.profA,UUID.randomUUID(),solicitud(f.clock.instant(),f.clock.instant().plusSeconds(600),null,null)).id();}
    void aprobar(UUID id){flow.decidir(f.f.revisor,UUID.randomUUID(),id,new SubroganciasInstitucionales.Decision(0,"Autoriza jefatura sintética independiente"),true);}
    void ejecutar(UUID id){flow.ejecutar(f.ti,UUID.randomUUID(),id,1);}
}
