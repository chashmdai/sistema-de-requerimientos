package cl.nexbyte.siger.institucional.documental;

import cl.nexbyte.siger.institucional.archivos.ArchivoPrivado;
import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.util.*;
import tools.jackson.databind.JsonNode;import tools.jackson.databind.json.JsonMapper;
import static cl.nexbyte.siger.institucional.documental.ProcesoDocumentoWindows.Operacion.*;

/** Herramientas fijas y privadas; sin sockets públicos, shells o selección de comandos del navegador. */
public final class ServiciosDocumentalesWindows {
    private final AntivirusDocumentoWindows av;
    private final ProcesoDocumentoWindows pdf,qpdf,media,office;
    private static final JsonMapper JSON=JsonMapper.builder(tools.jackson.core.json.JsonFactory.builder().enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION).streamReadConstraints(tools.jackson.core.StreamReadConstraints.builder().maxNestingDepth(100).maxStringLength(1024*1024).maxNumberLength(30).build()).build()).build();
    private final Properties config;
    public ServiciosDocumentalesWindows(Path file)throws IOException{
        config=new Properties();try(var in=Files.newInputStream(ArchivoPrivado.validar(file,65536))){config.load(in);}
        Path jobs=path("trabajos");ArchivoPrivado.validarAcceso(jobs);if(!Files.isDirectory(jobs))throw new IOException("Directorio de trabajos requerido");
        av=new AntivirusDocumentoWindows(path("antivirus"),path("bases"),job(jobs,"av"),manifest(path("antivirus.manifest")),evidence("av"));
        pdf=process(jobs,"pdf","poppler");qpdf=process(jobs,"qpdf","qpdf");media=process(jobs,"media","ffmpeg");office=process(jobs,"office","java");
    }
    private Path path(String key)throws IOException{var value=config.getProperty(key);if(value==null||value.isBlank())throw new IOException("Configuración documental incompleta");return Path.of(value).toAbsolutePath().normalize();}
    private static Path job(Path root,String name)throws IOException{var p=root.resolve(name);if(!Files.exists(p))Files.createDirectory(p);ArchivoPrivado.proteger(p);return p;}
    private Path evidence(String name)throws IOException{if(config.getProperty("evidencia")==null)return null;var base=path("evidencia");ArchivoPrivado.validarAcceso(base);return job(base,name);}
    public static Map<String,String> manifest(Path file)throws IOException{var node=JSON.readTree(Files.readAllBytes(ArchivoPrivado.validar(file,512*1024)));var result=new HashMap<String,String>();for(var f:node.path("files"))if(result.put(f.path("name").asString(),f.path("sha256").asString())!=null)throw new IOException("Manifiesto duplicado");if(result.isEmpty())throw new IOException("Manifiesto vacío");return result;}
    private ProcesoDocumentoWindows process(Path jobs,String name,String tool)throws IOException{return new ProcesoDocumentoWindows(path(tool),null,job(jobs,name),manifest(path(tool+".manifest")),evidence(name));}
    public ProcesamientoDocumental.Antivirus antivirus(){return av;}
    public ProcesamientoDocumental.Renderizador renderizador(){return new ProcesamientoDocumental.Renderizador(){
        public int paginas(Path file)throws IOException{var run=pdf.procesar(file,PDF_INFO,"");ok(run);var matcher=java.util.regex.Pattern.compile("(?m)^Pages:\\s+([0-9]+)\\s*$").matcher(text(run));if(!matcher.find())throw new IOException("PDF sin páginas legibles");int n=Integer.parseInt(matcher.group(1));if(n<1||n>100)throw new IllegalArgumentException("PDF fuera de perfil");return n;}
        public byte[] pagina(Path file,int number)throws IOException{if(number<1||number>100)throw new IllegalArgumentException("Página inválida");var run=pdf.procesar(file,PDF_PAGINA,Integer.toString(number));ok(run);return png(run.archivo(),6*1024*1024);}
    };}
    private static String text(ProcesoDocumentoWindows.Ejecucion r){return new String(r.stdout(),StandardCharsets.UTF_8);}
    private static void ok(ProcesoDocumentoWindows.Ejecucion r)throws IOException{int code=r.resultado().scannerExit();if(code!=0){if(code>0&&code<64)throw new IllegalArgumentException("Contenido no admitido");throw new IOException("Procesamiento documental no disponible");}}
    private static byte[] png(byte[] bytes,int max)throws IOException{if(bytes==null||bytes.length<33||bytes.length>max||!Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))throw new IOException("PNG derivado no disponible");return bytes;}
    private static void reject(boolean condition){if(!condition)throw new IllegalArgumentException("Contenido no admitido");}
    public ProcesamientoDocumental.Inspector inspector(){return this::examinar;}
    private ProcesamientoDocumental.Examen examinar(Path file,String ext)throws IOException{
        reject(FormatosDocumento.TIPOS.containsKey(ext));String type=FormatosDocumento.TIPOS.get(ext);String view="DESCARGA";byte[] preview=null;
        if(Set.of("txt","docx","xlsx","pptx","odt").contains(ext)){
            var r=office.procesar(file,OFICINA,ext);int code=r.resultado().scannerExit();if(code==5)throw new IllegalArgumentException("Contenido no admitido");if(code!=0||!text(r).strip().equals("VALIDO"))throw new IOException("Inspector de oficina no disponible");
        }else if(ext.equals("pdf")){
            ok(qpdf.procesar(file,PDF_CHECK,""));int encrypted=qpdf.procesar(file,PDF_ENCRYPTED,"").resultado().scannerExit();reject(encrypted==2);
            var r=qpdf.procesar(file,PDF_JSON,"");ok(r);var tree=JSON.readTree(r.stdout());var stack=new ArrayDeque<JsonNode>();stack.add(tree);int count=0;
            var forbidden=Set.of("/JavaScript","/JS","/Launch","/EmbeddedFiles","/RichMedia","/XFA","/AA","/OpenAction","/SubmitForm","/ImportData","/GoToR","/Sound","/Movie");
            while(!stack.isEmpty()){reject(++count<=1000000);var node=stack.removeLast();if(node.isObject())for(var entry:node.properties()){reject(!forbidden.contains(entry.getKey()));stack.add(entry.getValue());}else if(node.isArray())for(var child:node)stack.add(child);else if(node.isString())reject(!forbidden.contains(node.asString()));}
            try{preview=renderizador().pagina(file,1);if(preview.length<=2*1024*1024)view="IMAGEN_DERIVADA";else preview=null;}catch(IOException unavailable){preview=null;}
        }else{
            var r=media.procesar(file,MEDIA_INFO,"");ok(r);var data=JSON.readTree(r.stdout());var streams=data.path("streams");reject(streams.isArray()&&streams.size()>0&&streams.size()<=3);
            var containers=new HashSet<>(Arrays.asList(data.path("format").path("format_name").asString().split(",")));
            if(Set.of("png","jpg","jpeg","webp").contains(ext)){
                var expected=Map.of("png","png","jpg","mjpeg","jpeg","mjpeg","webp","webp").get(ext);var s=streams.get(0);reject(streams.size()==1&&s.path("codec_name").asString().equals(expected));long pixels=(long)s.path("width").asInt()*s.path("height").asInt();reject(pixels>0&&pixels<=24000000);
                var image=media.procesar(file,IMAGEN,"");ok(image);preview=png(image.archivo(),6*1024*1024);if(preview.length<=2*1024*1024)view="IMAGEN_DERIVADA";else preview=null;
            }else{
                var allowed=Map.of("mp3",Set.of("mp3"),"wav",Set.of("wav"),"ogg",Set.of("ogg"),"m4a",Set.of("mov","mp4","m4a"),"mp4",Set.of("mov","mp4"),"mov",Set.of("mov","mp4"),"webm",Set.of("matroska","webm")).get(ext);reject(allowed!=null&&!Collections.disjoint(allowed,containers));
                boolean video=Set.of("mp4","mov","webm").contains(ext);double duration;try{duration=Double.parseDouble(data.path("format").path("duration").asString());}catch(Exception e){throw new IllegalArgumentException("Duración inválida");}reject(Double.isFinite(duration)&&duration>0&&duration<=(video?180:1800));boolean hasVideo=false;
                for(var stream:streams){String kind=stream.path("codec_type").asString(),codec=stream.path("codec_name").asString();if(kind.equals("video")){long px=(long)stream.path("width").asInt()*stream.path("height").asInt();reject(video&&Set.of("h264","vp8","vp9").contains(codec)&&px>0&&px<=2073600);hasVideo=true;}else if(kind.equals("audio")){int channels=stream.path("channels").asInt();long rate;try{rate=Long.parseLong(stream.path("sample_rate").asString());}catch(Exception e){throw new IllegalArgumentException("Audio inválido");}reject(Set.of("mp3","aac","opus","vorbis","pcm_s16le","pcm_s24le","pcm_u8").contains(codec)&&channels>0&&channels<=2&&rate>0&&rate<=96000);}else reject(false);}
                reject(!video||hasVideo);ok(media.procesar(file,MEDIA_DECODIFICAR,""));view=video?"VIDEO":"AUDIO";
            }
        }
        return new ProcesamientoDocumental.Examen(type,view,preview);
    }
}
