// Exclusivamente QA: no procesa documentos ni contiene secretos.
using System;
using System.ComponentModel;
using System.Diagnostics;
using System.IO;
using System.Net.Sockets;
using System.Runtime.InteropServices;
using System.Threading;
public static class Canario {
    static volatile int value=1;
    [DllImport("advapi32.dll",SetLastError=true)] static extern bool OpenProcessToken(IntPtr p,uint access,out IntPtr token);
    [DllImport("advapi32.dll",SetLastError=true)] static extern bool GetTokenInformation(IntPtr t,int info,out int value,int size,out int returned);
    [DllImport("advapi32.dll",SetLastError=true)] static extern bool GetTokenInformation(IntPtr t,int info,IntPtr value,int size,out int returned);
    [DllImport("kernel32.dll")] static extern bool CloseHandle(IntPtr h);
    [DllImport("amsi.dll",CharSet=CharSet.Unicode)] static extern int AmsiInitialize(string app,out IntPtr context);
    [DllImport("amsi.dll",CharSet=CharSet.Unicode)] static extern int AmsiScanBuffer(IntPtr context,byte[] buffer,uint length,string name,IntPtr session,out uint result);
    [DllImport("amsi.dll")] static extern void AmsiUninitialize(IntPtr context);
    [DllImport("ole32.dll")] static extern int CoInitializeEx(IntPtr reserved,uint mode);
    [DllImport("ole32.dll")] static extern void CoUninitialize();
    static int Amsi(bool control){
        int co=CoInitializeEx(IntPtr.Zero,0);if(co<0){File.WriteAllText("result.txt","COM_INIT_HRESULT_"+unchecked((uint)co).ToString("X8"));return 42;}
        try{return AmsiInicializado(control);}finally{CoUninitialize();}
    }
    static int AmsiInicializado(bool control){
        IntPtr ctx;int hr=AmsiInitialize("SIGER_LAB_QA_SINTETICO",out ctx);
        if(hr!=0){File.WriteAllText("result.txt","AMSI_INIT_HRESULT_"+unchecked((uint)hr).ToString("X8"));return 40;}
        try{byte[] b=control?Convert.FromBase64String("WDVPIVAlQEFQWzRcUFpYNTQoUF4pN0NDKTd9JEVJQ0FSLVNUQU5EQVJELUFOVElWSVJVUy1URVNULUZJTEUhJEgrSCo="):System.Text.Encoding.UTF8.GetBytes("SINTETICO");
            try{uint result;hr=AmsiScanBuffer(ctx,b,(uint)b.Length,"SIGER_LAB_CONTROL.txt",IntPtr.Zero,out result);File.WriteAllText("result.txt","AMSI_SCAN_HRESULT_"+unchecked((uint)hr).ToString("X8")+";RESULT_"+result);return hr==0&&(control?result>=32768:result<=1)?0:41;}finally{Array.Clear(b,0,b.Length);}
        }finally{AmsiUninitialize(ctx);}
    }
    public static int Main(string[] args) {
        try {
            if(args[0]=="amsi-init-only") {int co=CoInitializeEx(IntPtr.Zero,0);if(co<0)return 42;try{IntPtr ctx;int hr=AmsiInitialize("SIGER_LAB_SOLO_DISPONIBILIDAD",out ctx);Console.WriteLine("AMSI_INIT_ONLY_HRESULT_"+unchecked((uint)hr).ToString("X8"));if(hr==0)AmsiUninitialize(ctx);return hr==0?0:40;}finally{CoUninitialize();}}
            if(args[0]=="noop") return 90;
            if(args[0]=="timeout") {Thread.Sleep(30000);return 91;}
            if(args[0]=="crash") Environment.Exit(37);
            if(args[0]=="memory") {try {var b=new byte[384*1024*1024];for(int i=0;i<b.Length;i+=4096)b[i]=1;return 92;}catch(OutOfMemoryException){return 0;}}
            if(args[0]=="cpu") {while(true) {value=unchecked(value*1664525+1013904223);}}
            IntPtr token;int container,returned;
            if(!OpenProcessToken(Process.GetCurrentProcess().Handle,8,out token)) return 10;
            try {if(!GetTokenInformation(token,29,out container,4,out returned)||container!=1) return 11;
                IntPtr caps=Marshal.AllocHGlobal(4096);try{if(!GetTokenInformation(token,30,caps,4096,out returned)||Marshal.ReadInt32(caps)!=0)return 19;}finally{Marshal.FreeHGlobal(caps);}
            }finally{CloseHandle(token);}
            if(File.ReadAllText("input.txt")!="SINTETICO") return 12;
            if(args[0]=="amsi-clean"||args[0]=="amsi-control")return Amsi(args[0]=="amsi-control");
            File.WriteAllText("stage.txt","input");
            try {File.ReadAllText(args[1]);return 13;}catch(UnauthorizedAccessException){}
            File.WriteAllText("stage.txt","read-denied");
            try {File.WriteAllText(args[2],"NO");return 14;}catch(UnauthorizedAccessException){}
            File.WriteAllText("stage.txt","write-denied");
            string network="NETWORK_TIMEOUT_NO_CONNECTION";
            try {using(var socket=new Socket(AddressFamily.InterNetwork,SocketType.Stream,ProtocolType.Tcp)){var pending=socket.BeginConnect("127.0.0.1",int.Parse(args[3]),null,null);if(pending.AsyncWaitHandle.WaitOne(750)){socket.EndConnect(pending);return 15;}}}
            catch(SocketException e){if(e.NativeErrorCode!=10013)return 16;network="NETWORK_10013";}
            File.WriteAllText("stage.txt","network-denied");
            try {var child=Process.Start(new ProcessStartInfo(Process.GetCurrentProcess().MainModule.FileName,"noop"){UseShellExecute=false,CreateNoWindow=true});child.Kill();child.WaitForExit();return 17;}
            catch(Win32Exception e){if(e.NativeErrorCode!=5 && e.NativeErrorCode!=1816)return 18;}
            File.WriteAllText("result.txt","APP_CONTAINER;READ_DENIED;WRITE_DENIED;"+network+";CHILD_DENIED");
            return 0;
        } catch(Exception e) {return 100+(e is UnauthorizedAccessException?1:e is FileNotFoundException?2:3);}
    }
}
