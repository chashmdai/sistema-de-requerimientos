package cl.nexbyte.siger.institucional.documental;
import java.io.*;import java.nio.file.*;import java.nio.channels.FileChannel;import java.util.function.Consumer;
import org.springframework.http.*;import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
public final class TransferenciaDocumento {
    private TransferenciaDocumento(){}
    public static ResponseEntity<StreamingResponseBody> responder(Path path,long size,String sha,String name,String mime,boolean inline,jakarta.servlet.http.HttpServletRequest request,Consumer<String> audit){
        long from=0,to=size-1;boolean partial=false;String etag="\""+sha+"\"",range=request.getHeader("Range");
        var headers=new HttpHeaders();headers.setCacheControl("no-store");headers.set("X-Content-Type-Options","nosniff");headers.set("Content-Security-Policy","default-src 'none'; sandbox");headers.set("Cross-Origin-Resource-Policy","same-origin");headers.set("Referrer-Policy","no-referrer");headers.setETag(etag);headers.set("Accept-Ranges","bytes");headers.setContentType(inline?MediaType.parseMediaType(mime):MediaType.APPLICATION_OCTET_STREAM);headers.setContentDisposition((inline?ContentDisposition.inline():ContentDisposition.attachment()).filename(name,java.nio.charset.StandardCharsets.UTF_8).build());
        if(request.getMethod().equals("GET")&&range!=null&&(request.getHeader("If-Range")==null||request.getHeader("If-Range").equals(etag))){try{
            if(!range.matches("bytes=[0-9]*-[0-9]*")||range.equals("bytes=-"))throw new IllegalArgumentException();var parts=range.substring(6).split("-",-1);
            if(parts[0].isEmpty()){long count=Long.parseLong(parts[1]);if(count<1)throw new IllegalArgumentException();from=Math.max(0,size-count);}else{from=Long.parseLong(parts[0]);if(!parts[1].isEmpty())to=Math.min(to,Long.parseLong(parts[1]));}
            if(from>to||from>=size)throw new IllegalArgumentException();partial=true;
        }catch(IllegalArgumentException invalid){headers.set("Content-Range","bytes */"+size);audit.accept("RANGO_INVALIDO");return new ResponseEntity<>(null,headers,HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);}}
        long start=from,length=to-from+1;if(partial)headers.set("Content-Range","bytes "+from+"-"+to+"/"+size);headers.setContentLength(length);
        audit.accept(inline?"REPRESENTACION":"ORIGINAL");
        StreamingResponseBody body=request.getMethod().equals("HEAD")?null:out->{try(var channel=FileChannel.open(path,StandardOpenOption.READ)){channel.position(start);var input=java.nio.channels.Channels.newInputStream(channel);byte[] buffer=new byte[65536];long left=length;while(left>0){int n=input.read(buffer,0,(int)Math.min(buffer.length,left));if(n<0)throw new EOFException();out.write(buffer,0,n);left-=n;}}};
        return new ResponseEntity<>(body,headers,partial?HttpStatus.PARTIAL_CONTENT:HttpStatus.OK);
    }
}
