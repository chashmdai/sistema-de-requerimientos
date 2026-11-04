package cl.nexbyte.siger.institucional;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SigerInstitucionalApplication {
    public static void main(String[] args) { if(args.length==1&&args[0].equals("--smr")){cl.nexbyte.siger.institucional.smr.ArranqueSmr.main(new String[0]);return;}
        if(args.length==1&&args[0].equals("--smr-probe")){cl.nexbyte.siger.institucional.smr.ArranqueSmr.sonda();return;}
        if(args.length==1&&args[0].equals("--smr-migrate")){cl.nexbyte.siger.institucional.smr.ArranqueSmr.migrar();return;}
        if (java.util.Arrays.asList(args).contains("--migrate")) { cl.nexbyte.siger.institucional.robustez.MigrarDatos.ejecutar(); return; }
        SpringApplication.run(SigerInstitucionalApplication.class, args); }
}
