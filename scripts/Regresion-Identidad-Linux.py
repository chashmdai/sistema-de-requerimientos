#!/usr/bin/env python3
"""Regresión funcional acotada. Valida los nombres excluidos antes de invocar Maven; no ejecuta carga."""
import argparse,json,os,re,subprocess
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__);p.add_argument('private',type=Path);p.add_argument('--mysql',action='store_true');p.add_argument('--fixtures',type=Path,required=True);a=p.parse_args();root=Path(__file__).resolve().parents[1];private=a.private.resolve()
props=dict(l.split('=',1) for l in (private/'application.properties').read_text().splitlines() if '=' in l and not l.startswith('#'))
if not(props.get('server.address')=='127.0.0.1' and props.get('server.port')=='20443' and props.get('institucional.nucleo.entorno')=='LABORATORIO'):p.error('Solo laboratorio institucional local')
excluded=['vectorInteroperableYDuplicadosConcurrentesTienenUnaConfirmacionEstable','doscientosFoliosDistintosBajoConcurrenciaYFormatoHistoricoEstable','consumidorParaleloAcotadoConservaUnicidadYConfirmaCadaRegistro']
source=(root/'siger-institucional/backend/src/test/java/cl/nexbyte/siger/institucional/robustez/RecepcionInstitucionalJdbcTest.java').read_text()
if any(not re.search(r'void\s+'+re.escape(name)+r'\s*\(',source) for name in excluded):p.error('El filtro no coincide con los métodos reales; no se ejecuta la batería')
env=os.environ.copy();env.update(DOCUMENTAL_REAL_CONFIG=str(private/'application.properties'),DOCUMENTAL_REAL_FIXTURES=str(a.fixtures.resolve()))
for key in list(env):
 if key.startswith('ROBUST_DB_'):del env[key]
if a.mysql:
 entry=json.loads((private/'db-secrets.json').read_text())['robust_inst_test']
 env.update(ROBUST_DB_URL='jdbc:mysql://127.0.0.1:13306/robust_inst_test?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=5000',ROBUST_DB_USER=entry['user'],ROBUST_DB_PASSWORD=entry['password'],ROBUST_DB_MIGRATION_USER=entry['migration_user'],ROBUST_DB_MIGRATION_PASSWORD=entry['migration_password'])
log=root/('.test-identidad-mysql.log' if a.mysql else '.test-identidad-h2.log')
with log.open('w') as f:
 result=subprocess.run(['mvn','-B','-ntp','-Dtest=*,!RecepcionInstitucionalJdbcTest#'+'+'.join(excluded),'test'],cwd=root/'siger-institucional/backend',env=env,stdout=f,stderr=subprocess.STDOUT)
summaries=re.findall(r'\[INFO\] Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)\s*$',log.read_text(),re.M)
print('Motor', 'MySQL' if a.mysql else 'H2','salida',result.returncode,'resumen',summaries[-1] if summaries else 'Ver diagnóstico privado')
raise SystemExit(result.returncode)
