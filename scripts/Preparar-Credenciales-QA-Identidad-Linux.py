#!/usr/bin/env python3
"""Prepara credenciales sintéticas distintas para QA, exclusivamente en custodia Linux privada."""
import argparse,json,os,secrets,stat
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__);p.add_argument('private',type=Path);a=p.parse_args();os.umask(0o077)
root=a.private.absolute()
if root!=root.resolve() or not root.is_relative_to(Path.home()/'.local/share/siger') or any((d/'.git').exists() for d in [root,*root.parents]):p.error('Custodia Linux privada requerida')
if stat.S_IMODE(root.stat().st_mode)!=0o700:p.error('El directorio privado debe tener modo 0700')
props=dict(l.split('=',1) for l in (root/'application.properties').read_text().splitlines() if '=' in l and not l.startswith('#'))
if not(props.get('server.address')=='127.0.0.1' and props.get('server.port')=='20443' and props.get('institucional.nucleo.entorno')=='LABORATORIO' and props.get('institucional.identidad.habilitada')=='true'):p.error('Solo laboratorio individual existente')
file=root/'identidad-qa.json'
if file.is_symlink():p.error('No se admiten enlaces')
names=['admin','regionala','regionalb','principal','prof','otraunidad','coordinador']
if file.exists():
 data=json.loads(file.read_text());assert set(data)==set(names)
 assert all(v['usuario']=='qa.identidad.'+k and 16<=len(v['password'])<=72 for k,v in data.items())
 assert len({v['password'] for v in data.values()})==len(names)
 os.chmod(file,0o600)
else:
 data={name:{'usuario':'qa.identidad.'+name,'password':secrets.token_urlsafe(32)} for name in names}
 fd=os.open(file,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
 with os.fdopen(fd,'w') as f:json.dump(data,f);f.flush();os.fsync(f.fileno())
print('Credenciales sintéticas individuales preparadas con modo 0600. Valores conservados fuera de Git y de la salida.')
