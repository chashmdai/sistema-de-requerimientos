#!/usr/bin/env python3
"""Verificación de solo lectura de la custodia integrada y del estado sintético de esta unidad."""
import argparse,hashlib,json,subprocess
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__);p.add_argument('private',type=Path);p.add_argument('--evidencias',type=Path,required=True);a=p.parse_args()
private=a.private.resolve();e=a.evidencias.resolve()
props=dict(l.split('=',1) for l in (private/'application.properties').read_text().splitlines() if '=' in l and not l.startswith('#'))
if not(props.get('server.address')=='127.0.0.1' and props.get('server.port')=='20443' and '/robust_inst_formulario_lab?' in props.get('institucional.db.url','')):p.error('Solo esquema institucional integrado de laboratorio')
def query(sql):
 r=subprocess.run(['docker','exec','-i','siger-mysql-lab-mysql-1','mysql','--defaults-extra-file=/run/secrets/root.cnf','--default-character-set=utf8mb4','-N','--raw'],input=sql,capture_output=True,text=True)
 if r.returncode:raise SystemExit('Consulta de solo lectura no completada; no se imprimen diagnósticos privados')
 return [json.loads(x) for x in r.stdout.splitlines()]
baseline=json.loads((e.parent/'documental/baseline-custodia.json').read_text())
current=query("SELECT JSON_OBJECT('id',i.id,'immutable',SHA2(CONCAT(i.body,i.receipt,c.comunicacion_id,c.numero,c.registrada_ms,c.canal,c.content_hash,c.provenance_hash),256),'ack_pending',i.ack_pending) FROM robust_inst_formulario_lab.institutional_inbox i JOIN robust_inst_formulario_lab.communication_registry c ON c.source=i.source AND c.operacion_id=i.id ORDER BY i.id;")
assert baseline==current and all(not x['ack_pending'] for x in current)
docs=query("SELECT JSON_OBJECT('origin',origin,'key',storage_key,'size',size_bytes,'sha256',sha256,'author',created_by) FROM robust_inst_formulario_lab.institution_document;")
pages=query("SELECT JSON_OBJECT('key',storage_key,'size',size_bytes,'sha256',sha256) FROM robust_inst_formulario_lab.document_page;")
for d in docs+pages:
 f=private/'archivo/objects'/(d['key']+'.bin');assert f.resolve().is_relative_to(private/'archivo/objects') and not f.is_symlink()
 assert f.stat().st_size==d['size']
 with f.open('rb') as source:assert hashlib.file_digest(source,'sha256').hexdigest()==d['sha256']
for d in docs:assert (d['author'] is None)==(d['origin']=='INGRESO_CIUDADANO')
counts=query("SELECT JSON_OBJECT('migraciones',(SELECT COUNT(*) FROM robust_inst_formulario_lab.flyway_schema_history WHERE success=1),'fallosMigracion',(SELECT COUNT(*) FROM robust_inst_formulario_lab.flyway_schema_history WHERE success=0),'comunicaciones',(SELECT COUNT(*) FROM robust_inst_formulario_lab.communication_registry),'foliosUnicos',(SELECT COUNT(DISTINCT numero) FROM robust_inst_formulario_lab.communication_registry),'casos',(SELECT COUNT(*) FROM robust_inst_formulario_lab.core_case),'requerimientos',(SELECT COUNT(*) FROM robust_inst_formulario_lab.core_requirement),'documentosGestion',(SELECT COUNT(*) FROM robust_inst_formulario_lab.institution_document WHERE origin='GESTION_INSTITUCIONAL'),'vinculosGestion',(SELECT COUNT(*) FROM robust_inst_formulario_lab.management_document_link),'comandosDocumentales',(SELECT COUNT(*) FROM robust_inst_formulario_lab.document_command),'cuentas',(SELECT COUNT(*) FROM robust_inst_formulario_lab.staff_account),'activas',(SELECT COUNT(*) FROM robust_inst_formulario_lab.staff_account WHERE enabled=TRUE),'hashesIndividuales',(SELECT COUNT(DISTINCT password_hash) FROM robust_inst_formulario_lab.staff_account),'distribucionesNuevas',(SELECT COUNT(*) FROM robust_inst_formulario_lab.distribution_event),'coberturasHistoricas',(SELECT COUNT(*) FROM robust_inst_formulario_lab.org_coverage),'webConSedeInventada',(SELECT COUNT(*) FROM robust_inst_formulario_lab.communication_origin WHERE origin_code='WEB' AND site_id IS NOT NULL));")[0]
assert counts['migraciones']==11 and counts['fallosMigracion']==0
assert counts['documentosGestion']==2 and counts['vinculosGestion']==3 and counts['comandosDocumentales']==3
assert counts['cuentas']==counts['activas']==counts['hashesIndividuales']==8
assert counts['comunicaciones']==counts['foliosUnicos']==9 and counts['casos']==3 and counts['requerimientos']==2
assert counts['distribucionesNuevas']==2 and counts['coberturasHistoricas']==3 and counts['webConSedeInventada']==0
assert len(list((private/'archivo/objects/gestion').glob('*.bin')))==2
assert not [f for f in (private/'archivo/tmp').iterdir() if f.is_file()]
result={'comunicacionesPrevias':len(current),'custodiaComFirmasAckSinCambios':True,'originalesCiudadanos':sum(d['origin']=='INGRESO_CIUDADANO' for d in docs),'originalesGestion':2,'paginasDerivadas':len(pages),'integridadOriginalesYDerivadas':True,'temporalesAbiertos':0,**counts}
e.mkdir(parents=True,exist_ok=True);(e/'custodia-verificada.json').write_text(json.dumps(result,indent=2));print(json.dumps(result))
