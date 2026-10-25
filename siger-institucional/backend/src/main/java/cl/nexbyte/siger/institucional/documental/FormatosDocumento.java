package cl.nexbyte.siger.institucional.documental;
import java.util.*;import cl.nexbyte.siger.institucional.robustez.FalloTecnico;
public final class FormatosDocumento {
    public static final long MAX=25L*1024*1024;
    public static final Map<String,String> TIPOS=Map.ofEntries(
        Map.entry("pdf","application/pdf"),Map.entry("png","image/png"),Map.entry("jpg","image/jpeg"),Map.entry("jpeg","image/jpeg"),Map.entry("webp","image/webp"),Map.entry("txt","text/plain"),
        Map.entry("docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document"),Map.entry("xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),Map.entry("pptx","application/vnd.openxmlformats-officedocument.presentationml.presentation"),Map.entry("odt","application/vnd.oasis.opendocument.text"),
        Map.entry("mp3","audio/mpeg"),Map.entry("m4a","audio/mp4"),Map.entry("wav","audio/wav"),Map.entry("ogg","audio/ogg"),Map.entry("mp4","video/mp4"),Map.entry("mov","video/quicktime"),Map.entry("webm","video/webm"));
    private FormatosDocumento(){}
    public static String extension(String nombre){
        if(nombre==null||nombre.isBlank()||nombre.length()>180||nombre.contains("/")||nombre.contains("\\")||nombre.codePoints().anyMatch(Character::isISOControl))throw new FalloTecnico(400);
        var index=nombre.lastIndexOf('.');var ext=index<0?"":nombre.substring(index+1).toLowerCase(Locale.ROOT);if(!TIPOS.containsKey(ext))throw new FalloTecnico(415);return ext;
    }
    public static String extensionVista(String tipo){return switch(tipo){case "application/pdf"->"pdf";case "image/png"->"png";case "image/jpeg"->"jpg";case "image/webp"->"webp";default->throw new IllegalArgumentException("Tipo no representable");};}
    public static boolean paginable(String tipo){return Set.of("application/pdf","image/png","image/jpeg","image/webp").contains(tipo);}
}
