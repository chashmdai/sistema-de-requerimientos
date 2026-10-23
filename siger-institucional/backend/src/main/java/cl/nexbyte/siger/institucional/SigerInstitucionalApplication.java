package cl.nexbyte.siger.institucional;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SigerInstitucionalApplication {
    public static void main(String[] args) { if (java.util.Arrays.asList(args).contains("--migrate")) { cl.nexbyte.siger.institucional.robustez.MigrarDatos.ejecutar(); return; }
        SpringApplication.run(SigerInstitucionalApplication.class, args); }
}
