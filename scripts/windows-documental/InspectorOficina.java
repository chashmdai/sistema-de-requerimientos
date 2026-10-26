package cl.nexbyte.siger.institucional.documental.worker;

import java.io.*;import java.nio.charset.*;import java.nio.file.*;import java.net.*;import java.util.*;import java.util.zip.*;
import javax.xml.XMLConstants;import javax.xml.parsers.*;import org.w3c.dom.*;

/** Parser descartable. Solo se ejecuta como hijo AppContainer, nunca dentro del backend. */
public final class InspectorOficina {
    static void require(boolean value)throws IOException{if(!value)throw new IOException("RECHAZADO");}
    static int u16(byte[] b,int p){return (b[p]&255)|((b[p+1]&255)<<8);}
    static long u32(byte[] b,int p){return Integer.toUnsignedLong(u16(b,p)|(u16(b,p+2)<<16));}
    // ZipFile no expone el modo Unix. Revisar el directorio central sin extraer nada.
    static void central(Path file)throws IOException{
        try(var in=new RandomAccessFile(file.toFile(),"r")){
            long size=in.length();int tail=(int)Math.min(size,65557);byte[] b=new byte[tail];in.seek(size-tail);in.readFully(b);int end=-1;
            for(int i=tail-22;i>=0;i--)if(u32(b,i)==0x06054b50L&&i+22+u16(b,i+20)==tail){end=i;break;}
            require(end>=0&&u16(b,end+4)==0&&u16(b,end+6)==0);int count=u16(b,end+10);require(count>0&&count<=2000&&u16(b,end+8)==count);
            long length=u32(b,end+12),offset=u32(b,end+16);require(offset+length==size-tail+end);in.seek(offset);
            for(int i=0;i<count;i++){byte[] h=new byte[46];in.readFully(h);require(u32(h,0)==0x02014b50L&&(u16(h,8)&1)==0&&Set.of(0,8).contains(u16(h,10))&&u16(h,34)==0);int mode=(int)(u32(h,38)>>>16)&0xf000;require(mode==0||mode==0x8000||mode==0x4000);require(u32(h,42)<offset);long next=in.getFilePointer()+u16(h,28)+u16(h,30)+u16(h,32);require(next<=offset+length);in.seek(next);}
            require(in.getFilePointer()==offset+length);
        }
    }
    static byte[] read(ZipFile zip,ZipEntry e,long max)throws IOException{
        require(e!=null&&e.getSize()>=0&&e.getSize()<=max);var crc=new CRC32();var out=new ByteArrayOutputStream();long count=0;
        try(var in=zip.getInputStream(e)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){count+=n;require(count<=max&&count<=e.getSize());crc.update(b,0,n);out.write(b,0,n);}}
        require(count==e.getSize()&&crc.getValue()==e.getCrc());return out.toByteArray();
    }
    static Document xml(byte[] bytes)throws Exception{
        var f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);f.setXIncludeAware(false);f.setExpandEntityReferences(false);
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        f.setFeature("http://xml.org/sax/features/external-general-entities",false);f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
        var builder=f.newDocumentBuilder();builder.setEntityResolver((p,s)->{throw new org.xml.sax.SAXException("EXTERNO");});return builder.parse(new ByteArrayInputStream(bytes));
    }
    static String local(Node n){return n.getLocalName()==null?n.getNodeName():n.getLocalName();}
    static String decode(String s){return URLDecoder.decode(s.replace("+","%2B"),StandardCharsets.UTF_8);}
    static String resolve(String base,String value)throws Exception{
        String v=decode(value);require(!v.isEmpty()&&!v.contains("\\")&&v.chars().noneMatch(c->c<32));var u=new URI(v);
        require(u.getScheme()==null&&u.getRawAuthority()==null&&u.getQuery()==null);String p=u.getPath();require(p!=null);
        String combined=p.startsWith("/")?p.substring(1):(base.isEmpty()?p:base+"/"+p);var parts=new ArrayDeque<String>();
        for(var part:combined.split("/")){if(part.isEmpty()||part.equals("."))continue;if(part.equals("..")){require(!parts.isEmpty());parts.removeLast();}else parts.add(part);}return String.join("/",parts);
    }
    static void content(ZipFile zip,ZipEntry entry,String ext,Set<String> names)throws Exception{
        String name=entry.getName().toLowerCase(Locale.ROOT);
        if(name.endsWith(".xml")||name.endsWith(".rels")){
            byte[] bytes=read(zip,entry,8L*1024*1024);var doc=xml(bytes);String low=new String(bytes,StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
            for(var bad:List.of("macroenabled","macrosheet","oleobject","javascript","vbaproject","ddeauto","includetext","webservice(","dde(","script:"))require(!low.contains(bad));
            var stack=new ArrayDeque<Node>();stack.add(doc.getDocumentElement());int count=0;
            while(!stack.isEmpty()){
                var node=stack.removeLast();require(++count<=1000000);if(node instanceof Element el){String tag=local(el);String lower=tag.toLowerCase(Locale.ROOT);
                    require(!Set.of("object","object-ole","oleobj","binary-data","control","customui","externaldata","script","event-listener").contains(lower));
                    require(!(ext.equals("xlsx")&&tag.equals("f")));require(!(tag.equals("scripts")&&(el.getChildNodes().getLength()>0||!el.getTextContent().isBlank())));
                    var attrs=el.getAttributes();StringBuilder joined=new StringBuilder(el.getTextContent());
                    for(int i=0;i<attrs.getLength();i++){var a=attrs.item(i);String key=local(a);String value=a.getNodeValue();joined.append(' ').append(value);require(!(key.endsWith("TargetMode")&&value.equalsIgnoreCase("external")));
                        if(Set.of("href","src").contains(key.toLowerCase(Locale.ROOT))){var u=new URI(decode(value.strip()));require(u.getScheme()==null&&u.getRawAuthority()==null&&!u.getPath().startsWith("/")&&!u.getPath().startsWith("\\")&&!Arrays.asList(u.getPath().split("/")).contains(".."));}}
                    String normal=joined.toString().toLowerCase(Locale.ROOT).replaceAll("\\s+","");if(Set.of("f","instrText","fldSimple").contains(tag))for(var bad:List.of("dde","webservice","includetext","includepicture","database","link","http:","https:","file:","|"))require(!normal.contains(bad));
                }
                var kids=node.getChildNodes();for(int i=0;i<kids.getLength();i++)if(kids.item(i) instanceof Element)stack.add(kids.item(i));
            }
            if(name.endsWith(".rels")){require(doc.getDocumentElement().getTagName().endsWith("Relationships")&&"http://schemas.openxmlformats.org/package/2006/relationships".equals(doc.getDocumentElement().getNamespaceURI()));
                String original=entry.getName();int index=original.lastIndexOf("/_rels/");String base=index<0?"":original.substring(0,index);
                var children=doc.getDocumentElement().getChildNodes();for(int i=0;i<children.getLength();i++)if(children.item(i) instanceof Element rel)require(names.contains(resolve(base,rel.getAttribute("Target"))));}
        }else{
            long count=0;var crc=new CRC32();try(var in=zip.getInputStream(entry)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){count+=n;require(count<=entry.getSize()&&count<=50L*1024*1024);crc.update(b,0,n);}}require(count==entry.getSize()&&crc.getValue()==entry.getCrc());
        }
    }
    static void inspect(Path input,String ext)throws Exception{
        require(Files.size(input)>0&&Files.size(input)<=150L*1024*1024);
        if(ext.equals("txt")){try(var reader=new InputStreamReader(Files.newInputStream(input),StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT))){char[] b=new char[8192];int n;while((n=reader.read(b))!=-1)for(int i=0;i<n;i++)require(!(b[i]<32&&b[i]!='\t'&&b[i]!='\r'&&b[i]!='\n'||b[i]==127));}return;}
        var required=Map.of("docx","word/document.xml","xlsx","xl/workbook.xml","pptx","ppt/presentation.xml","odt","content.xml");require(required.containsKey(ext));
        try(var header=Files.newInputStream(input)){require(Arrays.equals(header.readNBytes(4),new byte[]{80,75,3,4}));}
        central(input);
        try(var zip=new ZipFile(input.toFile())){
            var entries=Collections.list(zip.entries());require(entries.size()<=2000);var names=new HashSet<String>();long total=0;
            for(var e:entries){String n=e.getName().toLowerCase(Locale.ROOT);require(names.add(e.getName())&&!n.startsWith("/")&&!n.contains("..")&&!n.contains("\\")&&!n.contains(":"));
                for(var bad:List.of("vbaproject","activex/","embeddings/","scripts/","externallinks/","connections.xml","macrosheets/"))require(!n.contains(bad));
                require(!n.matches(".*\\.(exe|dll|bin|js|vbs|html|htm|svg)$"));require(e.getSize()>=0&&e.getSize()<=50L*1024*1024&&e.getCompressedSize()>=0&&e.getCompressedSize()<=Files.size(input));require(e.getSize()<=Math.max(1,e.getCompressedSize())*200);total+=e.getSize();require(total<=100L*1024*1024);}
            var main=xml(read(zip,zip.getEntry(required.get(ext)),8L*1024*1024)).getDocumentElement();
            String root=Map.of("docx","document","xlsx","workbook","pptx","presentation","odt","document-content").get(ext);
            String ns=Map.of("docx","http://schemas.openxmlformats.org/wordprocessingml/2006/main","xlsx","http://schemas.openxmlformats.org/spreadsheetml/2006/main","pptx","http://schemas.openxmlformats.org/presentationml/2006/main","odt","urn:oasis:names:tc:opendocument:xmlns:office:1.0").get(ext);require(root.equals(local(main))&&ns.equals(main.getNamespaceURI()));
            if(ext.equals("odt")){require(!entries.isEmpty()&&entries.getFirst().getName().equals("mimetype")&&entries.getFirst().getMethod()==ZipEntry.STORED);require(new String(read(zip,zip.getEntry("mimetype"),100),StandardCharsets.UTF_8).equals("application/vnd.oasis.opendocument.text"));xml(read(zip,zip.getEntry("META-INF/manifest.xml"),8L*1024*1024));}
            else{
                var types=xml(read(zip,zip.getEntry("[Content_Types].xml"),8L*1024*1024)).getDocumentElement();require(local(types).equals("Types")&&"http://schemas.openxmlformats.org/package/2006/content-types".equals(types.getNamespaceURI()));
                String expected=Map.of("docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml","xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml","pptx","application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml").get(ext);boolean found=false;var kids=types.getChildNodes();for(int i=0;i<kids.getLength();i++)if(kids.item(i) instanceof Element e&&e.getAttribute("PartName").equals("/"+required.get(ext))&&e.getAttribute("ContentType").equals(expected))found=true;require(found);
                var rels=xml(read(zip,zip.getEntry("_rels/.rels"),8L*1024*1024)).getDocumentElement();found=false;kids=rels.getChildNodes();for(int i=0;i<kids.getLength();i++)if(kids.item(i) instanceof Element e&&e.getAttribute("Type").endsWith("/officeDocument")&&e.getAttribute("Target").replaceFirst("^/","").equals(required.get(ext)))found=true;require(found);
            }
            for(var e:entries)content(zip,e,ext,names);
        }
    }
    public static void main(String[] args){try{if(args.length!=2)throw new IOException();inspect(Path.of(args[0]),args[1]);System.out.println("VALIDO");}catch(Throwable failure){System.exit(5);}}
}
