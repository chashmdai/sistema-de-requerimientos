package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.identidad.*;
import java.time.*;import java.util.*;

final class TerritorioFixture {
    final DatosNucleoTest d;final MateriasFixture m;final AreasFixture f;final Clock clock;
    final AutoridadesInstitucionales.Registro12 common;final OrganizacionInstitucional org;
    final UUID propuesta;
    TerritorioFixture(DatosNucleoTest d,Clock clock)throws Exception{
        this.d=d;this.clock=clock;m=new MateriasFixture(d,clock);f=m.f;
        common=d.mapper.readValue(Base64.getDecoder().decode(d.db.sql.queryForObject("SELECT signed_payload FROM authority_batch ORDER BY revision DESC LIMIT 1",String.class)),AutoridadesInstitucionales.Registro12.class);
        org=new OrganizacionInstitucional(d.db,f.ids,clock);
        propuesta=UUID.fromString(org.cubrir(f.ti,new OrganizacionInstitucional.Cobertura("REG_LAB_1","DEMO_SEDE_A",clock.instant().minusSeconds(60),null)));
    }
    CatalogoTerritorial.Acreditacion certificado(String estado,Instant hasta){return new CatalogoTerritorial.Acreditacion(propuesta,"REG_LAB_1","DEMO_SEDE_A",clock.instant().minusSeconds(30),hasta,f.core.territorio().huellaPropuesta(propuesta),estado,"Respaldo territorial sintético independiente","a".repeat(64));}
    void publicar(List<CatalogoTerritorial.Acreditacion> certificados)throws Exception{
        var r=common;long rev=d.db.sql.queryForObject("SELECT revision FROM authority_registry WHERE id=1",Long.class)+1;
        f.importar(new AutoridadesInstitucionales.Registro18("1.8",rev,clock.instant(),f.end,r.vinculos(),r.mandatos(),r.areas(),r.ambitos(),r.cambiosArea(),r.materias(),r.sedesRegionales(),r.ambitosMateria(),List.of(),List.of(),List.of(),List.of(),List.of(),certificados));
    }
    void publicar()throws Exception{publicar(List.of(certificado("ACTIVA",clock.instant().plusSeconds(600))));}
    UUID solicitud(String region){return f.core.u2().distribuir(f.a(f.dist),UUID.randomUUID(),d.nuevaWeb(),new NucleoU2.Encaminar(0,new ContratosNucleo.NuevoDestino("UPR","DEMO_SEDE_A",f.headA,"AREA_A"),"Competencia regional propuesta",region)).id();}
    ContratosNucleo.Resultado asumir(UUID sol){return f.core.u2().asumir(f.a(f.headA),UUID.randomUUID(),sol,new NucleoU2.Asumir(0,f.profA,"Asunción competente",null));}
}
