package cl.nexbyte.siger.institucional.documental;
import cl.nexbyte.siger.institucional.formulario.ArchivoInstitucional;import cl.nexbyte.siger.institucional.robustez.BaseJdbc;
import java.nio.file.*;import java.time.Clock;
import org.springframework.context.annotation.*;import org.springframework.core.env.Environment;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.boot.servlet.MultipartConfigFactory;import org.springframework.util.unit.DataSize;
@Configuration @Profile("jdbc-reception") @ConditionalOnProperty(name="institucional.documental.habilitada",havingValue="true")
public class DocumentalConfiguration {
    record Servicios(ProcesamientoDocumental.Antivirus antivirus,ProcesamientoDocumental.Inspector inspector,ProcesamientoDocumental.Renderizador renderizador){}
    @Bean Servicios serviciosDocumentales(Environment e,ArchivoInstitucional files)throws java.io.IOException{
        if(cl.nexbyte.siger.institucional.archivos.PlataformaArchivos.windows()){
            var s=new ServiciosDocumentalesWindows(Path.of(e.getRequiredProperty("institucional.documental.windows-configuracion")));
            return new Servicios(s.antivirus(),s.inspector(),s.renderizador());
        }
        return new Servicios(new AntivirusDocumentoLocal(Path.of(e.getRequiredProperty("institucional.documental.antivirus-socket")),Path.of(e.getRequiredProperty("institucional.documental.antivirus-configuracion")),FormatosDocumento.MAX),new InspectorDocumentoLocal(Path.of(e.getRequiredProperty("institucional.documental.inspector-socket"))),new PdfAislado(Path.of(e.getRequiredProperty("institucional.documental.sandbox")),files.temporales()));
    }
    @Bean ProcesamientoDocumental.Antivirus antivirusDocumental(Servicios s){return s.antivirus();}
    @Bean ProcesamientoDocumental.Inspector inspectorDocumental(Servicios s){return s.inspector();}
    @Bean ProcesamientoDocumental.Renderizador renderizadorDocumental(Servicios s){return s.renderizador();}
    @Bean DocumentosGestion documentosGestion(BaseJdbc db,ArchivoInstitucional files,AutorizacionDocumental policy,ProcesamientoDocumental.Antivirus av,ProcesamientoDocumental.Inspector inspector){return new DocumentosGestion(db,files,policy,av,inspector,Clock.systemUTC());}
    @Bean VisorDocumento visorDocumento(BaseJdbc db,ArchivoInstitucional files,ProcesamientoDocumental.Antivirus av,ProcesamientoDocumental.Inspector inspector,ProcesamientoDocumental.Renderizador render){return new VisorDocumento(db,files,av,inspector,render,Clock.systemUTC());}
    @Bean jakarta.servlet.MultipartConfigElement multipartConfigElement(ArchivoInstitucional files)throws java.io.IOException{
        var directory=files.temporales().resolve("multipart");Files.createDirectories(directory);var factory=new MultipartConfigFactory();factory.setLocation(directory.toString());factory.setFileSizeThreshold(DataSize.ofBytes(0));factory.setMaxFileSize(DataSize.ofBytes(FormatosDocumento.MAX));factory.setMaxRequestSize(DataSize.ofMegabytes(26));return factory.createMultipartConfig();
    }
}
