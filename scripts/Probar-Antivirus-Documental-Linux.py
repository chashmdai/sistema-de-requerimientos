from pathlib import Path
import urllib.request,urllib.parse,urllib.error,http.cookiejar,ssl,json,uuid
import argparse
parser=argparse.ArgumentParser(description='Control antivirus HTTPS sintético desde WSL, sin alterar protecciones de Windows');parser.add_argument('private',type=Path);args=parser.parse_args();p=args.private.resolve();r=Path(__file__).resolve().parents[1]
props=dict(l.split('=',1) for l in (p/'application.properties').read_text().splitlines() if '=' in l and not l.startswith('#'));state=json.loads((r/'.local/evidencias/documental/estado.json').read_text());origin='https://claveunica.formularioadulto.defensorianinez.gob.cl:20443'
assert props['server.port']=='20443' and props['institucional.nucleo.entorno']=='LABORATORIO'
client=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()),urllib.request.HTTPSHandler(context=ssl.create_default_context(cafile=str(p/'rootCA.pem'))))
def get(path):
 with client.open(origin+path,timeout=10) as response:return json.load(response)
try:
 csrf=get('/nucleo/csrf');body=urllib.parse.urlencode({'username':'demo.profesional.upr.a','password':props['institucional.nucleo.password-laboratorio']}).encode()
 with client.open(urllib.request.Request(origin+'/nucleo/login',data=body,headers={csrf['headerName']:csrf['token'],'Content-Type':'application/x-www-form-urlencoded'}),timeout=10) as response:assert response.status==204
 csrf=get('/nucleo/csrf');boundary='siger'+uuid.uuid4().hex
 original=(r/'.local/fixtures-documental/control-antivirus.txt').read_bytes()
 content=('--'+boundary+'\r\nContent-Disposition: form-data; name="archivo"; filename="control-antivirus.txt"\r\nContent-Type: application/octet-stream\r\n\r\n').encode()+original+('\r\n--'+boundary+'--\r\n').encode()
 request=urllib.request.Request(origin+'/nucleo/casos/'+state['caso1']+'/documentos-gestion',data=content,headers={csrf['headerName']:csrf['token'],'Content-Type':'multipart/form-data; boundary='+boundary,'Idempotency-Key':str(uuid.uuid4())})
 try:
  with client.open(request,timeout=30) as response:status=response.status
 except urllib.error.HTTPError as error:status=error.code;problem=json.load(error);assert problem.get('code')=='DOCUMENTO_RECHAZADO'
 assert status==422
 (r/'.local/evidencias/documental/antivirus-http-real.json').write_text(json.dumps({'cliente':'WSL','https':True,'antivirusReal':True,'status':422,'receptor':'institucional','simulado':False},indent=2));print('Control antivirus real por HTTPS desde WSL: rechazado 422.')
except Exception:
 print('Control antivirus HTTP no completado; no se imprimen cabeceras ni credenciales.');raise SystemExit(1)