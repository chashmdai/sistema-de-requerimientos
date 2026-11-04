#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr',[string]$JavaHome='',[string]$KeyId='smr-windows-20261104-v1')
. "$PSScriptRoot\Common.ps1"
if($KeyId -notmatch '^[A-Za-z0-9_-]{1,64}$'){throw 'KeyId inválido'}
if(-not(Test-Path -LiteralPath $Root)){[IO.Directory]::CreateDirectory($Root)|Out-Null;Set-SmrPrivate $Root};Assert-SmrPrivate $Root
foreach($name in @('secrets','public','releases','logs','backups','recovery','evidence','config','instances')){[IO.Directory]::CreateDirectory((Join-Path $Root $name))|Out-Null}
$secret=Join-Path $Root 'secrets\ack-identity.dpapi';$pub=Join-Path $Root 'public\ack'
if(Test-Path -LiteralPath $secret){Write-Output 'Identidad ACK ya existe; no regenerada';return}
if(-not $JavaHome){$JavaHome=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../runtime/java25'))};if(-not(Test-Path "$JavaHome\bin\java.exe")){throw 'Java25 portable explícito requerido'}
$keys=(Invoke-SmrJava (Join-Path $JavaHome 'bin\java.exe') @('-XX:-UsePerfData',"$PSScriptRoot\IdentitySmr.java") '' ) -split '\r?\n' | Where-Object {$_}
if($keys.Count -ne 2){throw 'Generación ACK no acreditada'}
$private="-----BEGIN PRIVATE KEY-----`n$($keys[0])`n-----END PRIVATE KEY-----`n";$public="-----BEGIN PUBLIC KEY-----`n$($keys[1])`n-----END PUBLIC KEY-----`n"
$identity=@{keyId=$KeyId;privatePem=$private;publicPem=$public;createdUtc=[DateTime]::UtcNow.ToString('o');ownerSid=[Security.Principal.WindowsIdentity]::GetCurrent().User.Value}
Protect-SmrJson $identity $secret
# Recuperación cifrada independiente del formato DPAPI; la clave de recuperación se custodia aparte.
$recoveryKey=[Security.Cryptography.RandomNumberGenerator]::GetBytes(32);$nonce=[Security.Cryptography.RandomNumberGenerator]::GetBytes(12)
$plain=[Text.Encoding]::UTF8.GetBytes(($identity|ConvertTo-Json -Compress));$cipher=[byte[]]::new($plain.Length);$tag=[byte[]]::new(16)
$aes=[Security.Cryptography.AesGcm]::new($recoveryKey,16)
try{$aes.Encrypt($nonce,$plain,$cipher,$tag);@{version=1;algorithm='AES-256-GCM';nonce=[Convert]::ToBase64String($nonce);tag=[Convert]::ToBase64String($tag);ciphertext=[Convert]::ToBase64String($cipher)}|ConvertTo-Json|Set-Content -LiteralPath (Join-Path $Root 'backups\ack-recovery.enc.json');Protect-SmrJson @{key=[Convert]::ToBase64String($recoveryKey)} (Join-Path $Root 'recovery\ack-recovery-key.dpapi')}finally{$aes.Dispose();[Array]::Clear($plain);[Array]::Clear($recoveryKey)}
[IO.Directory]::CreateDirectory($pub)|Out-Null
[IO.File]::WriteAllText((Join-Path $pub 'ack-public.pem'),$public,[Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText((Join-Path $pub 'ack-trust.json'),(@{$KeyId=$public}|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
$manifest=@{keyId=$KeyId;algorithm='Ed25519';format='SPKI PEM';publicPemSha256=(Get-FileHash "$pub\ack-public.pem").Hash.ToLowerInvariant();trustJsonSha256=(Get-FileHash "$pub\ack-trust.json").Hash.ToLowerInvariant();identityCreated=$true;productionPullAuthorized=$false;formalInstitutionalApproval='NO_ACREDITADA';privateMaterialIncluded=$false}
$manifest|ConvertTo-Json|Set-Content -LiteralPath "$pub\manifest.json"
Compress-Archive -Path "$pub\*" -DestinationPath (Join-Path $Root 'public\smr-ack-publico-20261104.zip')
$zipHash=(Get-FileHash (Join-Path $Root 'public\smr-ack-publico-20261104.zip')).Hash.ToLowerInvariant()
@"
# Para Codex Ubuntu · identidad SMR Windows

Identidad creada y persistente, privada protegida por DPAPI CurrentUser y ACL. No hay autorización de PULL acreditada todavía.

- ID ACK: $KeyId
- Clave: Ed25519, PEM SPKI en ack/ack-public.pem.
- SHA-256 PEM: $($manifest.publicPemSha256)
- Mapa real instalable: ack/ack-trust.json
- SHA-256 JSON: $($manifest.trustJsonSha256)
- Paquete: smr-ack-publico-20261104.zip
- SHA-256 paquete: $zipHash
- Origen previsto: https://formulario.defensorianinez.gob.cl (sin path).
- Ruta contractual: /integracion/v3/formulario-completo/entregas; lote 1; versiones 1.0,1.1.
- Consumidor público: desactivado; Bearer existente pendiente de canal privado.
- Sonda Java HEAD 401/401 sin credencial válida: pendiente de ejecutar; se actualizará en SONDA_PUBLICA.json.

Instalar este mapa únicamente en ventana autorizada, verificando la huella por canal independiente. La herramienta Ubuntu reemplaza el mapa: preservar verificadoras históricas necesarias. No ejecutar scripts Ubuntu en Windows.

Para continuar faltan Bearer privado, instalación de confianza ACK y confirmación concreta de custodia en este PC, alcance de toda la cola elegible, receptor único, respaldo y apertura PULL. No reclamar como sonda. El operador realiza el envío autorizado.
"@|Set-Content -LiteralPath (Join-Path $Root 'public\PARA_CODEX_UBUNTU.md')
$identity.Clear();$private=$null;$keys=$null;$plain=$null
Write-Output "Identidad ACK creada. Paquete público: $Root\public\smr-ack-publico-20261104.zip"
Write-Output "SHA-256 paquete: $zipHash"
