param()
$ErrorActionPreference='Stop'
if(-not $IsWindows){throw 'Solo Windows nativo'}
$source=@"
using System;using System.Runtime.InteropServices;
public static class SigerAislamientoProbe {
 [DllImport("userenv.dll",CharSet=CharSet.Unicode)]static extern int CreateAppContainerProfile(string n,string d,string description,IntPtr caps,uint count,out IntPtr sid);
 [DllImport("userenv.dll",CharSet=CharSet.Unicode)]static extern int DeleteAppContainerProfile(string n);
 [DllImport("advapi32.dll")]static extern IntPtr FreeSid(IntPtr sid);
 [DllImport("kernel32.dll",CharSet=CharSet.Unicode,SetLastError=true)]static extern IntPtr CreateJobObjectW(IntPtr security,string name);
 [DllImport("kernel32.dll",SetLastError=true)]static extern bool SetInformationJobObject(IntPtr job,int info,ref Extended limits,uint len);
 [DllImport("kernel32.dll")]static extern bool CloseHandle(IntPtr handle);
 [StructLayout(LayoutKind.Sequential)]struct Basic {public long processTime,jobTime;public uint flags;public UIntPtr minWorking,maxWorking;public uint processes;public UIntPtr affinity;public uint priority,scheduling;}
 [StructLayout(LayoutKind.Sequential)]struct Io {public ulong readOps,writeOps,otherOps,readBytes,writeBytes,otherBytes;}
 [StructLayout(LayoutKind.Sequential)]struct Extended {public Basic basic;public Io io;public UIntPtr processMemory,jobMemory,peakProcess,peakJob;}
 public static string Profile(){string name="Nexbyte.Siger.U45.Probe."+Guid.NewGuid().ToString("N");IntPtr sid;int hr=CreateAppContainerProfile(name,"SIGER U4.5 probe","Sonda transitoria sin capacidades ni documentos",IntPtr.Zero,0,out sid);if(hr!=0)return "CREATE_HRESULT_"+((uint)hr).ToString("X8");try{return "CREATE_OK_DELETE_HRESULT_"+((uint)DeleteAppContainerProfile(name)).ToString("X8");}finally{if(sid!=IntPtr.Zero)FreeSid(sid);}}
 public static string Job(){IntPtr h=CreateJobObjectW(IntPtr.Zero,null);if(h==IntPtr.Zero)return "CREATE_ERROR_"+Marshal.GetLastWin32Error();try{var limits=new Extended();limits.basic.flags=0x8|0x100|0x2000;limits.basic.processes=1;limits.processMemory=(UIntPtr)(128UL*1024*1024);return SetInformationJobObject(h,9,ref limits,(uint)Marshal.SizeOf<Extended>())?"LIMITS_ACCEPTED_NO_CHILD_EXECUTED":"LIMITS_ERROR_"+Marshal.GetLastWin32Error();}finally{CloseHandle(h);}}
}
"@
Add-Type -TypeDefinition $source
$results=@{date='2026-10-26';appContainer=[SigerAislamientoProbe]::Profile();jobObject=[SigerAislamientoProbe]::Job();newServices=0;capabilitiesRequested=0;untrustedDocumentsProcessed=0;negativeIsolationTestsExecuted=0;securityEquivalenceVerified=$false;notes='Disponibilidad de API no acredita aislamiento; no es un worker ni un runtime documental'}
$results|ConvertTo-Json|Tee-Object C:\req\evidencias\sonda-aislamiento-windows-2026-10-26.json