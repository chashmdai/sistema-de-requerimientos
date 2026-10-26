#!/usr/bin/env python3
"""Snapshot lógico consistente de solo lectura del laboratorio WSL. Nunca detiene ni cambia el origen."""
import hashlib,json,subprocess,sys
from pathlib import Path
SCHEMA='robust_inst_formulario_lab'
DEST=Path('/mnt/c/req/config')
CMD=['docker','exec','-i','siger-mysql-lab-mysql-1','mysql','--defaults-extra-file=/run/secrets/root.cnf','--default-character-set=utf8mb4','--batch','--raw','--skip-column-names']
def query(sql):
 r=subprocess.run(CMD,input=sql.encode(),capture_output=True)
 if r.returncode:raise SystemExit('Lectura de snapshot no completada; diagnóstico remoto omitido')
 return r.stdout
if not Path('/proc/sys/kernel/osrelease').is_file() or not DEST.is_dir():raise SystemExit('Solo origen WSL y destino autorizado')
file=DEST/'origen-u4-snapshot.jsonl'
if file.exists() or file.with_suffix('.jsonl.dpapi').exists():raise SystemExit('Snapshot existente: no sobrescribir')
metadata=[json.loads(x) for x in query("SELECT JSON_OBJECT('table',table_name,'column',column_name,'ordinal',ordinal_position,'type',column_type) FROM information_schema.columns WHERE table_schema='"+SCHEMA+"' AND table_name IN(SELECT table_name FROM information_schema.tables WHERE table_schema='"+SCHEMA+"' AND table_type='BASE TABLE') ORDER BY table_name,ordinal_position;").splitlines()]
tables={}
for col in metadata:tables.setdefault(col['table'],[]).append(col['column'])
script=['SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;','START TRANSACTION WITH CONSISTENT SNAPSHOT;']
for table,cols in tables.items():
 values=','.join('IF(`'+col+'` IS NULL,NULL,HEX(CAST(`'+col+'` AS BINARY)))' for col in cols)
 script.append("SELECT JSON_OBJECT('table','"+table+"','values',JSON_ARRAY("+values+")) FROM `"+SCHEMA+"`.`"+table+"`;")
script.append('COMMIT;')
raw=query('\n'.join(script));file.write_bytes(raw)
rows={table:[] for table in tables}
for line in raw.splitlines():
 row=json.loads(line);hexes=row['values'];encoded='|'.join('N' if value is None else 'V'+value for value in hexes).encode('ascii');rows[row['table']].append(hashlib.sha256(encoded).hexdigest())
manifest={t:{'count':len(v),'sha256':hashlib.sha256(''.join(x+'\n' for x in sorted(v)).encode('ascii')).hexdigest()} for t,v in rows.items()}
Path('/mnt/c/req/evidencias/sql-snapshot-origen.json').write_text(json.dumps({'tables':manifest,'columns':metadata,'snapshotBytes':len(raw),'snapshotSha256':hashlib.sha256(raw).hexdigest(),'isolation':'REPEATABLE READ / WITH CONSISTENT SNAPSHOT','flywayHistoryIncluded':True},indent=2))
print(json.dumps({'snapshot':True,'tables':len(tables),'rows':sum(len(v) for v in rows.values()),'bytes':len(raw)}))