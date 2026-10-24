#!/usr/bin/env python3
"""Arranque aislado del consumidor institucional; nunca modifica la instancia pública."""
import argparse,json,os,signal,socket,subprocess,time,shutil,ssl,urllib.request
from pathlib import Path
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('private',type=Path);parser.add_argument('--jar',type=Path);parser.add_argument('--migrar',action='store_true');parser.add_argument('--detener',action='store_true')
args=parser.parse_args();p=args.private.resolve();os.umask(0o077)
if not p.is_dir() or any((v/'.git').exists() for v in [p,*p.parents]):parser.error('Directorio privado existente fuera de Git requerido')
props=dict(line.split('=',1) for line in (p/'application.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
if not (props.get('server.address')=='127.0.0.1' and props.get('server.port')=='20443' and props.get('server.ssl.enabled')=='true' and props.get('institucional.nucleo.entorno')=='LABORATORIO' and props.get('institucional.formulario-completo.habilitado')=='true' and props.get('institucional.formulario-completo.automatico')=='false' and props.get('institucional.v2.habilitada')=='false' and '/robust_inst_formulario_lab?' in props.get('institucional.db.url','') and props.get('institucional.formulario-completo.origen-publico')=='https://claveunica.formularioadulto.defensorianinez.gob.cl:19443'):parser.error('Solo laboratorio institucional aislado 20443 con origen multimedia 19443')
jar=p/'runtime.jar';pidfile=p/'app.pid'
if pidfile.exists():
 pid=int(pidfile.read_text());proc=Path(f'/proc/{pid}')
 if proc.exists():
  if str(jar).encode() not in (proc/'cmdline').read_bytes() or str(p/'application.properties').encode() not in (proc/'environ').read_bytes():parser.error('El PID no corresponde a este laboratorio')
  if not args.detener:parser.error('Proceso existente; no se sobrescribe un JAR activo')
  os.kill(pid,signal.SIGTERM)
  for _ in range(40):
   if not proc.exists():break
   time.sleep(.25)
  print('Detención solicitada exclusivamente al laboratorio institucional');raise SystemExit(0)
if args.detener:raise SystemExit('El laboratorio institucional ya está detenido')
with socket.socket() as sock:
 if sock.connect_ex(('127.0.0.1',20443))==0:parser.error('Puerto ocupado; no se altera ningún servicio')
if args.jar:
 if not args.jar.is_file():parser.error('JAR compilado requerido')
 shutil.copyfile(args.jar,p/'runtime.jar.next');os.replace(p/'runtime.jar.next',jar)
if not jar.is_file():parser.error('Falta JAR privado; proporcionar --jar')
if args.migrar:
 secrets=json.loads((p/'db-secrets.json').read_text())['robust_inst_formulario_lab']
 env=dict(os.environ,MIGRATION_DB_URL=props['institucional.db.url'],MIGRATION_DB_USER=secrets['migration_user'],MIGRATION_DB_PASSWORD=secrets['migration_password'],LOCAL_DB_ALLOWED='true')
 with (p/'migration.log').open('ab') as log:result=subprocess.run(['java','-Xmx256m','-jar',str(jar),'--migrate'],env=env,stdout=log,stderr=subprocess.STDOUT)
 if result.returncode:raise SystemExit('Migración no completada; revisar diagnóstico privado')
with (p/'app.log').open('ab') as log:process=subprocess.Popen(['java','-Djava.net.preferIPv4Stack=true','-Xms64m','-Xmx384m','-jar',str(jar)],env=dict(os.environ,SPRING_CONFIG_ADDITIONAL_LOCATION='file:'+str(p/'application.properties')),stdin=subprocess.DEVNULL,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
pidfile.write_text(str(process.pid))
tls=ssl.create_default_context(cafile=props['institucional.formulario-completo.ca-certificado'])
for _ in range(40):
 if process.poll() is not None:raise SystemExit('Arranque no completado; revisar app.log privado')
 try:
  with urllib.request.urlopen('https://claveunica.formularioadulto.defensorianinez.gob.cl:20443/api/estado',context=tls,timeout=1) as response:
   if response.status==200:break
 except OSError:pass
 time.sleep(.25)
else:raise SystemExit('HTTPS todavía no disponible; no ejecutar verificadores con credenciales hasta revisar el laboratorio')
print('Laboratorio institucional listo en HTTPS loopback 20443; PID',process.pid)