#!/usr/bin/env python3
"""Transferencia inicial, de solo lectura en origen, del respaldo lógico y objetos a C:/req. Fase Linux, no runtime nativo."""
import subprocess,json,hashlib
from pathlib import Path
base=Path('/home/nexbyteproveedor/.local/share/siger/institucional-completo-lab')
out=Path('/mnt/c/req/evidencias')
# Read-only logical snapshot; source runtime and database are not stopped or reconfigured.
cmd=['docker','exec','siger-mysql-lab-mysql-1','mysqldump','--defaults-extra-file=/run/secrets/root.cnf','--single-transaction','--skip-lock-tables','--no-tablespaces','--set-gtid-purged=OFF','--column-statistics=0','--hex-blob','--skip-comments','--no-create-info','--skip-add-locks','--skip-disable-keys','--complete-insert','--order-by-primary','--ignore-table=robust_inst_formulario_lab.flyway_schema_history','robust_inst_formulario_lab']
dump=Path('/mnt/c/req/config/origen-u4-datos.sql')
if dump.exists() or dump.with_suffix('.sql.dpapi').exists():raise SystemExit('Snapshot existente: no sobrescribir')
with dump.open('xb') as f:r=subprocess.run(cmd,stdout=f,stderr=subprocess.PIPE)
if r.returncode:raise SystemExit('Respaldo no completado; error omitido')
def query(sql):
 r=subprocess.run(['docker','exec','-i','siger-mysql-lab-mysql-1','mysql','--defaults-extra-file=/run/secrets/root.cnf','--default-character-set=utf8mb4','-N','--raw'],input=sql,text=True,capture_output=True)
 if r.returncode:raise SystemExit('Inventario SQL no completado')
 return [json.loads(line) for line in r.stdout.splitlines()]
files=query("SELECT JSON_OBJECT('key',storage_key,'size',size_bytes,'sha256',sha256,'kind','original') FROM robust_inst_formulario_lab.institution_document UNION ALL SELECT JSON_OBJECT('key',storage_key,'size',size_bytes,'sha256',sha256,'kind','page') FROM robust_inst_formulario_lab.document_page;")
objects=base/'archivo/objects';dest=Path('/mnt/c/req/storage/archivo/objects')
if dest.exists():raise SystemExit('Destino documental existente: no sobrescribir')
for entry in files:
 rel=Path(entry['key']+'.bin');src=objects/rel;dst=dest/rel
 if not src.resolve().is_relative_to(objects) or src.is_symlink():raise SystemExit('Referencia no permitida')
 with src.open('rb') as f:assert hashlib.file_digest(f,'sha256').hexdigest()==entry['sha256']
 assert src.stat().st_size==entry['size']
# Copy every persistent object including any unlinked evidence. Never copy writer.lock or transient jobs.
allfiles=[]
for src in sorted(objects.rglob('*')):
 if src.is_symlink():raise SystemExit('Enlace inesperado')
 if not src.is_file():continue
 rel=src.relative_to(objects);dst=dest/rel;dst.parent.mkdir(parents=True,exist_ok=True)
 with src.open('rb') as f, dst.open('xb') as g:
  while chunk:=f.read(65536):g.write(chunk)
 with src.open('rb') as f:sha=hashlib.file_digest(f,'sha256').hexdigest()
 with dst.open('rb') as f:assert hashlib.file_digest(f,'sha256').hexdigest()==sha
 allfiles.append({'path':rel.as_posix(),'size':src.stat().st_size,'sha256':sha})
history=query("SELECT JSON_OBJECT('version',version,'description',description,'script',script,'checksum',checksum,'success',success) FROM robust_inst_formulario_lab.flyway_schema_history ORDER BY installed_rank;")
(out/'manifest-documentos.json').write_text(json.dumps({'referenced':files,'allObjects':allfiles,'pendingTemps':len(list((base/'archivo/tmp').iterdir()))},indent=2))
(out/'flyway-origen.json').write_text(json.dumps(history,indent=2))
(out/'respaldo-origen.json').write_text(json.dumps({'dumpBytes':dump.stat().st_size,'dumpSha256':hashlib.sha256(dump.read_bytes()).hexdigest(),'objects':len(allfiles),'referenced':len(files),'sourceVersion':'8.4','scope':'robust_inst_formulario_lab','method':'single-transaction logical data snapshot; no physical copy'},indent=2))
print(json.dumps({'backup':True,'objects':len(allfiles),'referenced':len(files),'bytes':sum(x['size'] for x in allfiles),'migrations':len(history)}))