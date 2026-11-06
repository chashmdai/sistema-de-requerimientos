package cl.nexbyte.siger.institucional.nucleo;

import java.nio.file.Path;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;

class BitacoraMigracionTest {
    @TempDir Path temp;
    @Test void actualizar1515NoInventaFechasNiReescribeActuacionesComandosOFirmas()throws Exception{
        String mysql=System.getProperty("siger.test.autoridad.mysql");
        try(var d=new DatosNucleoTest(temp,mysql==null?null:mysql+"_bitprev?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=10000","15.15")){
            var f=new AreasFixture(d,Clock.systemUTC());UUID caso=f.caso("AREA_A",f.headA,f.profA),key=UUID.randomUUID();var body=new Decision(0,"Actuación anterior a la fecha efectiva explícita");
            var result=f.core.actuar(f.a(f.profA),key,caso,body);var flow=f.core.u2();var sol=flow.distribuir(f.a(f.dist),UUID.randomUUID(),d.nuevaWeb(),new NucleoU2.Encaminar(0,new NuevoDestino("UPR","DEMO_SEDE_A",f.headA,"AREA_A"),"Clasificación",null)).id();
            UUID req=flow.asumir(f.a(f.headA),UUID.randomUUID(),sol,new NucleoU2.Asumir(0,f.profA,"Atención sin Caso","Decisión fundada de territorio sintético")).id(),reqKey=UUID.randomUUID();var reqBody=new Decision(0,"Atención histórica efectivamente registrada");var reqResult=flow.actuar(f.a(f.profA),reqKey,req,reqBody,false);
            var actions=d.db.sql.queryForList("SELECT * FROM core_action ORDER BY id");var history=d.db.sql.queryForList("SELECT * FROM institution_flow_history ORDER BY seq");var commands=d.db.sql.queryForList("SELECT * FROM core_command ORDER BY id");var proofs=d.db.sql.queryForList("SELECT * FROM authority_batch ORDER BY revision");
            d.db.migrate("institucional");assertEquals(actions,d.db.sql.queryForList("SELECT * FROM core_action ORDER BY id"));assertEquals(history,d.db.sql.queryForList("SELECT * FROM institution_flow_history ORDER BY seq"));assertEquals(commands,d.db.sql.queryForList("SELECT * FROM core_command ORDER BY id"));assertEquals(proofs,d.db.sql.queryForList("SELECT * FROM authority_batch ORDER BY revision"));
            assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_case_action",Long.class));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_req_action",Long.class));
            var bit=f.core.bitacora();assertNull(bit.caso(f.a(f.profA),caso,0,25).entradas().getFirst().realizadaEn());assertNull(bit.requerimiento(f.a(f.profA),req,0,25).entradas().getFirst().ejecutor());
            assertEquals(result,f.core.actuar(f.a(f.profA),key,caso,body));assertEquals(reqResult,flow.actuar(f.a(f.profA),reqKey,req,reqBody,false));
            if(mysql!=null){d.limpiar();for(var table:List.of("staff_profile_history","identity_event","staff_assignment","staff_account","org_coverage"))d.db.sql.update("DELETE FROM "+table);}
        }
    }
}
