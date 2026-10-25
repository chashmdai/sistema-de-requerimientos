#!/usr/bin/env python3
"""Prepara adaptadores sobre workers existentes; no crea contenedores ni modifica el Formulario Público."""
import argparse,hashlib,os,re,subprocess
from pathlib import Path
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('private',type=Path);parser.add_argument('--inspector-socket',type=Path,required=True);parser.add_argument('--antivirus-socket',type=Path,required=True)
parser.add_argument('--inspector-container',default='siger-multimedia-lab-inspector-1');parser.add_argument('--antivirus-container',default='siger-multimedia-lab-antivirus-1')
args=parser.parse_args();p=args.private.resolve();os.umask(0o077)
if not p.is_dir() or any((v/'.git').exists() for v in [p,*p.parents]):parser.error('Directorio privado existente fuera de Git requerido')
file=p/'application.properties';props=dict(l.split('=',1) for l in file.read_text().splitlines() if '=' in l and not l.startswith('#'))
if not(props.get('server.address')=='127.0.0.1' and props.get('server.port')=='20443' and props.get('institucional.nucleo.entorno')=='LABORATORIO' and '/robust_inst_formulario_lab?' in props.get('institucional.db.url','')):parser.error('Solo configuración institucional aislada de laboratorio')
for name in [args.inspector_container,args.antivirus_container]:
 if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]{1,100}',name):parser.error('Identificador de contenedor inválido')
for socket in [args.inspector_socket,args.antivirus_socket]:
 if not socket.is_absolute() or not socket.is_socket() or socket.is_symlink():parser.error('Socket Unix existente requerido')
try:
 helper=subprocess.run(['docker','exec',args.inspector_container,'cat','/opt/sandbox'],capture_output=True,check=True).stdout
 if hashlib.sha256(helper).hexdigest()!='9031ed433ae36c282b07640b90027bd533935e7469330e0364095f00a73792d1':parser.error('Helper distinto del artefacto revisado; no se instala automáticamente')
 config=subprocess.run(['docker','exec',args.antivirus_container,'cat','/etc/clamd-media.conf'],capture_output=True,check=True).stdout
except subprocess.SubprocessError:parser.error('No se pudieron leer los artefactos existentes; no se imprimen salidas privadas')
owner=p.stat();directory=p/'bin';directory.mkdir(mode=0o700,exist_ok=True)
def guardar(target,data,mode):
 temp=target.with_name(target.name+'.documental-next');temp.write_bytes(data);temp.chmod(mode);os.chown(temp,owner.st_uid,owner.st_gid);os.replace(temp,target)
# No se sobrescribe un ejecutable en uso; publicación atómica del mismo artefacto revisado.
if not (directory/'sandbox').exists() or (directory/'sandbox').read_bytes()!=helper:guardar(directory/'sandbox',helper,0o500)
guardar(p/'clamd-documental.conf',config,0o600);os.chown(directory,owner.st_uid,owner.st_gid)
settings={'institucional.documental.habilitada':'true','institucional.documental.antivirus-socket':str(args.antivirus_socket),'institucional.documental.inspector-socket':str(args.inspector_socket),'institucional.documental.antivirus-configuracion':str(p/'clamd-documental.conf'),'institucional.documental.sandbox':str(directory/'sandbox')}
lines=[l for l in file.read_text().splitlines() if l.split('=',1)[0] not in settings];lines.extend(k+'='+v for k,v in settings.items());guardar(file,('\n'.join(lines)+'\n').encode(),0o600)
print('Adaptadores documentales preparados sobre workers existentes; reiniciar únicamente la instancia institucional cuando corresponda.')