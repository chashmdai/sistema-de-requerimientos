#!/usr/bin/env python3
"""Configura notificaciones del laboratorio existente; secreto solo desde el archivo privado autorizado."""
import argparse,os,shutil,secrets
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__);p.add_argument('private',type=Path);p.add_argument('--modo',choices=['DESACTIVADO','SIMULADO','GRAPH'],default='DESACTIVADO');p.add_argument('--secreto-origen',type=Path,default=Path('/mnt/c/maven/sclient.txt'));a=p.parse_args();os.umask(0o077)
root=a.private.absolute()
if root!=root.resolve() or not root.is_relative_to(Path.home()/'.local/share/siger') or not root.is_dir() or any((d/'.git').exists() for d in [root,*root.parents]):p.error('Solo custodia privada Linux')
cfg=root/'application.properties';raw=cfg.read_text();props=dict(x.split('=',1) for x in raw.splitlines() if '=' in x and not x.startswith('#'))
if not(props.get('server.address')=='127.0.0.1' and props.get('server.port')=='20443' and props.get('server.ssl.enabled')=='true' and props.get('institucional.identidad.habilitada')=='true' and props.get('institucional.nucleo.entorno')=='LABORATORIO' and '/robust_inst_formulario_lab?' in props.get('institucional.db.url','')):p.error('Solo laboratorio individual aislado 20443')
private=root/'notificaciones';private.mkdir(mode=0o700,exist_ok=True)
if private.is_symlink() or private.stat().st_mode&0o777!=0o700:p.error('Custodia de correo inválida')
secret=private/'graph-client-secret.txt'
if not secret.exists():
 value=a.secreto_origen.read_bytes().strip()
 if not 20<=len(value)<=512 or any(x<32 for x in value):p.error('Material reservado inválido')
 fd=os.open(secret,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
 with os.fdopen(fd,'wb') as f:f.write(value);f.flush();os.fsync(f.fileno())
if secret.is_symlink() or secret.stat().st_mode&0o777!=0o600:p.error('Secreto requiere modo 0600')
allow=private/'allowlist.txt'
expected='nexbyte.proveedor@defensorianinez.cl\n'
if allow.exists():
 if allow.is_symlink() or allow.read_text()!=expected or allow.stat().st_mode&0o777!=0o600:p.error('Allowlist privada no coincide con la campaña autorizada')
else:
 fd=os.open(allow,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
 with os.fdopen(fd,'w') as f:f.write(expected);f.flush();os.fsync(f.fileno())
backup=root/'application.antes-notificaciones.properties'
if not backup.exists():
 fd=os.open(backup,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
 with os.fdopen(fd,'w') as f:f.write(raw)
old=root/'runtime-antes-notificaciones.jar'
if not old.exists():shutil.copyfile(root/'runtime.jar',old);old.chmod(0o600)
values={'institucional.notificaciones.habilitadas':'true','institucional.notificaciones.correo.modo':a.modo,'institucional.notificaciones.correo.automatico':'false','institucional.notificaciones.correo.allowlist':str(allow),'institucional.notificaciones.correo.campana':'u4_20261025_v1','institucional.notificaciones.graph.tenant':'8f0223aa-42e9-47b1-b9bc-e09d58b2cd8a','institucional.notificaciones.graph.client':'8bcb0433-067a-48a2-8518-cb9472c7a729','institucional.notificaciones.graph.remitente':'siger@defensorianinez.cl','institucional.notificaciones.graph.secreto':str(secret)}
lines=[x for x in raw.splitlines() if not x.startswith('institucional.notificaciones.')]+[k+'='+v for k,v in values.items()]
temp=root/'application.notificaciones.next'
fd=os.open(temp,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
with os.fdopen(fd,'w') as f:f.write('\n'.join(lines)+'\n');f.flush();os.fsync(f.fileno())
os.replace(temp,cfg)
print('Notificaciones preparadas en modo',a.modo,'sin envíos automáticos. Secreto reservado fuera de Git; allowlist única y campaña persistente de máximo cuatro intentos.')
