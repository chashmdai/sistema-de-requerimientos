#!/usr/bin/env python3
"""Fixture HTTPS aislado con el código publicado; no modifica ni arranca los laboratorios existentes."""
from pathlib import Path
import argparse,subprocess,zipfile,os,json,time,hashlib,shutil
BASELINE='b6ab36af4357f6852f035466b4653da97d267e00'
JAR_SHA256='d859b952eec59fbd1564a370e71c80cdbd2137472739a7dc761dfd2b4d2bd779'
def call(args,**kwargs):return subprocess.run(args,check=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,**kwargs).stdout
def main():
 ap=argparse.ArgumentParser();ap.add_argument('--id',required=True);ap.add_argument('--public-repo',default='/home/nexbyteproveedor/proyectos/formulario-web');a=ap.parse_args()
 import re
 if not re.fullmatch(r'[a-f0-9-]{36}',a.id):raise ValueError('Run inválido')
 repo=Path(a.public_repo);win=Path('/mnt/c/req/testdata')/('sin-agente-'+a.id)
 if call(['git','-C',str(repo),'rev-parse','HEAD']).decode().strip()!=BASELINE or call(['git','-C',str(repo),'status','--porcelain']).strip():raise ValueError('Baseline público distinto o modificado')
 root=Path('/home/nexbyteproveedor/.local/share/siger')/('sin-agente-'+a.id)
 root.mkdir(mode=0o700)
 shutil.copyfile(win/'institution-keys.json',root/'institution-keys.json');os.chmod(root/'institution-keys.json',0o600)
 jar=repo/'.local/agente-tls-final-target/formulario-publico-0.1.0-SNAPSHOT.jar'
 if hashlib.sha256(jar.read_bytes()).hexdigest()!=JAR_SHA256:raise ValueError('Artefacto público distinto; revisar procedencia antes de repetir')
 binary=root/'codigo';binary.mkdir(mode=0o700)
 with zipfile.ZipFile(jar) as z:
  for name in z.namelist():
   if name.startswith(('BOOT-INF/classes/','BOOT-INF/lib/')) and not name.endswith('/'):
    rel=Path(name)
    if '..' in rel.parts:raise ValueError('Artefacto inseguro')
    p=binary/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(z.read(name))
 classes=binary/'BOOT-INF/classes';libs=binary/'BOOT-INF/lib'
 h2=Path('/home/nexbyteproveedor/.m2/repository/com/h2database/h2/2.4.240/h2-2.4.240.jar')
 cp=str(classes)+':'+str(libs/'*')+':'+str(h2)
 source=Path('/mnt/c/req/repo/scripts/qa/PublicoSinAgenteFixture.java');qa=root/'qa';qa.mkdir(mode=0o700)
 (root/'compile.log').write_bytes(call(['javac','-encoding','UTF-8','-cp',cp,'-d',str(qa),str(source)]))
 for args in [
  ['openssl','req','-x509','-newkey','rsa:2048','-nodes','-sha256','-days','2','-subj','/CN=CA QA SIN AGENTE','-addext','basicConstraints=critical,CA:TRUE','-keyout',str(root/'ca-key.pem'),'-out',str(root/'ca.pem')],
  ['openssl','req','-newkey','rsa:2048','-nodes','-sha256','-subj','/CN=localhost','-keyout',str(root/'leaf-key.pem'),'-out',str(root/'leaf.csr')]]:
  call(args)
 (root/'extensions.txt').write_text('subjectAltName=DNS:localhost,IP:127.0.0.1\nbasicConstraints=critical,CA:FALSE\nkeyUsage=critical,digitalSignature,keyEncipherment\nextendedKeyUsage=serverAuth\n')
 call(['openssl','x509','-req','-in',str(root/'leaf.csr'),'-CA',str(root/'ca.pem'),'-CAkey',str(root/'ca-key.pem'),'-CAcreateserial','-days','2','-sha256','-extfile',str(root/'extensions.txt'),'-out',str(root/'leaf.pem')])
 call(['openssl','pkcs12','-export','-name','qa','-inkey',str(root/'leaf-key.pem'),'-in',str(root/'leaf.pem'),'-certfile',str(root/'ca.pem'),'-passout','pass:synthetic-lab','-out',str(root/'https.p12')])
 for p in root.iterdir():
  if p.is_file():p.chmod(0o600)
 (root/'provenance.json').write_text(json.dumps(dict(commit=BASELINE,jarSHA256=hashlib.sha256(jar.read_bytes()).hexdigest(),sourceSHA256=hashlib.sha256(source.read_bytes()).hexdigest(),h2Publico=True,mysqlInstitucional=True)))
 log=(root/'server.log').open('wb');process=None
 try:
  process=subprocess.Popen(['java','-Djava.net.preferIPv4Stack=true','-Xmx256m','-XX:-UsePerfData','-cp',str(qa)+':'+cp,'cl.nexbyte.siger.qa.PublicoSinAgenteFixture',str(root)],stdout=log,stderr=subprocess.STDOUT,env={**os.environ,'TZ':'UTC'})
  (root/'pid').write_text(str(process.pid))
  until=time.monotonic()+45
  while not (root/'ready.json').exists() and time.monotonic()<until:
   if process.poll() is not None:raise RuntimeError('Fixture detenido: consultar server.log privado')
   time.sleep(.15)
  if not (root/'ready.json').exists():raise RuntimeError('Timeout de arranque')
  for src,dest in [('ready.json','conexion-publico.json'),('public-keys.json','public-keys.json'),('ca.pem','ca-publico.pem')]:shutil.copyfile(root/src,win/dest)
  (win/'fixture-root.txt').write_text(str(root))
  print(json.dumps(dict(ok=True,fase='fixture-https',commit=BASELINE,loopback=True,puerto=json.loads((root/'ready.json').read_text())['origin'].rsplit(':',1)[1],agente=False,retiroPublico=False)),flush=True)
  # Solo proceso QA, acotado; el coordinador solicita cierre por HTTPS.
  try:process.wait(timeout=600)
  except subprocess.TimeoutExpired:process.terminate();process.wait(timeout=15)
 finally:
  if process is not None and process.poll() is None:process.terminate();process.wait(timeout=15)
  log.close()
if __name__=='__main__':
 os.umask(0o077)
 main()
