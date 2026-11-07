package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.documental.*;import cl.nexbyte.siger.institucional.identidad.*;
import java.nio.file.*;import java.util.*;
final class CuarentenaFixture implements AutoCloseable {
 final DocumentosReqFixture x;final CuarentenaDocumental q;final String peer;
 CuarentenaFixture(Path p)throws Exception{x=new DocumentosReqFixture(p,null,"_cuarentena");peer=ti("ti.revisor",null,x.f.ti);q=servicio(file->true,x.inspector);}
 CuarentenaDocumental servicio(ProcesamientoDocumental.Antivirus av,ProcesamientoDocumental.Inspector inspector){return new CuarentenaDocumental(x.d.db,x.f.core,x.f.cat,x.f.f.autoridades,x.files,x.d.recepcion,av,inspector,x.clock);}
 String ti(String nombre,String persona,String ejecutor)throws Exception{
  String target=x.f.user(nombre);var r=x.d.mapper.readValue(Base64.getDecoder().decode(x.d.db.sql.queryForObject("SELECT signed_payload FROM authority_batch ORDER BY revision DESC LIMIT 1",String.class)),AutoridadesInstitucionales.Registro11.class);var bindings=new ArrayList<>(r.vinculos());bindings.add(new AutoridadesInstitucionales.Vinculo(target,persona==null?"p-"+target:persona,x.f.start,x.f.end,ConcesionFixture.huella(x.d.db,target)));var mandates=new ArrayList<>(r.mandatos());
  if(mandates.stream().noneMatch(m->m.id().equals("TI_SEGURIDAD_SINTETICA")))mandates.add(new AutoridadesInstitucionales.Mandato("TI_SEGURIDAD_SINTETICA","p-"+x.f.f.revisor,"AUTORIDAD_SINTETICA","TI","INSTITUCION",null,null,x.f.start,x.f.end,true,true,"EVIDENCIA_TI_SINTETICA","a".repeat(64)));
  x.f.importar(new AutoridadesInstitucionales.Registro11("1.1",r.revision()+1,x.clock.instant(),r.expira(),bindings,mandates,r.areas(),r.ambitos(),r.cambiosArea()));var req=x.f.f.concesiones.solicitar(ejecutor,x.key(),new ConcesionesInstitucionales.Solicitud(target,"TI","INSTITUCION",null,null,x.clock.instant().minusSeconds(1),x.clock.instant().plusSeconds(1800),"Instrucción técnica con aprobación independiente"));x.f.f.concesiones.decidir(x.f.f.revisor,x.key(),req.id(),new ConcesionesInstitucionales.Decision(0,"Autoridad sintética independiente"),true);x.f.f.concesiones.ejecutar(ejecutor,x.key(),req.id(),new ConcesionesInstitucionales.Ejecucion(1));return target;
 }
 CuarentenaDocumental.Aislar solicitud(DocumentosRequerimiento.Documento d){return new CuarentenaDocumental.Aislar(d.id(),d.sha256(),"INCIDENTE-SINTETICO-01","Sospecha técnica sin relato de expediente");}
 public void close()throws Exception{x.close();}
}
