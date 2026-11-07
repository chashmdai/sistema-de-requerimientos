package cl.nexbyte.siger.institucional.documental;

import cl.nexbyte.siger.institucional.formulario.*;
import java.io.*;import java.nio.file.Files;import java.util.UUID;

/** Un único proceso de preparación, antivirus, inspección y custodia para documentos de gestión. */
public final class OriginalesGestion {
 public static final class Rechazo extends RuntimeException {
  public final String codigo;
  private Rechazo(String codigo){super(codigo);this.codigo=codigo;}
 }
 private final ArchivoInstitucional files;private final ProcesamientoDocumental.Antivirus antivirus;private final ProcesamientoDocumental.Inspector inspector;
 public OriginalesGestion(ArchivoInstitucional files,ProcesamientoDocumental.Antivirus antivirus,ProcesamientoDocumental.Inspector inspector){this.files=files;this.antivirus=antivirus;this.inspector=inspector;}
 public Carga preparar(String nombre,InputStream input)throws IOException{var ext=FormatosDocumento.extension(nombre);return new Carga(nombre,ext,files.preparar(input,FormatosDocumento.MAX));}
 public final class Carga implements AutoCloseable {
  private final String nombre,extension;private final ArchivoInstitucional.Temporal temp;private boolean validado;
  private Carga(String nombre,String extension,ArchivoInstitucional.Temporal temp){this.nombre=nombre;this.extension=extension;this.temp=temp;}
  public long tamanio(){return temp.size();}public String sha256(){return temp.sha256();}public String tipo(){return FormatosDocumento.TIPOS.get(extension);}
  public void validar()throws IOException{
   try{if(!antivirus.limpio(temp.path()))throw new Rechazo("ANTIVIRUS_RECHAZADO");if(!tipo().equals(inspector.examinar(temp.path(),extension).tipo()))throw new IllegalArgumentException();}
   catch(IllegalArgumentException rejected){throw new Rechazo("FORMATO_RECHAZADO");}validado=true;
  }
  public String conservar(UUID id)throws IOException{
   if(!validado)throw new IllegalStateException("Validación documental requerida");String storage="gestion/"+id;
   var descriptor=new EntregaCompleta.Adjunto(id,nombre,tamanio(),sha256(),tipo(),"VALIDADO");try(var bytes=Files.newInputStream(temp.path())){files.guardarClave(storage,descriptor,bytes);}return storage;
  }
  @Override public void close()throws IOException{temp.close();}
  @Override public String toString(){return "CargaOriginal[privada]";}
 }
}
