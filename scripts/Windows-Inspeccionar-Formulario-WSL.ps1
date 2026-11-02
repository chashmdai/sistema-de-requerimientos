[CmdletBinding()]
param([Parameter(Mandatory)][string]$Evidencia,[string]$RuntimeRoot='/home/nexbyteproveedor/.local/share/siger/nna-local-20261023')
$ErrorActionPreference='Stop'
if(-not $IsWindows -or -not ([IO.Path]::GetFullPath($Evidencia)).StartsWith('C:\req\evidencias\')){throw 'Solo evidencia Windows de laboratorio'}
if($RuntimeRoot -notmatch '^/home/nexbyteproveedor/\.local/share/siger/(nna-local-20261023|e2e-siger-[a-f0-9]{32})$'){throw 'Solo runtime publico original o QA aislado acordado'}
# Auditoría puntual de solo lectura. No es una dependencia del runtime SIGER.
$running=((& wsl.exe --list --running --quiet) -join "`n").Replace("`0",'')
if($LASTEXITCODE -ne 0 -or @($running -split "`r?`n"|ForEach-Object {$_.Trim()}) -notcontains 'Ubuntu-24.04'){throw 'WSL Ubuntu-24.04 no esta ejecutandose; la inspeccion no inicia la distribucion automaticamente'}
$probe=@'
import pathlib,json,hashlib,zipfile,subprocess,os,datetime,sys
r=pathlib.Path(sys.argv[1])
repo=pathlib.Path('/home/nexbyteproveedor/proyectos/formulario-web')
active=json.loads((r/'active-runtime.json').read_text());selected=json.loads((r/'selected-jar.json').read_text())
pid=int(active['pid']);args=pathlib.Path(f'/proc/{pid}/cmdline').read_bytes().split(b'\0')
jar=pathlib.Path(args[args.index(b'-jar')+1].decode())
def digest(p):
 h=hashlib.sha256()
 with open(p,'rb') as f:
  for b in iter(lambda:f.read(1048576),b''):h.update(b)
 return h.hexdigest()
sha=digest(jar);config=pathlib.Path(active['config'])
flags={}
allowed={'server.address','server.port','server.ssl.enabled','formulario.habilitado','formulario.recepcion.habilitada','formulario.adultos.habilitado','ciclo-vida.online-habilitado','ciclo-vida.eliminacion.habilitada'}
for line in config.read_text().splitlines():
 k,sep,v=line.partition('=')
 if sep and k in allowed:flags[k]=v.strip()
env=dict(os.environ,GIT_OPTIONAL_LOCKS='0')
def git(*args):return subprocess.check_output(['git','-c',f'safe.directory={repo}','-C',str(repo),*args],env=env,text=True).strip()
with zipfile.ZipFile(jar) as z:
 names=z.namelist();catalog=next(n for n in names if n.endswith('/paises.json'))
 catalog_bytes=z.read(catalog);source_catalog=(repo/'formulario-publico/front/src/formulario/paises.json').read_bytes()
 features={'foreignIdentification':b'DOCUMENTO_EXTRANJERO' in z.read('BOOT-INF/classes/cl/nexbyte/siger/formulario/IdentificacionCiudadana.class'),'nnaGroups':b'grupo-nna-' in z.read('BOOT-INF/classes/cl/nexbyte/siger/formulario/ValidacionAdultos.class'),'catalogMatchesSource':catalog_bytes==source_catalog,'countries':len(json.loads(catalog_bytes)['paises'])}
 embedded_git=any(n.endswith('/git.properties') for n in names)
inodes=set()
for table in ['tcp','tcp6']:
 for line in pathlib.Path('/proc/net/'+table).read_text().splitlines()[1:]:
  row=line.split()
  if row[3]=='0A' and int(row[1].split(':')[1],16)==int(active['port']):inodes.add('socket:['+row[9]+']')
listening=False
for fd in pathlib.Path(f'/proc/{pid}/fd').iterdir():
 try:listening=listening or os.readlink(fd) in inodes
 except OSError:pass
ticks=pathlib.Path(f'/proc/{pid}/stat').read_text().split(') ',1)[1].split()[19]
result={'scope':'READ_ONLY_PUBLIC_RUNTIME','checkedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'repositoryHead':git('rev-parse','HEAD'),'repositoryClean':not bool(git('status','--porcelain')),'pid':pid,'port':active['port'],'processListensOnRecordedPort':listening,'startTicksMatch':str(active['start_ticks'])==ticks,'jar':str(jar),'jarSHA256':sha,'selectedArtifactMatches':selected['jar']==str(jar) and selected['sha256']==sha,'runtimeArtifactMatches':active['jar']==str(jar) and active['sha256']==sha,'configUnchanged':digest(config)==active['config_sha256'],'features':features,'embeddedGitMetadata':embedded_git,'flags':flags,'writes':0}
print(json.dumps(result,ensure_ascii=False))
if not all([listening,result['startTicksMatch'],result['selectedArtifactMatches'],result['runtimeArtifactMatches'],result['configUnchanged'],result['repositoryClean'],*list(features.values())]):raise SystemExit(2)
'@
$raw=@(& wsl.exe -d Ubuntu-24.04 -u root --exec python3 -B -c $probe $RuntimeRoot 2>$null)
if($LASTEXITCODE -ne 0){throw 'Proceso publico no acreditado; no se modifica WSL'}
$result=($raw -join "`n")|ConvertFrom-Json
$result|ConvertTo-Json -Depth 6|Set-Content -LiteralPath (Join-Path $Evidencia 'artefacto-publico-activo.json')
$capability=Invoke-WebRequest -Uri 'https://formulario.defensorianinez.gob.cl/api/formulario/recepcion/capacidades' -MaximumRedirection 0 -TimeoutSec 10
@{origin='https://formulario.defensorianinez.gob.cl';addresses=@([Net.Dns]::GetHostAddresses('formulario.defensorianinez.gob.cl')|ForEach-Object {$_.ToString()});tlsValidation='WINDOWS_DEFAULT_NO_BYPASS';status=[int]$capability.StatusCode;capabilities=($capability.Content|ConvertFrom-Json);readOnly=$true}|ConvertTo-Json -Depth 4|Set-Content -LiteralPath (Join-Path $Evidencia 'https-capacidades.json')
@{repositoryHead=$result.repositoryHead;artifactSHA256=$result.jarSHA256;pid=$result.pid;port=$result.port;verified=$true;claims=0;acksSent=0}|ConvertTo-Json -Compress
