package cl.nexbyte.siger.institucional.documental;
import java.io.IOException;import java.nio.file.Path;
public final class ProcesamientoDocumental {
    private ProcesamientoDocumental(){}
    public interface Antivirus { boolean limpio(Path archivo)throws IOException; }
    public interface Inspector { Examen examinar(Path archivo,String extension)throws IOException; }
    public interface Renderizador { int paginas(Path original)throws IOException; byte[] pagina(Path original,int pagina)throws IOException; }
    public record Examen(String tipo,String vista,byte[] png){
        public Examen {png=png==null?null:png.clone();}
        @Override public byte[] png(){return png==null?null:png.clone();}
        @Override public String toString(){return "Examen[protegido]";}
    }
}
