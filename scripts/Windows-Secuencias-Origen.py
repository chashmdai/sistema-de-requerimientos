#!/usr/bin/env python3
"""Secuencias del snapshot WSL, sin cache de information_schema y sin cambiar el servidor."""
import subprocess,json
from pathlib import Path
q="SET SESSION information_schema_stats_expiry=0; SELECT JSON_OBJECT('table',table_name,'next',auto_increment) FROM information_schema.tables WHERE table_schema='robust_inst_formulario_lab' AND auto_increment IS NOT NULL ORDER BY table_name;"
r=subprocess.run(['docker','exec','-i','siger-mysql-lab-mysql-1','mysql','--defaults-extra-file=/run/secrets/root.cnf','--batch','--raw','--skip-column-names'],input=q,text=True,capture_output=True)
if r.returncode:raise SystemExit('Inventario de secuencias no completado')
seq=[json.loads(x) for x in r.stdout.splitlines()];file=Path('/mnt/c/req/evidencias/auto-increment-origen.json')
if file.exists():
 if json.loads(file.read_text())!=seq:raise SystemExit('Origen cambio: no sobrescribir el corte existente')
else:file.write_text(json.dumps(seq,indent=2))
print(json.dumps({'sequences':len(seq),'sourceMutated':False}))