// Sonda QA de APIs Windows. No es un broker de produccion ni una interfaz del backend.
using System;
using System.ComponentModel;
using System.IO;
using System.Runtime.InteropServices;
using System.Security.AccessControl;
using System.Security.Principal;
using System.Text;
public static class Aislamiento {
    public static int Main(string[] args) {try {Console.WriteLine(Run(args[0],Path.Combine(args[0],"Canario.exe"),args[1],args[2],int.Parse(args[3])));return 0;}catch(Win32Exception e){Console.WriteLine("API_FAILURE;op="+e.Message+";win32="+e.NativeErrorCode);return 1;}catch(Exception e){Console.WriteLine("PROBE_FAILURE;type="+e.GetType().Name);return 2;}}
    [StructLayout(LayoutKind.Sequential)] struct SI {public int cb;public string reserved,desktop,title;public uint x,y,xSize,ySize,xChars,yChars,fill,flags;public short show,reserved2;public IntPtr reservedPtr,input,output,error;}
    [StructLayout(LayoutKind.Sequential)] struct SX {public SI si;public IntPtr attributes;}
    [StructLayout(LayoutKind.Sequential)] struct PI {public IntPtr process,thread;public uint processId,threadId;}
    [StructLayout(LayoutKind.Sequential)] struct Caps {public IntPtr sid,capabilities;public uint count,reserved;}
    [StructLayout(LayoutKind.Sequential)] struct Basic {public long processTime,jobTime;public uint flags;public UIntPtr minWorking,maxWorking;public uint processes;public UIntPtr affinity;public uint priority,scheduling;}
    [StructLayout(LayoutKind.Sequential)] struct Io {public ulong a,b,c,d,e,f;}
    [StructLayout(LayoutKind.Sequential)] struct Extended {public Basic basic;public Io io;public UIntPtr processMemory,jobMemory,peakProcess,peakJob;}
    [StructLayout(LayoutKind.Sequential)] struct SA {public int size;public IntPtr descriptor;public int inherit;}
    [StructLayout(LayoutKind.Sequential)] struct Memory {public uint size,load;public ulong totalPhysical,availablePhysical,totalPage,availablePage,totalVirtual,availableVirtual,extended;}
    [DllImport("kernel32.dll",SetLastError=true)] static extern bool GlobalMemoryStatusEx(ref Memory status);
    [DllImport("kernel32.dll",SetLastError=true)] static extern bool QueryInformationJobObject(IntPtr job,int info,ref Extended limits,uint len,IntPtr returned);
    [DllImport("kernel32.dll",CharSet=CharSet.Unicode,SetLastError=true)] static extern IntPtr CreateFileW(string path,uint access,uint share,ref SA attrs,uint create,uint flags,IntPtr template);
    [DllImport("userenv.dll",CharSet=CharSet.Unicode)] static extern int CreateAppContainerProfile(string n,string d,string description,IntPtr caps,uint count,out IntPtr sid);
    [DllImport("userenv.dll",CharSet=CharSet.Unicode)] static extern int DeleteAppContainerProfile(string n);
    [DllImport("advapi32.dll")] static extern IntPtr FreeSid(IntPtr sid);
    [DllImport("kernel32.dll",SetLastError=true)] static extern bool InitializeProcThreadAttributeList(IntPtr list,int count,int flags,ref IntPtr size);
    [DllImport("kernel32.dll",SetLastError=true)] static extern bool UpdateProcThreadAttribute(IntPtr list,uint flags,IntPtr key,IntPtr value,IntPtr size,IntPtr old,IntPtr returned);
    [DllImport("kernel32.dll")] static extern void DeleteProcThreadAttributeList(IntPtr list);
    [DllImport("kernel32.dll",CharSet=CharSet.Unicode,SetLastError=true)] static extern bool CreateProcessW(string app,StringBuilder args,IntPtr processAttrs,IntPtr threadAttrs,bool inherit,uint flags,IntPtr env,string cwd,ref SX start,out PI process);
    [DllImport("kernel32.dll",CharSet=CharSet.Unicode,SetLastError=true)] static extern IntPtr CreateJobObjectW(IntPtr security,string name);
    [DllImport("kernel32.dll",SetLastError=true)] static extern bool SetInformationJobObject(IntPtr job,int info,ref Extended limits,uint len);
    [DllImport("kernel32.dll",SetLastError=true)] static extern bool AssignProcessToJobObject(IntPtr job,IntPtr process);
    [DllImport("kernel32.dll",SetLastError=true)] static extern uint ResumeThread(IntPtr t);
    [DllImport("kernel32.dll",SetLastError=true)] static extern uint WaitForSingleObject(IntPtr h,uint ms);
    [DllImport("kernel32.dll",SetLastError=true)] static extern bool GetExitCodeProcess(IntPtr p,out uint code);
    [DllImport("advapi32.dll",SetLastError=true)] static extern bool OpenProcessToken(IntPtr p,uint access,out IntPtr token);
    [DllImport("advapi32.dll",SetLastError=true)] static extern bool GetTokenInformation(IntPtr token,int info,IntPtr value,int size,out int returned);
    [DllImport("kernel32.dll",SetLastError=true)] static extern bool TerminateJobObject(IntPtr h,uint code);
    [DllImport("kernel32.dll",SetLastError=true)] static extern bool TerminateProcess(IntPtr h,uint code);
    [DllImport("kernel32.dll")] static extern bool CloseHandle(IntPtr h);
    [DllImport("kernel32.dll")] static extern uint SetErrorMode(uint mode);
    [DllImport("advapi32.dll",CharSet=CharSet.Unicode,SetLastError=true)] static extern bool ConvertStringSecurityDescriptorToSecurityDescriptorW(string s,uint revision,out IntPtr sd,out uint size);
    [DllImport("advapi32.dll",CharSet=CharSet.Unicode,SetLastError=true)] static extern bool SetFileSecurityW(string path,uint info,IntPtr sd);
    [DllImport("kernel32.dll")] static extern IntPtr LocalFree(IntPtr p);
    static void Check(bool ok,string op) {if(!ok)throw new Win32Exception(Marshal.GetLastWin32Error(),op);}
    static void Grant(string path,SecurityIdentifier sid,FileSystemRights rights,bool directory,bool inherit) {
        FileSystemSecurity acl=directory?(FileSystemSecurity)Directory.GetAccessControl(path):File.GetAccessControl(path);
        acl.AddAccessRule(new FileSystemAccessRule(sid,rights,inherit?InheritanceFlags.ContainerInherit|InheritanceFlags.ObjectInherit:InheritanceFlags.None,PropagationFlags.None,AccessControlType.Allow));
        if(directory) Directory.SetAccessControl(path,(DirectorySecurity)acl);else File.SetAccessControl(path,(FileSecurity)acl);
    }
    static void Low(string path) {IntPtr sd;uint size;Check(ConvertStringSecurityDescriptorToSecurityDescriptorW("S:(ML;OICI;NW;;;LW)",1,out sd,out size),"label-create");try{Check(SetFileSecurityW(path,0x10,sd),"label-set");}finally{LocalFree(sd);}}
    // Un descriptor leido no tiene AccessRulesModified. Rehidratarlo marca la DACL para persistirla.
    static void RestoreDirectory(string path,DirectorySecurity saved){var copy=new DirectorySecurity();copy.SetSecurityDescriptorSddlForm(saved.GetSecurityDescriptorSddlForm(AccessControlSections.Access),AccessControlSections.Access);Directory.SetAccessControl(path,copy);}
    static void RestoreFile(string path,FileSecurity saved){var copy=new FileSecurity();copy.SetSecurityDescriptorSddlForm(saved.GetSecurityDescriptorSddlForm(AccessControlSections.Access),AccessControlSections.Access);File.SetAccessControl(path,copy);}
    public static string Run(string root,string exe,string mode,string arguments,int timeout) {
        if(!new System.Collections.Generic.HashSet<string>{"probe","memory","cpu","timeout","crash","noop","av-clean","av-eicar","av-unavailable","av-capacity-clean","av-capacity-eicar","amsi-clean","amsi-control"}.Contains(mode))throw new Exception("Modo QA no admitido");
        root=Path.GetFullPath(root);exe=Path.GetFullPath(exe);bool av=mode.StartsWith("av-",StringComparison.Ordinal);
        bool capacity=mode.StartsWith("av-capacity-",StringComparison.Ordinal);
        ulong availablePhysical=0;
        if(capacity){var memory=new Memory{size=(uint)Marshal.SizeOf(typeof(Memory))};Check(GlobalMemoryStatusEx(ref memory),"memory-query");availablePhysical=memory.availablePhysical;if(memory.availablePhysical<4UL*1024*1024*1024)throw new Exception("Presupuesto de memoria no disponible");}
        if(!root.StartsWith(@"C:\req\testdata\isolation-",StringComparison.OrdinalIgnoreCase)||!System.Text.RegularExpressions.Regex.IsMatch(Path.GetFileName(root),"^isolation-[a-f0-9]{32}$")||!exe.Equals(Path.Combine(root,"Canario.exe"),StringComparison.OrdinalIgnoreCase))throw new Exception("Solo fixture propio");
        for(string path=root;path!=null;path=Path.GetDirectoryName(path))if((File.GetAttributes(path)&FileAttributes.ReparsePoint)!=0)throw new Exception("Enlace no admitido en QA");
        string name="Nexbyte.Siger.U45.Canary."+Guid.NewGuid().ToString("N");IntPtr sid=IntPtr.Zero,list=IntPtr.Zero,capsPtr=IntPtr.Zero,env=IntPtr.Zero,job=IntPtr.Zero,output=IntPtr.Zero,error=IntPtr.Zero,handles=IntPtr.Zero;PI pi=new PI();bool initialized=false,assigned=false;int deleted=0;
        string result=null;bool restored=true;string work=Path.Combine(root,"work-"+mode);Directory.CreateDirectory(work);
        var rootAcl=Directory.GetAccessControl(root);var workAcl=Directory.GetAccessControl(work);var exeAcl=File.GetAccessControl(exe);
        string avTools=@"C:\req\bin\clamav-1.4.6",avDefs=@"C:\req\storage\antivirus-defs";DirectorySecurity toolsAcl=null,defsAcl=null;
        if(av){toolsAcl=Directory.GetAccessControl(avTools);defsAcl=Directory.GetAccessControl(avDefs);}
        int hr=CreateAppContainerProfile(name,"SIGER QA","Canario sintetico efimero sin capacidades",IntPtr.Zero,0,out sid);if(hr!=0)throw new Exception("Profile HRESULT "+hr);
        try {
            SetErrorMode(0x1|0x2|0x8000);
            var identity=new SecurityIdentifier(sid);
            Low(work);Grant(work,identity,FileSystemRights.Modify,true,true);File.WriteAllText(Path.Combine(work,"input.txt"),"SINTETICO");
            // El SID solo atraviesa la raiz privada; el canario externo no se comparte.
            Grant(root,identity,FileSystemRights.Traverse,true,false);
            Grant(exe,identity,FileSystemRights.ReadAndExecute,false,false);
            if(av){Grant(avTools,identity,FileSystemRights.ReadAndExecute,true,true);Grant(avDefs,identity,FileSystemRights.ReadAndExecute,true,true);}
            var caps=new Caps{sid=sid};capsPtr=Marshal.AllocHGlobal(Marshal.SizeOf(typeof(Caps)));Marshal.StructureToPtr(caps,capsPtr,false);
            IntPtr size=IntPtr.Zero;int attributes=av?2:1;InitializeProcThreadAttributeList(IntPtr.Zero,attributes,0,ref size);list=Marshal.AllocHGlobal(size);Check(InitializeProcThreadAttributeList(list,attributes,0,ref size),"attributes-init");initialized=true;
            Check(UpdateProcThreadAttribute(list,0,(IntPtr)0x20009,capsPtr,(IntPtr)Marshal.SizeOf(typeof(Caps)),IntPtr.Zero,IntPtr.Zero),"attributes-caps");
            job=CreateJobObjectW(IntPtr.Zero,null);Check(job!=IntPtr.Zero,"job-create");
            var limits=new Extended();limits.basic.flags=0x2|0x8|0x100|0x2000;limits.basic.processTime=(av?45L:2L)*10000000;limits.basic.processes=1;limits.processMemory=(UIntPtr)((capacity?1536UL:av?768UL:128UL)*1024*1024);
            Check(SetInformationJobObject(job,9,ref limits,(uint)Marshal.SizeOf(typeof(Extended))),"job-limits");
            string block="LOCALAPPDATA="+Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData)+"\0SystemRoot="+Environment.GetFolderPath(Environment.SpecialFolder.Windows)+"\0TEMP="+work+"\0TMP="+work+"\0USERPROFILE="+Environment.GetFolderPath(Environment.SpecialFolder.UserProfile)+"\0windir="+Environment.GetFolderPath(Environment.SpecialFolder.Windows)+"\0\0";env=Marshal.StringToHGlobalUni(block);
            var start=new SX();start.si.cb=Marshal.SizeOf(typeof(SX));start.attributes=list;
            string app=exe,command="\""+exe+"\" "+mode+" "+arguments;
            if(av){
                var security=new SA{size=Marshal.SizeOf(typeof(SA)),inherit=1};output=CreateFileW(Path.Combine(work,"salida.log"),0x40000000,1,ref security,2,0x80,IntPtr.Zero);Check(output!=new IntPtr(-1),"output-create");
                error=CreateFileW(Path.Combine(work,"stderr.log"),0x40000000,1,ref security,2,0x80,IntPtr.Zero);Check(error!=new IntPtr(-1),"error-create");
                handles=Marshal.AllocHGlobal(2*IntPtr.Size);Marshal.WriteIntPtr(handles,output);Marshal.WriteIntPtr(handles,IntPtr.Size,error);Check(UpdateProcThreadAttribute(list,0,(IntPtr)0x20002,handles,(IntPtr)(2*IntPtr.Size),IntPtr.Zero,IntPtr.Zero),"handle-list");
                start.si.flags=0x100;start.si.output=output;start.si.error=error;
                app=Path.Combine(avTools,"clamscan.exe");
                if(mode=="av-eicar"||mode=="av-capacity-eicar")File.WriteAllBytes(Path.Combine(work,"input.txt"),Convert.FromBase64String("WDVPIVAlQEFQWzRcUFpYNTQoUF4pN0NDKTd9JEVJQ0FSLVNUQU5EQVJELUFOVElWSVJVUy1URVNULUZJTEUhJEgrSCo="));
                string database=mode=="av-unavailable"?Path.Combine(work,"absent"):avDefs;
                command="\""+app+"\" --debug --database=\""+database+"\" --tempdir=\""+work+"\" --max-filesize=25M --max-scansize=50M --max-files=2000 --max-recursion=16 --max-scantime=45000 --alert-exceeds-max=yes --no-summary --official-db-only=yes --bytecode-unsigned=no \""+Path.Combine(work,"input.txt")+"\"";
            }
            Check(CreateProcessW(app,new StringBuilder(command),IntPtr.Zero,IntPtr.Zero,av,0x80000|0x4|0x400|0x08000000,env,work,ref start,out pi),"create-suspended");
            Check(AssignProcessToJobObject(job,pi.process),"assign-job");assigned=true;
            IntPtr token;Check(OpenProcessToken(pi.process,8,out token),"token-open");
            try{IntPtr data=Marshal.AllocHGlobal(4096);try{int returned;Check(GetTokenInformation(token,29,data,4096,out returned),"token-container");if(Marshal.ReadInt32(data)!=1)throw new Exception("Token sin aislamiento");Check(GetTokenInformation(token,30,data,4096,out returned),"token-capabilities");if(Marshal.ReadInt32(data)!=0)throw new Exception("Capacidades no permitidas");}finally{Marshal.FreeHGlobal(data);}}finally{CloseHandle(token);}
            var effective=new Extended();Check(QueryInformationJobObject(job,9,ref effective,(uint)Marshal.SizeOf(typeof(Extended)),IntPtr.Zero),"job-effective");
            if(effective.basic.flags!=limits.basic.flags||effective.basic.processTime!=limits.basic.processTime||effective.basic.processes!=1||effective.processMemory!=limits.processMemory)throw new Exception("Limites no efectivos");
            Check(ResumeThread(pi.thread)!=0xffffffff,"resume");
            uint waited=258;var clock=System.Diagnostics.Stopwatch.StartNew();
            while(clock.ElapsedMilliseconds<timeout){waited=WaitForSingleObject(pi.process,50);if(waited!=258)break;if(av&&new FileInfo(Path.Combine(work,"salida.log")).Length+new FileInfo(Path.Combine(work,"stderr.log")).Length>1024*1024){TerminateJobObject(job,123);WaitForSingleObject(pi.process,5000);throw new Exception("Salida QA excesiva");}}
            bool timedOut=waited==258;
            if(timedOut) {Check(TerminateJobObject(job,124),"timeout-kill");Check(WaitForSingleObject(pi.process,5000)==0,"timeout-wait");}
            else Check(waited==0,"wait");
            uint code;Check(GetExitCodeProcess(pi.process,out code),"exit-code");
            var usage=new Extended();Check(QueryInformationJobObject(job,9,ref usage,(uint)Marshal.SizeOf(typeof(Extended)),IntPtr.Zero),"job-accounting");
            result="mode="+mode+";exit="+code+";timeout="+timedOut+";pid="+pi.processId+";result="+(File.Exists(Path.Combine(work,"result.txt"))?File.ReadAllText(Path.Combine(work,"result.txt")):"NONE")+";peakPrivateBytes="+usage.peakProcess.ToUInt64()+";appContainer=true;capabilities=0;processLimit=1;memoryLimitBytes="+effective.processMemory.ToUInt64()+";cpuLimitTicks="+effective.basic.processTime+";killOnJobClose=true;availablePhysicalBeforeBytes="+availablePhysical+";elapsedMilliseconds="+clock.ElapsedMilliseconds;
        } finally {
            if(pi.process!=IntPtr.Zero){if(assigned&&job!=IntPtr.Zero)TerminateJobObject(job,125);else TerminateProcess(pi.process,125);WaitForSingleObject(pi.process,5000);CloseHandle(pi.process);}
            if(pi.thread!=IntPtr.Zero)CloseHandle(pi.thread);if(job!=IntPtr.Zero)CloseHandle(job);
            if(output!=IntPtr.Zero&&output!=new IntPtr(-1))CloseHandle(output);if(error!=IntPtr.Zero&&error!=new IntPtr(-1))CloseHandle(error);if(handles!=IntPtr.Zero)Marshal.FreeHGlobal(handles);
            if(initialized)DeleteProcThreadAttributeList(list);if(list!=IntPtr.Zero)Marshal.FreeHGlobal(list);if(capsPtr!=IntPtr.Zero)Marshal.FreeHGlobal(capsPtr);if(env!=IntPtr.Zero)Marshal.FreeHGlobal(env);
            try{var actions=new System.Collections.Generic.List<Action>{()=>RestoreDirectory(work,workAcl),()=>RestoreFile(exe,exeAcl),()=>RestoreDirectory(root,rootAcl)};if(av){actions.Add(()=>RestoreDirectory(avTools,toolsAcl));actions.Add(()=>RestoreDirectory(avDefs,defsAcl));}foreach(var action in actions)try{action();}catch{restored=false;}}
            finally{deleted=DeleteAppContainerProfile(name);if(sid!=IntPtr.Zero)FreeSid(sid);}
        }
        if(deleted!=0)throw new Exception("No se pudo retirar perfil QA, HRESULT "+deleted);
        if(!restored)throw new Exception("No se pudieron restaurar todas las DACL de QA");
        return result+";profileDeleted=true";
    }
}
