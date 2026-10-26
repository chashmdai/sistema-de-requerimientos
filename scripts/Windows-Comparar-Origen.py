import subprocess,json,hashlib
from pathlib import Path
out=Path('/mnt/c/req/evidencias');snapshot=json.loads((out/'sql-snapshot-origen.json').read_text());columns={}
for col in snapshot['columns']:columns.setdefault(col['table'],[]).append(col['column'])
queries=['START TRANSACTION WITH CONSISTENT SNAPSHOT;']
for table,cols in columns.items():
 expression=','.join('IF(`'+c+'` IS NULL,\'N\',CONCAT(\'V\',HEX(CAST(`'+c+'` AS BINARY))))' for c in cols)
 queries.append("SELECT JSON_OBJECT('table','"+table+"','sha256',SHA2(CONCAT_WS('|',"+expression+"),256)) FROM robust_inst_formulario_lab.`"+table+"`;")
queries.append('COMMIT;')
cmd=['docker','exec','-i','siger-mysql-lab-mysql-1','mysql','--defaults-extra-file=/run/secrets/root.cnf','--default-character-set=utf8mb4','--batch','--raw','--skip-column-names']
r=subprocess.run(cmd,input='\n'.join(queries),capture_output=True,text=True)
if r.returncode:raise SystemExit('Comparacion no completada')
rows={table:[] for table in columns}
for line in r.stdout.splitlines():
 value=json.loads(line);rows[value['table']].append(value['sha256'])
for table,hashes in rows.items():
 actual={'count':len(hashes),'sha256':hashlib.sha256(''.join(x+'\n' for x in sorted(hashes)).encode('ascii')).hexdigest()}
 if actual!=snapshot['tables'][table]:raise SystemExit('Origen cambio despues del snapshot: no ejecutar cutover')
q="SET SESSION information_schema_stats_expiry=0; SELECT JSON_OBJECT('table',table_name,'next',auto_increment) FROM information_schema.tables WHERE table_schema='robust_inst_formulario_lab' AND auto_increment IS NOT NULL ORDER BY table_name;"
r=subprocess.run(cmd,input=q,capture_output=True,text=True)
if r.returncode:raise SystemExit('Secuencias no comprobadas')
seq=[json.loads(x) for x in r.stdout.splitlines()];target=json.loads((out/'auditoria-sql-windows.json').read_text())['autoIncrement']
assert all(target[s['table']]==s['next'] for s in seq)
result={'sourceStillMatchesSnapshot':True,'sourceTables':len(rows),'sourceRows':sum(len(x) for x in rows.values()),'sourceSequenceCacheBypassed':True,'matchingAutoIncrements':len(seq),'sourceMutatedByMigration':False}
(out/'comparacion-origen-final.json').write_text(json.dumps(result,indent=2));print(json.dumps(result))