# Consumidor automático de laboratorio · Windows → Formulario WSL

## Alcance autorizado

El humano autorizó sustituir el paso manual por el planificador automático existente **en la misma QA aislada**, para presentar personalmente una nueva Comunicación sintética. El E2E manual COM-002498 no se reenvía ni se usa como entrega nueva. No se crean ingresos por los agentes. **La recepción automática de una nueva Comunicación sigue pendiente del envío humano**: sondeos vacíos y pruebas con dobles no equivalen al nuevo E2E.

Se conservan el origen `https://formulario.defensorianinez.gob.cl` resuelto a loopback, perfil20d434…, credenciales privadas DPAPI, CA y claves públicas, MySQL QA y custodia NTFS existentes. Formulario continúa bajo responsabilidad exclusiva de su agente.

## Cambios compatibles

- `ArranqueWindows`: opt-in `siger.windows.formulario-automatico=true`, distinto y excluyente del modo manual. Exige el mismo esquema QA, aplicación DML, documental calificado, TLS, CA privada, origen exacto aprobado y resolución loopback. No admite activar directamente propiedades generales ni Graph real, V2 o integraciones históricas. El valor predeterminado sigue cerrado.
- Launcher: `-HabilitarConsumidorAutomaticoQA -MantenerDisponible`, únicamente con perfil E2E aislado explícito. No instala servicio ni tarea ni cambia Java global.
- Se reutilizan `FuenteFormulario`, `RecuperarFormulario` y el planificador: lote1, versiones1.0/1.1, streaming, firmas/huellas, originales antes de persistir y COMMIT antes de ACK. Conciliación antes del siguiente reclamo y backoff10s→5min existentes.
- Exclusividad MySQL mediante lock de conexión dedicado al esquema QA. Rechaza otra instancia automática incluso con otra custodia. Se comprueba la propiedad antes de reclamar/conciliar y antes de persistir/confirmar; si se pierde, falla cerrado. No crea tablas ni requiere privilegios administrativos. Se conservan el bloqueo de escritor de custodia y la exclusión en memoria.
- Estado autenticado `GET /operacion/v1/formulario-completo/estado`: ciclos, último intento/éxito, backoff, errores sanitizados y contadores de recuperación/confirmación/conciliación. No contiene documentos, destinatarios ni secretos. Sin credencial devuelve401.
- Los verificadores de solo lectura admiten el runtime automático, sin pararlo, reclamar ni confirmar entregas desde esos verificadores.

No cambia ningún contrato externo, migración, firma histórica, folio, facultad U5 o regla del núcleo.

## Artefacto y proceso

Candidato conservado fuera de `target`: `C:\req\bin\siger-ae70ab769605c1e9.jar`, SHA-256 `ae70ab769605c1e93eab0de052ceee46f724a7b9dfe684a45227717d6a07c2f0`. El JAR anterior9897abd… permanece intacto.

Run actual `C:\req\testdata\runtime-qa-9f6a743c07f1461b93f2ade3ede8d035`, JavaPID163036, launcherPID188044; `https://localhost:8081/`, binding127.0.0.1. Usuario operativo `DEFENSORIANINEZ\nexbyte.proveedor`, Java25 explícito y MySQL TLS1.3/VERIFY_IDENTITY con `siger_windows_qa_app` DML. Sin Flyway ni Graph real en runtime.

El proceso temporal de revisión anterior ya no estaba activo al preparar esta unidad; no había archivo `stop`, salida final ni limpieza de sus copias efímeras. La causa de terminación no se acreditó. No se atribuyó a caída de base o pérdida de datos.

Para la continuidad actual se utilizó **creación local Windows mediante CIM, oculta y bajo el mismo usuario**, padre verificado `WmiPrvSE`. El launcher queda desacoplado del comando temporal del agente y sin vencimiento; no se alteró WMI, políticas, servicios, tareas programadas ni privilegios. Esto no es un supervisor productivo: un fallo del proceso o reinicio del anfitrión requiere reanudación explícita.

## Pruebas de esta implementación

-49 pruebas seleccionadas correctas en Windows/H2: ArranqueWindows8, Planificador6, RecuperarFormularioHttp13, FormularioCompletoInstitucional22. Cero fallidas/errores/omisiones en la batería final.
- Primer intento:35errores porque JUnit utilizó el directorio temporal global con ACL incompatibles y1error de restubbing del doble Mockito. Se corrigió el comando para temporales privados bajo `C:\req\testdata` y el doble; no se relajó ninguna ACL ni exclusión de pruebas.
- MySQL QA real no destructivo: primera adquisición válida, segundo consumidor rechazado, liberación/reapertura válidas. Otra adquisición también fue rechazada mientras el runtime automático estaba activo.
- Sondeos HTTPS reales de cola vacía, varios ciclos correctos y estado **ESPERANDO_ENTREGAS**; contadores de entregas/ACK nuevos0. Estos son PULL automáticos reales sin entrega, no invocaciones manuales ni Comunicación sintética creada por los agentes.
-26 comprobaciones Edge/HTTPS sobre COM-002498 preservada: login `nexbyte`, facultad Distribución institucional, buzón/detalle/formulario, dos originales, descargas, vistas PNG/PDF y denegación sin sesión. Firmas y hashes de COM-002498 siguen válidos.
- Base QA sin nuevos COM/documentos por los sondeos; cero ACK pendientes y cero folios duplicados. Histórico504filas/59tablas idéntico y24objetos con SHA-256/ACL conservados.
- No se ejecutó la batería MySQL de fixtures que borra datos, ni regresión general U5, ni proveedores reales. Frontend sin cambios nuevos; la interfaz empaquetada se comprobó realmente en Edge.

## Operación reproducible

Antes de reanudar: comprobar ausencia del proceso anterior y listener, cero ACK pendientes de otro origen, identidad del perfil/CA/claves y estado de la misma QA pública con su agente. No iniciar dos procesos sobre la misma base/custodia. No cambiar de perfil para resolver un pendiente.

Arranque local oculto, sin secretos en argumentos; ejecutar solo cuando no exista el runtime y se haya autorizado reanudar la QA:

```powershell
$startup=New-CimInstance -ClassName Win32_ProcessStartup -ClientOnly -Property @{ShowWindow=[uint16]0}; Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{CommandLine='"C:\Program Files\PowerShell\7\pwsh.exe" -NoProfile -NonInteractive -File C:\req\repo\scripts\Windows-Iniciar-QA.ps1 -Jar C:\req\bin\siger-ae70ab769605c1e9.jar -Archivo C:\req\testdata\runtime-qa-b371266c31744ccc96f5f0668fcf7154\archivo -TrustPublicoQA C:\req\testdata\runtime-qa-5e52e30513944a6c9204d94979499a1e\recepcion\publico-trust.json -DocumentalConfig C:\req\testdata\documental-win-4c816361d6ad4322a1bfd2e8c1dd2009\windows.properties -PerfilFormulario C:\req\config\formulario-wsl-e2e-20d4349425a943378b12328fb67e5711\perfil.json -HabilitarConsumidorAutomaticoQA -MantenerDisponible';CurrentDirectory='C:\req\repo';ProcessStartupInformation=$startup}
```

El arranque no abre por sí mismo `operation_control`; si la base ya estaba habilitada, el planificador sí lo respetará. Comprobar siempre ese control antes de arrancar y coordinar que la recepción ciudadana esté cerrada durante preparación. Tras crear un nuevo run, actualizar el puntero correspondiente únicamente después de comprobar su PID, perfil y listener.

Consultar estado sin mutación:

```powershell
& C:\req\repo\scripts\Windows-Control-Consumidor-QA.ps1 -Run ((Get-Content C:\req\scratch\runtime-consumidor-automatico-qa.txt -Raw).Trim()) -Accion estado
```

Habilitar tras aprobación/preflights, mediante API auditada:

```powershell
& C:\req\repo\scripts\Windows-Control-Consumidor-QA.ps1 -Run ((Get-Content C:\req\scratch\runtime-consumidor-automatico-qa.txt -Raw).Trim()) -Accion habilitar -AutorizadoLaboratorio
```

Pausar cuando el humano lo solicite, conservando interfaz y datos:

```powershell
& C:\req\repo\scripts\Windows-Control-Consumidor-QA.ps1 -Run ((Get-Content C:\req\scratch\runtime-consumidor-automatico-qa.txt -Raw).Trim()) -Accion pausar -AutorizadoLaboratorio
```

Pausar impide nuevos ciclos; no equivale a cancelar una transferencia ya iniciada. Dejar terminar/conciliar antes de una parada autorizada. Cerrar ingreso/PULL públicos solo a través de su agente. No borrar COM, originales, outbox ni colas para revertir.

## Prueba humana pendiente

Cuando ambos agentes confirmen listos, abrir el Formulario local desde inicio en una sesión privada nueva; completar personalmente un único ingreso sintético sin CU/PISEE. Esperar el sondeo automático, actualizar el comprobante y buscar el nuevo folio en SIGER con `nexbyte`.

No pedir una autorización manual adicional por cada PULL de esta campaña: la recepción automática QA ya está autorizada. No invocar `Windows-Recuperar-Formulario-Una-Vez.ps1` para acreditar el modo automático. Después del envío humano verificar UUID nuevo, documento íntegro, originales, único folio, firmas, COMMIT/ACK y estado CONFIRMADA en el origen. No reutilizar COM-002498 como si fuera ese nuevo resultado.

La campaña privada y sus controles finales están referenciados en el checkpoint. No declarar producción lista, disponibilidad tras reinicio del host ni migración a DDN-UBUNTU acreditadas por esta preparación.
