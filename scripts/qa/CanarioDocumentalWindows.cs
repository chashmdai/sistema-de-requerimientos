// Exclusivamente QA. Se instala como clamscan.exe sintetico con manifiesto propio, nunca como AV real.
using System;using System.IO;using System.Net;using System.Net.Sockets;using System.Diagnostics;using System.Runtime.InteropServices;using System.Threading;using System.Security.AccessControl;
public static class CanarioDocumentalWindows {
 [DllImport("kernel32.dll",SetLastError=true)]static extern IntPtr VirtualAlloc(IntPtr address,UIntPtr size,uint type,uint protect);
 [DllImport("kernel32.dll")]static extern bool VirtualFree(IntPtr address,UIntPtr size,uint type);
 static volatile int value=1;
 static bool Connect(string host,int port){using(var c=new TcpClient(host.Contains(":")?AddressFamily.InterNetworkV6:AddressFamily.InterNetwork)){try{var t=c.ConnectAsync(host,port);return t.Wait(700)&&c.Connected;}catch(AggregateException){return false;}}}
 public static int Main(string[] args){try{
  string input=args[args.Length-1];var lines=File.ReadAllLines(input);string mode=lines[0];
  Console.WriteLine("CANARIO_INICIADO");Console.Out.Flush();
  if(mode=="crash")Environment.Exit(37);
  if(mode=="timeout"){Thread.Sleep(180000);return 91;}
  if(mode=="cpu"){while(true)value=unchecked(value*1664525+1013904223);}
  if(mode=="output"){for(int i=0;i<512;i++)Console.Write(new string('X',8192));Console.Out.Flush();Thread.Sleep(180000);return 92;}
  if(mode=="memory"){
   var small=VirtualAlloc(IntPtr.Zero,(UIntPtr)(4UL*1024*1024),0x3000,4);if(small==IntPtr.Zero)return 20;VirtualFree(small,UIntPtr.Zero,0x8000);
   // Reserva/commit sin tocar páginas. El Job debe rechazarlo antes de consumir RAM física.
   var big=VirtualAlloc(IntPtr.Zero,(UIntPtr)(2UL*1024*1024*1024),0x3000,4);if(big!=IntPtr.Zero){VirtualFree(big,UIntPtr.Zero,0x8000);return 21;}Console.WriteLine(input+": OK");return 0;
  }
  if(mode=="probe"){
   try{File.ReadAllText(lines[1]);return 10;}catch(UnauthorizedAccessException){}
   try{File.WriteAllText(lines[2],"NO");return 11;}catch(UnauthorizedAccessException){}
   try{File.AppendAllText(input,"NO");return 12;}catch(UnauthorizedAccessException){}
   try{var acl=File.GetAccessControl(lines[1]);acl.AddAccessRule(new FileSystemAccessRule(new System.Security.Principal.SecurityIdentifier("S-1-1-0"),FileSystemRights.FullControl,AccessControlType.Allow));File.SetAccessControl(lines[1],acl);return 13;}catch(UnauthorizedAccessException){}
   if(Connect("127.0.0.1",int.Parse(lines[3]))||Connect("::1",int.Parse(lines[4])))return 14;
   if(lines.Length>6&&Connect(lines[6],443))return 17;
   try{using(var u=new UdpClient()){u.Send(new byte[]{81,65},2,"127.0.0.1",int.Parse(lines[5]));}}catch(SocketException){}
   try{var child=Process.Start(new ProcessStartInfo(System.Reflection.Assembly.GetExecutingAssembly().Location,"noop"){UseShellExecute=false,CreateNoWindow=true});child.Kill();child.WaitForExit();return 15;}catch(System.ComponentModel.Win32Exception e){if(e.NativeErrorCode!=5&&e.NativeErrorCode!=1816)return 16;}
   Console.WriteLine("LECTURA_ESCRITURA_DACL_RED_HIJO_RECHAZADOS");Console.WriteLine(input+": OK");return 0;
  }
  if(mode=="sleep"){Thread.Sleep(180000);return 93;}
  Console.WriteLine(input+": OK");return 0;
 }catch(Exception){return 99;}}
}
