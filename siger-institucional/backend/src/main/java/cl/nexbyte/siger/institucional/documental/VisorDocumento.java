package cl.nexbyte.siger.institucional.documental;

import cl.nexbyte.siger.institucional.formulario.*;
import cl.nexbyte.siger.institucional.robustez.*;
import java.io.*;import java.nio.*;import java.nio.file.*;import java.security.*;import java.time.*;import java.util.*;import java.util.concurrent.Semaphore;

/** Derivadas independientes del original; un trabajo acotado y páginas generadas bajo demanda. */
public final class VisorDocumento {
    public static final String PERFIL="PNG1600_V1";
    public record Vista(String representacion,int paginas,String perfil,boolean originalInline,boolean descargable){}
    public record Pagina(Path path,long tamanio,String sha256) { @Override public String toString(){return "Pagina[privada]";} }
    private final BaseJdbc db;private final ArchivoInstitucional files;private final ProcesamientoDocumental.Antivirus antivirus;private final ProcesamientoDocumental.Inspector inspector;private final ProcesamientoDocumental.Renderizador renderer;private final Clock clock;
    private final Semaphore trabajo=new Semaphore(1);
    public VisorDocumento(BaseJdbc db,ArchivoInstitucional files,ProcesamientoDocumental.Antivirus av,ProcesamientoDocumental.Inspector inspector,ProcesamientoDocumental.Renderizador renderer,Clock clock){this.db=db;this.files=files;this.antivirus=av;this.inspector=inspector;this.renderer=renderer;this.clock=clock;}
    private Path original(DocumentoPrivado d){try{return files.verificar(d.clave(),d.original());}catch(IOException corrupt){throw new FalloTecnico(409,"INTEGRIDAD_DOCUMENTAL_INVALIDA");}}
    private Map<String,Object> metadata(DocumentoPrivado d){var rows=db.sql.queryForList("SELECT * FROM document_representation WHERE document_id=? AND profile=?",d.id().toString(),PERFIL);if(rows.isEmpty())return null;var row=rows.getFirst();if(!d.original().sha256().equals(row.get("source_sha256")))throw new FalloTecnico(409);return row;}
    public Vista vista(DocumentoPrivado d){
        if(!FormatosDocumento.paginable(d.original().tipo()))return new Vista("DESCARGA",0,PERFIL,false,true);
        var source=original(d);var existing=metadata(d);if(existing!=null)return new Vista("PAGINAS",((Number)existing.get("pages")).intValue(),PERFIL,false,true);
        if(!trabajo.tryAcquire())throw new FalloTecnico(429);
        try{
            existing=metadata(d);if(existing!=null)return new Vista("PAGINAS",((Number)existing.get("pages")).intValue(),PERFIL,false,true);
            if(!antivirus.limpio(source))throw new FalloTecnico(422,"DOCUMENTO_RECHAZADO");
            var examined=inspector.examinar(source,FormatosDocumento.extensionVista(d.original().tipo()));if(!examined.tipo().equals(d.original().tipo()))throw new IllegalArgumentException();
            boolean pdf=d.original().tipo().equals("application/pdf");int pages=pdf?renderer.paginas(source):1;
            if(pages<1||pages>100)throw new IllegalArgumentException();byte[] png=pdf?renderer.pagina(source,1):examined.png();
            var ready=materializar(d,1,png);
            db.transaction(()->{db.sql.update("INSERT INTO document_representation VALUES(?,?,?,?,?)",d.id().toString(),PERFIL,d.original().sha256(),pages,clock.millis());registrarPagina(d,1,ready);return null;});
            return new Vista("PAGINAS",pages,PERFIL,false,true);
        }catch(IllegalArgumentException bad){throw new FalloTecnico(422,"VISTA_NO_DISPONIBLE");}catch(IOException unavailable){throw new FalloTecnico(503,"VISTA_NO_DISPONIBLE");}finally{trabajo.release();}
    }
    public Pagina pagina(DocumentoPrivado d,int number){
        var view=vista(d);if(!view.representacion().equals("PAGINAS")||number<1||number>view.paginas())throw new FalloTecnico(404);
        var found=buscar(d,number);if(found!=null)return found;
        if(!trabajo.tryAcquire())throw new FalloTecnico(429);
        try{found=buscar(d,number);if(found!=null)return found;var source=original(d);var ready=materializar(d,number,renderer.pagina(source,number));db.transaction(()->{registrarPagina(d,number,ready);return null;});return buscar(d,number);}
        catch(IOException unavailable){throw new FalloTecnico(503,"VISTA_NO_DISPONIBLE");}catch(IllegalArgumentException invalid){throw new FalloTecnico(422,"VISTA_NO_DISPONIBLE");}finally{trabajo.release();}
    }
    private record Preparada(long size,String sha,String key){}
    private static String key(DocumentoPrivado d,int n){return "vistas/"+d.id()+"/"+PERFIL+"/"+n;}
    private Preparada materializar(DocumentoPrivado d,int n,byte[] png)throws IOException{
        if(png==null||png.length<33||png.length>6*1024*1024||!Arrays.equals(Arrays.copyOf(png,8),new byte[]{(byte)137,80,78,71,13,10,26,10})||!new String(png,12,4,java.nio.charset.StandardCharsets.US_ASCII).equals("IHDR"))throw new IOException("PNG no disponible");
        int width=ByteBuffer.wrap(png,16,4).getInt(),height=ByteBuffer.wrap(png,20,4).getInt();if(width<1||height<1||width>1600||height>1600)throw new IOException("Dimensiones inválidas");
        String sha;try{sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(png));}catch(GeneralSecurityException e){throw new IllegalStateException(e);}
        var a=new EntregaCompleta.Adjunto(d.id(),"pagina.png",png.length,sha,"image/png","VALIDADO");files.guardarClave(key(d,n),a,new ByteArrayInputStream(png));return new Preparada(png.length,sha,key(d,n));
    }
    private void registrarPagina(DocumentoPrivado d,int n,Preparada p){db.sql.update("INSERT INTO document_page VALUES(?,?,?,?,?,?,?)",d.id().toString(),PERFIL,n,p.size(),p.sha(),p.key(),clock.millis());}
    private Pagina buscar(DocumentoPrivado d,int n){
        var rows=db.sql.queryForList("SELECT * FROM document_page WHERE document_id=? AND profile=? AND page_number=?",d.id().toString(),PERFIL,n);if(rows.isEmpty())return null;var r=rows.getFirst();if(!key(d,n).equals(r.get("storage_key")))throw new FalloTecnico(409);
        var a=new EntregaCompleta.Adjunto(d.id(),"pagina.png",((Number)r.get("size_bytes")).longValue(),r.get("sha256").toString(),"image/png","VALIDADO");try{return new Pagina(files.verificar(r.get("storage_key").toString(),a),a.tamanio(),a.sha256());}catch(IOException corrupt){throw new FalloTecnico(409,"INTEGRIDAD_DOCUMENTAL_INVALIDA");}
    }
}