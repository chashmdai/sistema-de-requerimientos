package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.documental.*;import cl.nexbyte.siger.institucional.formulario.*;
import java.io.*;import java.nio.file.*;import java.time.*;import java.util.*;
import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;

final class DocumentosReqFixture implements AutoCloseable {
 final DatosNucleoTest d;final AreasFixture f;final DatosAvisosTest.Tiempo clock=new DatosAvisosTest.Tiempo();
 final ArchivoInstitucional files;final DocumentosRequerimiento docs;final UUID req;
 final ProcesamientoDocumental.Inspector inspector=(p,e)->new ProcesamientoDocumental.Examen(FormatosDocumento.TIPOS.get(e),"DESCARGA",null);
 DocumentosReqFixture(Path p)throws Exception{this(p,null);}
 DocumentosReqFixture(Path p,String target)throws Exception{this(p,target,target==null?"_reqdoc":"_reqdocprev");}
 DocumentosReqFixture(Path p,String target,String suffix)throws Exception{String mysql=System.getProperty("siger.test.autoridad.mysql");d=new DatosNucleoTest(p,mysql==null?null:mysql+suffix+"?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=10000",target);
  if(mysql!=null){d.limpiar();for(var t:List.of("staff_profile_history","identity_event","staff_assignment","staff_account","org_coverage"))d.db.sql.update("DELETE FROM "+t);}
  f=new AreasFixture(d,clock);files=new ArchivoInstitucional(p.resolve("privados"));docs=servicio(path->true,inspector);req=nuevoReq();
 }
 DocumentosRequerimiento servicio(ProcesamientoDocumental.Antivirus av,ProcesamientoDocumental.Inspector i){return new DocumentosRequerimiento(d.db,f.core,files,av,i,clock);}
 UUID key(){return UUID.randomUUID();}
 UUID nuevoReq(){UUID sol=f.core.u2().distribuir(f.a(f.dist),key(),d.nuevaWeb(),new NucleoU2.Encaminar(0,new NuevoDestino("UPR","DEMO_SEDE_A",f.headA,"AREA_A"),"Clasificación sintética",null)).id();return f.core.u2().asumir(f.a(f.headA),key(),sol,new NucleoU2.Asumir(0,f.profA,"Atención profesional sin Caso",null)).id();}
 long revision(){return d.db.sql.queryForObject("SELECT revision FROM core_requirement WHERE id=?",Long.class,req.toString());}
 UUID abrir(){return f.core.u2().abrirCaso(f.a(f.headA),key(),req,new NucleoU2.Apertura(revision(),f.profA,"Objetivo independiente")).id();}
 DocumentosRequerimiento.Documento cargar(){return docs.cargar(f.a(f.profA),key(),req,null,"aporte.txt",bytes());}
 ByteArrayInputStream bytes(){return new ByteArrayInputStream("Original institucional completamente sintético".getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 long gestion(){f.core.bitacora().registrarRequerimiento(f.a(f.profA),key(),req,new BitacoraGestion.Registrar("1.0",revision(),clock.instant().minusSeconds(20),"Gestión efectivamente realizada",null,null));return Long.parseLong(f.core.bitacora().requerimiento(f.a(f.profA),req,0,25).entradas().getFirst().id().substring(4));}
 public void close()throws Exception{files.close();d.close();}
}
