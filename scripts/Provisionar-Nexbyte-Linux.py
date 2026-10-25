#!/usr/bin/env python3
"""Provisiona/reutiliza nexbyte por las API TI existentes, sin mostrar ni reemplazar contraseñas."""
import argparse,json,os,secrets,ssl,urllib.request,urllib.error,urllib.parse,http.cookiejar,datetime
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__);p.add_argument('private',type=Path);a=p.parse_args();os.umask(0o077)
root=a.private.resolve()
if not root.is_relative_to(Path.home()/'.local/share/siger') or any((d/'.git').exists() for d in [root,*root.parents]):p.error('Directorio privado fuera de Git requerido')
props=dict(l.split('=',1) for l in (root/'application.properties').read_text().splitlines() if '=' in l and not l.startswith('#'))
if not(props.get('server.address')=='127.0.0.1' and props.get('server.port')=='20443' and props.get('institucional.identidad.habilitada')=='true' and props.get('institucional.nucleo.entorno')=='LABORATORIO'):p.error('Solo laboratorio individual 20443')
origin='https://claveunica.formularioadulto.defensorianinez.gob.cl:20443'
tls=ssl.create_default_context(cafile=props['institucional.formulario-completo.ca-certificado']);opener=urllib.request.build_opener(urllib.request.HTTPSHandler(context=tls),urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
def call(route,body=None,method='GET',form=False,csrf=None):
 data=None;headers={}
 if body is not None:
  if form:
   data=urllib.parse.urlencode(body).encode();headers['Content-Type']='application/x-www-form-urlencoded'
  else:data=json.dumps(body).encode();headers['Content-Type']='application/json'
 if csrf:headers[csrf['headerName']]=csrf['token']
 try:
  with opener.open(urllib.request.Request(origin+route,data=data,headers=headers,method=method),timeout=15) as r:
   raw=r.read(512000);return json.loads(raw) if raw else None
 except urllib.error.HTTPError as e:raise RuntimeError('HTTP_'+str(e.code)) from None
 except Exception:raise RuntimeError('HTTP_NO_CONFIRMADO') from None
try:
 ti=json.loads((root/'identidad-bootstrap.json.aplicado').read_text());c=call('/nucleo/csrf');call('/nucleo/login',{'username':ti['usuario'],'password':ti['password']},'POST',True,c)
 accounts=call('/nucleo/ti/funcionarios');account=next((u for u in accounts if u['usuario']=='nexbyte'),None)
 file=root/'nexbyte-cuenta.json'
 if file.is_symlink():raise RuntimeError('CUSTODIA_INVALIDA')
 if account is None:
  if file.exists():creds=json.loads(file.read_text())
  else:
   creds={'usuario':'nexbyte','password':secrets.token_urlsafe(32)}
   fd=os.open(file,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
   with os.fdopen(fd,'w') as f:json.dump(creds,f);f.flush();os.fsync(f.fileno())
  if creds['usuario']!='nexbyte':raise RuntimeError('CUSTODIA_NO_COINCIDE')
  account=call('/nucleo/ti/funcionarios',{'usuario':'nexbyte','nombre':'Nexbyte Proveedor','correo':'nexbyte.proveedor@defensorianinez.cl','cargo':'Proveedor de laboratorio','unidad':'UPR','sede':'DEMO_SEDE_A','password':creds['password']},'POST',csrf=call('/nucleo/csrf'))
 else:
  if not file.exists():raise RuntimeError('CUENTA_EXISTENTE_SIN_CREDENCIAL_PRIVADA_NO_SE_RESETEA')
  creds=json.loads(file.read_text())
 if file.stat().st_mode&0o777!=0o600:raise RuntimeError('PERMISOS_PRIVADOS_INVALIDOS')
 if account['correo']!='nexbyte.proveedor@defensorianinez.cl' or account['nombre']!='Nexbyte Proveedor' or not account['activo']:
  account=call('/nucleo/ti/funcionarios/'+account['id'],{'nombre':'Nexbyte Proveedor','correo':'nexbyte.proveedor@defensorianinez.cl','cargo':account['cargo'],'unidad':account['unidad'],'sede':account['sede'],'activo':True,'revision':account['revision']},'PUT',csrf=call('/nucleo/csrf'))
 specs=[('TI','INSTITUCION',None,None),('DISTRIBUCION','INSTITUCION',None,None),('DIRECTOR','UNIDAD_SEDE','UPR','DEMO_SEDE_A'),('COORDINADOR','UNIDAD_SEDE','UPD','DEMO_SEDE_A'),('COORDINADOR_REGIONAL','SEDE',None,'DEMO_SEDE_A'),('PROFESIONAL','UNIDAD_SEDE','UPR','DEMO_SEDE_A'),('ADMINISTRATIVO','UNIDAD_SEDE','UGI','DEMO_SEDE_B')]
 now=datetime.datetime.now(datetime.timezone.utc)
 for role,scope,unit,site in specs:
  current=next(x for x in call('/nucleo/ti/funcionarios') if x['id']==account['id'])
  if not any(g['role_code']==role and g['scope_code']==scope and g['unit_code']==unit and g['site_id']==site and g['revoked_ms'] is None and g['valid_from']<=int(now.timestamp()*1000) and (g['valid_until'] is None or g['valid_until']>int(now.timestamp()*1000)) for g in current['asignaciones']):
   call('/nucleo/ti/funcionarios/'+account['id']+'/roles',{'rol':role,'alcance':scope,'unidad':unit,'sede':site,'desde':(now-datetime.timedelta(seconds=2)).isoformat(),'hasta':None},'POST',csrf=call('/nucleo/csrf'))
 call('/nucleo/logout',method='POST',csrf=call('/nucleo/csrf'));call('/nucleo/login',{'username':'nexbyte','password':creds['password']},'POST',True,call('/nucleo/csrf'));session=call('/nucleo/session');assert session['operador']['id']==account['id'] and len({x['rol'] for x in session['operador']['accesos']})==7
 print('Cuenta nexbyte única, activa y autenticada; siete roles con ámbitos explícitos. Credencial conservada exclusivamente en custodia privada.')
except Exception as e:
 print('Provisión no completada; diagnóstico sanitizado:',str(e) if isinstance(e,RuntimeError) and str(e).isupper() else type(e).__name__);raise SystemExit(1)
