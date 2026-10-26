package cl.nexbyte.siger.institucional.documental;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import com.sun.jna.Pointer;

/** Adaptador AV concreto; el lanzador compartido solo admite operaciones documentales fijas. */
public final class AntivirusDocumentoWindows implements ProcesamientoDocumental.Antivirus {
    private final ProcesoDocumentoWindows proceso;
    public static final class MemoriaInsuficiente extends IOException {
        private final long disponible,requerida;
        MemoriaInsuficiente(long disponible,long requerida){super("Margen de memoria antivirus insuficiente");this.disponible=disponible;this.requerida=requerida;}
        public long disponible(){return disponible;} public long requerida(){return requerida;}
    }
    public AntivirusDocumentoWindows(Path tools,Path definitions,Path jobs,Map<String,String> hashes)throws IOException{this(tools,definitions,jobs,hashes,null);}
    public AntivirusDocumentoWindows(Path tools,Path definitions,Path jobs,Map<String,String> hashes,Path evidence)throws IOException{
        if(definitions==null)throw new IOException("Bases antivirus requeridas");
        proceso=new ProcesoDocumentoWindows(tools,definitions,jobs,hashes,evidence);
    }
    public ProcesoDocumentoWindows.Resultado ultimoResultado(){return proceso.ultimoResultado();}
    @Override public boolean limpio(Path original)throws IOException{
        try{var result=proceso.procesar(original,ProcesoDocumentoWindows.Operacion.AV,"").resultado();
            if(result.scannerExit()!=0&&result.scannerExit()!=1)throw new IOException("Inspección antivirus incompleta");
            return result.scannerExit()==0;
        }catch(ProcesoDocumentoWindows.MemoriaInsuficiente m){throw new MemoriaInsuficiente(m.disponible(),m.requerida());}
    }
    static void grant(Path p,String user,String sid,String inheritance,String rights)throws IOException{ProcesoDocumentoWindows.grant(p,user,sid,inheritance,rights);}
    static void aplicarDescriptor(Path p,Pointer descriptor,int flags)throws IOException{ProcesoDocumentoWindows.aplicarDescriptor(p,descriptor,flags);}
    public interface Api extends ProcesoDocumentoWindows.Api {}
}
