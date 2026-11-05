# SMR RC1 · recepción técnica Windows

SMR reutiliza SIGER para recibir el formulario completo, conservar documento y originales, asignar COM/folio, confirmar mediante ACK y consultar la custodia con sesión individual. No habilita Casos, distribución, derivaciones, solicitudes, Graph, CU ni PISEE. La cuenta inicial es **sintética y local**, conforme al alcance solicitado; no representa un funcionario o una designación institucional.

## Instalación de desarrollo

| Recurso | Sintético | Recepción pública preparada |
|---|---|---|
| UI HTTPS loopback | https://localhost:18443/ | https://localhost:18444/ |
| Esquema | smr_sintetico | smr_recepcion |
| Datos/configuración | C:\req\smr\instances\sintetico | C:\req\smr\instances\recepcion |
| Origen | https://localhost:19443 | https://formulario.defensorianinez.gob.cl |
| Estado de entrega | Cinco registros recibidos por PULL local | Sin PULL público; transporte desactivado |

Motor nuevo MySQL 8.0.46 en `127.0.0.1:13316`, con datos/configuración/TLS propios bajo `C:\req\smr\mysql`. Reutiliza el ejecutable ya instalado, sin modificar MySQL80 ni su configuración. App: SELECT/INSERT/UPDATE/DELETE; migrador separado; respaldo: SELECT/SHOW VIEW. Ninguna identidad de runtime usa root. El administrador DPAPI pertenece exclusivamente al motor nuevo.

El código vive en `C:\req\worktrees\smr`, rama `release/smr-recepcion-2026-11`. La instalación utiliza artefactos inmutables bajo `C:\req\smr\releases`, sin ejecutar desde Git, target o front/dist. `instance.json` selecciona el release y sus rutas externas.

## Java 8 y Java 25 separados

La VM conserva el Java 8 del SIGER actual. SMR incluye **su propio Temurin Java 25 Windows x64** en `runtime\java25`, con licencias y manifiesto SHA-256 de todos los archivos. Incluye PowerShell 7 portable en `runtime\pwsh` y una copia del wrapper NSSM existente para preparar el servicio. No instala Java, registra JDK, modifica JAVA_HOME/PATH ni cambia asociaciones o servicios ajenos.

El launcher valida que `javaHome` pertenezca al mismo release y ejecuta `runtime\java25\bin\java.exe` mediante ruta absoluta. El servicio invoca PowerShell portable y este launcher. No utiliza `java` resuelto por PATH. El build usa Java25 explícito y restaura su entorno de proceso.

`Test-SmrJavaIsolation.ps1` verifica hashes y ejecuta Java25 en un proceso hijo sin JAVA_HOME ni Java en PATH; compara la resolución/hash del Java global y las variables persistentes antes/después. En desarrollo el Java global es 21; **no se presenta como prueba física sobre la VM Java8**. En destino ejecutar el mismo chequeo y comparar además `java -version` antes/después bajo la cuenta efectiva. No detener SIGER Java8 para instalar o revertir SMR.

## Acceso privado

Ejecutar con el PowerShell portable del release:

```powershell
& 'RELEASE\runtime\pwsh\pwsh.exe' -NoProfile -STA -File 'RELEASE\scripts\Show-SmrAccess.ps1' -Root C:\req\smr -Instance sintetico
```

Para el receptor público preparado, cambiar Instance a `recepcion`. El visor muestra usuario y contraseña enmascarada; permite revelarla localmente. Nunca copiar la contraseña al chat, logs o evidencias. No hay contraseña predeterminada. El archivo `access.dpapi` solo puede descifrarse bajo la identidad Windows que lo creó.

Login/logout, cookie Secure/HttpOnly/SameSite, sesión renovada, CSRF, límites de login, bcrypt y revocación reutilizan SIGER. Lectura y operación SMR son facultades explícitas persistidas como eventos de identidad; TI por sí solo no concede lectura. Se reutiliza `identity_event` y `security_epoch`, sin migraciones adicionales ni reinterpretar cargos. El bootstrap es transaccional y recuperable si cae después del COMMIT y antes de marcar el archivo aplicado; retira el texto claro inicial. No se entrega consola de administración institucional.

La bandeja filtra folio, tipo, canal, fecha de registro (America/Santiago) y estado ACK. El detalle vuelve a verificar firma, documento, registro y originales; conserva respuestas completas, relaciones, instituciones, fuentes, discrepancias, Unicode, nulls y campos compatibles adicionales. El JSON íntegro puede expandirse. El comprobante se descarga firmado.

Los originales se descargan bajo sesión/facultad y autorización por Comunicación/documento, con verificación SHA-256, no-store, attachment, nosniff y CSP sandbox. No se sirven estáticamente. Este perfil no dispone de workers calificados para representaciones: lo indica y ofrece el original; no ejecuta Office/PDF ni fabrica vistas. No se afirma una nueva calificación antivirus/documental nativa a partir de pruebas con dobles.

## Arranque y control

Los comandos siguientes usan scripts del release seleccionado, con PowerShell 7.4+ explícito. Sustituir RELEASE por su ruta absoluta.

```powershell
& 'RELEASE\scripts\Start-SmrMySql.ps1' -Root C:\req\smr
& 'RELEASE\scripts\Start-Smr.ps1' -Root C:\req\smr -Instance recepcion
& 'RELEASE\scripts\Start-Smr.ps1' -Root C:\req\smr -Instance sintetico
```

Para el productor sintético, `Start-TestProducer.ps1`. Es una herramienta de pruebas separada del JAR, con su propio firmante y Bearer; nunca sustituye al productor Ubuntu.

`Run-Smr.ps1` descifra DPAPI en memoria, materializa temporalmente la privada ACK bajo ACL y entrega contraseñas/configuración a Java por stdin anónimo. No hay secretos en argumentos o entorno global. Locks de launcher, escritor NTFS y conexión MySQL exclusiva impiden duplicados. El vínculo durable fija origen e ID de instalación; no se concilian ACK contra otro productor. El ID de instalación se conserva al migrar datos.

La UI funciona con el transporte desactivado. Si el origen falla, muestra estado/error y backoff, sin traducir TLS, 401/403/503 a cola vacía. Los contadores de proceso se distinguen de registros y ACK durables. Pausa/reanudación usan CAS con revisión, motivo, identidad y auditoría existentes. Conciliación y reintento de ACK bloqueado requieren facultad de operación. Las APIs de negocio y operación Bearer antigua quedan inaccesibles en SMR también en servidor.

```powershell
& 'RELEASE\scripts\Stop-Smr.ps1' -Root C:\req\smr -Instance recepcion
```

El script autentica privadamente, pausa, espera `enCurso=false`, registra drenaje y verifica PID/ejecutable/JAR antes de detener solo SMR. Los ACK pendientes permanecen. Para detener el motor propio, primero drenar/detener ambas aplicaciones y ejecutar `Stop-SmrMySql.ps1`; nunca MySQL80.

Los launchers locales se crean mediante WMI, independientes de la terminal del agente. No equivalen a un servicio registrado ni acreditan arranque tras reinicio/logoff. La cuenta actual no es administradora. El registro de servicio queda pendiente de privilegio/cuenta calificados.

## Servicio Windows preparado

`Install-SmrService.ps1` requiere administrador y PSCredential privada de la **misma identidad efectiva** de DPAPI. Rechaza otra identidad hasta reprovisión. Registra únicamente `SMRRecepcion`, y `SMRMySQL` si se usa el motor propio; los deja en Manual. El wrapper, PowerShell y Java tienen rutas explícitas. No cambia JAVA_HOME/PATH ni el servicio Java8 existente.

```powershell
$cuenta = Get-Credential
& 'RELEASE\scripts\Install-SmrService.ps1' -Root C:\req\smr -ServiceCredential $cuenta
```

TI debe calificar logon de servicio/perfil DPAPI, ACL, reinicio, recuperación y parada con drenaje antes de habilitar inicio automático. Parar mediante `Stop-SmrService.ps1`, que drena antes del SCM. Una parada directa/forzada del SCM o del host se trata como caída recuperable, no como drenaje acreditado. El código conserva COM/originales/ACK pendientes para ese caso.

## Primera instalación en la VM

1. Inventariar Java8/SIGER, puertos, cuentas, motor SQL, disco y servicios. No asumir que los parámetros del PC son los institucionales.
2. Copiar el paquete y verificar SHA-256 externo, JAR, dependencias y runtime-manifest. No recompilar. Usar su `runtime\pwsh\pwsh.exe` y Java25 propio.
3. Crear raíz nueva privada. Si se conserva la identidad ACK, ejecutar primero `Import-SmrSecrets.ps1` bajo la cuenta efectiva; después `Initialize-SmrIdentity.ps1` prepara carpetas sin regenerar la identidad existente. Si se elige identidad nueva, Initialize-SmrIdentity la genera y exige coordinar confianza histórica. Si TI aprueba motor independiente, `Initialize-SmrPlatform.ps1` lo aprovisiona con ejecutable MySQL explícito ya aprobado. Si se usa motor institucional compartido, **no ejecutar** Initialize-SmrPlatform: TI crea esquema/identidades SMR, proporciona TLS/CA y guarda credenciales DPAPI app/migrador/backup separadas. El descriptor `mysql\platform.json` describe ese motor; las herramientas administrativas acotadas al motor de desarrollo no se usan contra él.
4. Crear HTTPS válido para el hostname real; el certificado localhost del PC no se considera certificado de la VM. La privada ACK importada se reprotege por DPAPI, no se copia su XML antiguo.
5. `Initialize-SmrInstance.ps1 -Instance recepcion -Release RELEASE` prepara cuenta sintética/credencial aleatoria y configuración externa. Ajustar bind/puerto, JDBC VERIFY_IDENTITY, schema, javaHome dentro del release, ownerSid y principal MySQL aprobado. El esquema debe comenzar `smr_`; app/migrador cumplen los prefijos SMR. `institucional.db.principal` permite principal exacto distinto de @localhost.
6. En instalación vacía, `Migrate-Smr.ps1` aplica V1–V14 con migrador. Al restaurar custodia existente, restaurar sobre esquema vacío con historial incluido; no migrar antes ni reparar checksums. Runtime nunca ejecuta Flyway.
7. Arrancar cerrado; probar TLS/login/permisos/consulta, aislamiento Java8/25 y respaldo/restauración. Registrar/calificar solo servicios SMR. Abrir PULL únicamente con la coordinación concreta siguiente.

## Conexión real con Ubuntu

`C:\req\smr\public\PARA_CODEX_UBUNTU.md` contiene ID/pública/huellas y el paquete ACK real. La privada queda en Windows. Sonda Java real al dominio público: HEAD sin Bearer 401 y con falso 401, TLS/hostname verificados con la CA del borde; todavía falta Bearer válido para 404. No se usó POST reclamar como sonda.

```powershell
& 'RELEASE\scripts\Import-SmrBearer.ps1' -Root C:\req\smr
& 'RELEASE\scripts\Probe-SmrPublic.ps1' -Root C:\req\smr -WithBearer
```

El primer comando captura el Bearer existente de Ubuntu privadamente y lo protege por DPAPI; no lo regenera ni activa PULL. No se utiliza la contraseña ciudadana. El segundo solo hace HEAD con UUID/reserva cero, sin cookies, redirects o trust-all.

Ubuntu instala el mapa público en una ventana acordada con su herramienta existente, conservando verificadoras históricas: la importación reemplaza el mapa. Solo su responsable opera esos scripts. Antes de cualquier reclamo deben constar custodia en este PC, alcance de **toda** la cola elegible, receptor único, respaldo y apertura Ubuntu. Límite 1 no selecciona campaña/UUID.

Preparar un archivo privado a partir de `autorizacion-publica.example.json`, con referencia real, ventana UTC y huella exacta del mapa ACK. Ningún flag de ejemplo está aprobado. Con SMR recepción drenado/detenido:

```powershell
& 'RELEASE\scripts\Enable-SmrPublic.ps1' -Root C:\req\smr -ApprovalFile RUTA_PRIVADA
& 'RELEASE\scripts\Start-Smr.ps1' -Root C:\req\smr -Instance recepcion
```

Después activar el consumidor desde su UI autenticada. Antes de conciliar/reclamar/persistir/confirmar, el guardia vuelve a comprobar autorización/ventana y exclusión real. Si vence, falla cerrado y conserva pendientes. El operador hace el envío ciudadano autorizado; Windows no lo crea en su nombre.

El E2E solo se acredita comprobando mismo UUID/contenido/originales, COMMIT, folio estable, ACK 204, confirmación Ubuntu y visualización. Lo confirmado en este PC deja de ser descartable.

## Respaldo, recuperación y traslado

```powershell
$b = & 'RELEASE\scripts\Backup-Smr.ps1' -Root C:\req\smr -Instance recepcion
& 'RELEASE\scripts\Test-SmrRestore.ps1' -Root C:\req\smr -Instance recepcion -Backup $b
```

Backup pausa/drena, hace dump consistente con identidad SELECT/SHOW VIEW, copia objetos y confianza histórica, crea manifiesto de hashes y cifra por streaming con AES-256-CBC/HMAC-SHA256. Verifica el descifrado byte a byte; retira únicamente las copias transitorias creadas. Permanece pausado. La clave de recuperación DPAPI está separada del archivo cifrado; copiar el DPAPI a la VM no basta. No usar restauración de base vieja como rollback después de nuevos ACK.

El ensayo crea esquema/usuarios/custodia nuevos en el motor **propio SMR de desarrollo**, rechaza destinos con tablas, verifica hashes, Flyway, registros/firmas/originales y exclusión. Nunca llama al productor real. Solo mapea directivas DDL de vistas a definidor/esquema destino; no cambia INSERT, cuerpos JSON, comprobantes o binarios. En motor institucional compartido, TI prepara el destino vacío y ejecuta la restauración lógica bajo credencial de alcance acotado siguiendo el mismo procedimiento; no se entrega root al aplicativo.

`Export-SmrSecrets.ps1 -Destination ARCHIVO_NUEVO` genera recuperación portable cifrada bajo frase privada capturada por Get-Credential (PBKDF2/HMAC, frase por canal separado). Incluye ACK, confianza histórica, claves de backup y Bearer si existe, sin credenciales CU/PISEE. `Import-SmrSecrets.ps1` verifica integridad, rechaza sobrescribir identidad y reprotege con DPAPI bajo la cuenta/destino nuevos. Estos procedimientos de transferencia no se ejecutaron sobre una VM inexistente ni se presenta copiar XML como recuperación.

Traslado con datos reales: pausar/drenar Windows anterior, coordinar cierre PULL Ubuntu y entrega explícita de control; respaldar y verificar SQL, todos los objetos, folios/secuencias, recibos, reservas/ACK pendientes y confianza histórica. Restaurar en destino vacío, conservar origen e ID de instalación de `instance.json`, reprovisionar claves/TLS/credenciales, comprobar firmas/hashes/folios/pendientes y UI antes de activar un único consumidor. Copiar solo el JAR no migra la custodia; Ubuntu no reentrega lo confirmado. Un lock local/MySQL no coordina dos máquinas con bases independientes.

## Actualización y reversión

Conservar ambos releases inmutables. Drenar/parar solo SMR, verificar el nuevo paquete y ejecutar `Set-SmrRelease.ps1 -Instance recepcion -Release RELEASE_NUEVO`, luego Start-Smr. El journal conserva anterior/siguiente y no restaura datos. Para volver, repetir con el release previo compatible. Start-Smr y el despachador Service-Smr seleccionan el launcher del release elegido, evitando entregar propiedades nuevas a un JAR anterior. Cada release utiliza su propio Java25; Java8 global permanece igual. No bajar esquema ni restaurar SQL viejo. Las migraciones V1–V14 no cambiaron; no se añadieron versiones que colisionen con V15.1–V15.3 de la rama de lógica.

Las pruebas nuevas, bitácoras privadas, manifiestos, capturas sintéticas y checkpoint de esta entrega están bajo `C:\req\smr`; las evidencias QA/U4 previas se preservan en sus ubicaciones.
