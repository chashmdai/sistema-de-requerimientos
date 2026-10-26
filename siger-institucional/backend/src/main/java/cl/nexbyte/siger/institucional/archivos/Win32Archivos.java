package cl.nexbyte.siger.institucional.archivos;

import com.sun.jna.*;
import com.sun.jna.ptr.*;
import com.sun.jna.win32.*;
import com.sun.jna.platform.win32.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.Set;

/** API Win32, sin shells, privilegios de backup, acceso de volumen ni servicios externos. */
final class Win32Archivos {
    private Win32Archivos(){}
    interface Sddl extends StdCallLibrary {
        Sddl INSTANCE=Native.load("advapi32",Sddl.class,W32APIOptions.UNICODE_OPTIONS);
        boolean ConvertStringSecurityDescriptorToSecurityDescriptorW(WString string,int revision,PointerByReference descriptor,IntByReference size);
    }
    private static IOException error(){return new IOException("Operación de custodia Win32 rechazada ("+Kernel32.INSTANCE.GetLastError()+")");}
    private static String actualSid()throws IOException{
        var token=new WinNT.HANDLEByReference();if(!Advapi32.INSTANCE.OpenProcessToken(Kernel32.INSTANCE.GetCurrentProcess(),WinNT.TOKEN_QUERY,token))throw error();
        try{return Advapi32Util.getTokenAccount(token.getValue()).sidString;}catch(Win32Exception e){throw new IOException("Identidad Windows no disponible ("+e.getErrorCode()+")");}finally{Kernel32.INSTANCE.CloseHandle(token.getValue());}
    }
    static void validarCadena(Path input)throws IOException{
        Path p=input.toAbsolutePath().normalize();
        if(p.getRoot()==null||!p.getRoot().toString().matches("[A-Za-z]:\\\\")||p.toString().substring(2).contains(":"))throw new IOException("Ruta NTFS local requerida");
        for(var part:p){String n=part.toString();if(n.endsWith(".")||n.endsWith(" ")||n.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?"))throw new IOException("Nombre de ruta no permitido");}
        for(var a=p;a!=null;a=a.getParent()){
            int attributes=Kernel32.INSTANCE.GetFileAttributes(a.toString());
            if(attributes==-1){int code=Kernel32.INSTANCE.GetLastError();if(code==2||code==3)continue;throw error();}
            if((attributes&0x400)!=0)throw new IOException("Reparse point no permitido");
        }
    }
    static void proteger(Path path)throws IOException{
        validarCadena(path);boolean directory=Files.isDirectory(path,LinkOption.NOFOLLOW_LINKS);String flags=directory?"OICI":"";
        String acl="D:P(A;"+flags+";FA;;;SY)(A;"+flags+";FA;;;BA)(A;"+flags+";FA;;;"+actualSid()+")";
        var ref=new PointerByReference();if(!Sddl.INSTANCE.ConvertStringSecurityDescriptorToSecurityDescriptorW(new WString(acl),1,ref,null))throw error();
        try{if(!Advapi32.INSTANCE.SetFileSecurity(path.toString(),0x80000004,ref.getValue()))throw error();}finally{Kernel32.INSTANCE.LocalFree(ref.getValue());}
        validarAcl(path);
    }
    static void validarAcl(Path path)throws IOException{
        validarCadena(path);var allowed=Set.of(actualSid(),"S-1-5-18","S-1-5-32-544");
        final WinNT.ACE_HEADER[] entries;try{entries=Advapi32Util.getFileSecurity(path.toString(),false);}catch(Win32Exception e){throw new IOException("DACL no disponible ("+e.getErrorCode()+")");}if(entries==null||entries.length==0)throw new IOException("DACL privada requerida");
        for(var entry:entries){
            if((entry.AceFlags&8)!=0)continue; // INHERIT_ONLY no concede acceso al objeto actual.
            if(entry instanceof WinNT.ACCESS_ALLOWED_ACE ace){if(!allowed.contains(ace.getSidString()))throw new IOException("DACL permite otra identidad");}
            else if(!(entry instanceof WinNT.ACCESS_DENIED_ACE))throw new IOException("DACL no admitida");
        }
    }
    static void sincronizar(Path path)throws IOException{
        validarCadena(path);validarAcl(path);
        // Java no permite abrir el directorio para force. Win32 exige GENERIC_WRITE + BACKUP_SEMANTICS.
        // BACKUP_SEMANTICS permite el handle de directorio; no se habilitan SeBackup/SeRestorePrivilege.
        var h=Kernel32.INSTANCE.CreateFile(path.toString(),WinNT.GENERIC_WRITE,WinNT.FILE_SHARE_READ|WinNT.FILE_SHARE_WRITE|WinNT.FILE_SHARE_DELETE,null,WinNT.OPEN_EXISTING,0x02200000,null);
        if(WinBase.INVALID_HANDLE_VALUE.equals(h))throw error();
        try{if(!Kernel32.INSTANCE.FlushFileBuffers(h))throw error();}finally{Kernel32.INSTANCE.CloseHandle(h);}
    }
    static void publicar(Path source,Path dest)throws IOException{
        validarCadena(source);validarCadena(dest);validarAcl(source);validarAcl(dest.getParent());
        if(Files.exists(dest,LinkOption.NOFOLLOW_LINKS))throw new FileAlreadyExistsException("Original ya existe");
        // Solo rename en el mismo volumen. Sin COPY_ALLOWED ni REPLACE_EXISTING; nunca copia/borrado silencioso.
        if(!Files.getFileStore(source).equals(Files.getFileStore(dest.getParent())))throw new IOException("Publicación requiere el mismo volumen");
        if(!Kernel32.INSTANCE.MoveFileEx(source.toString(),dest.toString(),new WinDef.DWORD(8)))throw error();
    }
}