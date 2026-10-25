#!/usr/bin/env python3
"""Habilita exclusivamente identidad individual en el laboratorio privado existente. No imprime credenciales."""
import argparse,json,os,secrets,stat
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__);p.add_argument('private',type=Path);a=p.parse_args();os.umask(0o077)
root=a.private.absolute()
if root!=root.resolve() or not root.is_dir() or any((v/'.git').exists() for v in [root,*root.parents]):p.error('Custodia privada existente sin enlaces requerida')
home=Path.home()/'.local/share/siger'
if not root.is_relative_to(home):p.error('Solo directorio privado del usuario bajo .local/share/siger')
props=root/'application.properties';raw=props.read_text();v=dict(l.split('=',1) for l in raw.splitlines() if '=' in l and not l.startswith('#'))
if not(v.get('server.address')=='127.0.0.1' and v.get('server.port')=='20443' and v.get('server.ssl.enabled')=='true' and v.get('institucional.nucleo.entorno')=='LABORATORIO' and '/robust_inst_formulario_lab?' in v.get('institucional.db.url','')):p.error('Solo laboratorio institucional aislado')
bootstrap=root/'identidad-bootstrap.json';done=root/'identidad-bootstrap.json.aplicado'
backup=root/'application.antes-identidad.properties'
if not backup.exists():
 fd=os.open(backup,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
 with os.fdopen(fd,'w') as f:f.write(raw);f.flush();os.fsync(f.fileno())
if not bootstrap.exists() and not done.exists():
 data={'usuario':'ti.lab','nombre':'Administración TI sintética','correo':'ti.lab@example.invalid','cargo':'Soporte de laboratorio','unidad':None,'sede':None,'password':secrets.token_urlsafe(32)}
 fd=os.open(bootstrap,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
 with os.fdopen(fd,'w') as f:json.dump(data,f);f.flush();os.fsync(f.fileno())
for file in (bootstrap,done):
 if file.exists() and (file.is_symlink() or stat.S_IMODE(file.stat().st_mode)!=0o600):p.error('Permisos iniciales inválidos')
# Mantener sin uso la antigua credencial privada: no reemplazarla ni reutilizarla.
new=[l for l in raw.splitlines() if not l.startswith(('institucional.identidad.habilitada=','institucional.identidad.legado-pruebas=','institucional.identidad.bootstrap.archivo='))]
new+=['institucional.identidad.habilitada=true','institucional.identidad.legado-pruebas=false','institucional.identidad.bootstrap.archivo='+str(bootstrap)]
tmp=root/'application.identidad.next'
fd=os.open(tmp,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
with os.fdopen(fd,'w') as f:f.write('\n'.join(new)+'\n');f.flush();os.fsync(f.fileno())
os.replace(tmp,props)
print('Identidad individual preparada; respaldo y habilitación inicial en custodia privada. No se han impreso secretos ni iniciado servicios.')
