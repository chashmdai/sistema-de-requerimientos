# Preparación SIGER Windows ↔ Formulario WSL · 31/10/2026

> **Actualización posterior:** la primera Comunicación sintética real ya fue acreditada con COM-002497. Ver [informe E2E](E2E_FORMULARIO_WSL_SIGER_WINDOWS_2026-10-31.md). Se conserva abajo el corte de preparación previo, que aún no había realizado extracciones. Para repetir el aislamiento usar una copia DPAPI y la raíz QA acordada, no activar la cola histórica por estos comandos antiguos.

## Dictamen

El consumidor institucional queda preparado para una **futura prueba local, manual y autorizada** contra el Formulario actual. Se acreditó conexión HTTPS y autenticación técnica de solo lectura entre ambos procesos. **No se acreditó E2E de una Comunicación:** no se reclamaron entregas, descargaron originales ni enviaron ACK al Formulario real. La recuperación pública permanece deshabilitada; el proceso SIGER usado para el smoke quedó detenido.

No se requiere cambiar el contrato público 1.0/1.1 ni se identificó una corrección indispensable en el Formulario. Antes de la campaña conjunta, su agente debe garantizar una cola reclamable aislada y sintética. El endpoint reclama el primer elemento disponible, no un UUID elegido por SIGER. Además, al último preflight había **1,78 GiB físicos disponibles**: cualquier operación del AV Windows exige al menos 4 GiB. No se redujo esa guarda ni se detuvieron procesos ajenos.

## Continuidad y alcance

- Punto de partida: U5 `48dd236368ca7e2229b1342c5a5a86933d9bff5b`, checkpoint del 27/10 y su validación técnica. Git estaba limpio.
- Rama local de preparación: `preparacion-interoperabilidad-formulario-2026-10-31`. `main` y `origin/main` siguen en `b322c9b905b533605fb53c8d706dbc144ed074a3`.
- No hay commits nuevos ni push en esta preparación. Los cambios quedan conservados en el working tree de esa rama.
- No se alteraron migraciones, U5 pendiente, credenciales existentes, histórico, servicios compartidos ni el Formulario. Responder/canalizar/escalar continúan deshabilitados.
- Se leyeron arquitectura del receptor transitorio, validación institucional, selector de nacionalidad del 31/10, custodia documental 1.0 y contrato multimedia 1.1. Se contrastaron con controladores, seguridad, recepción JDBC, identidad, validaciones y cliente consumidor efectivos.

## Formulario efectivo: no confundir con checkpoints históricos

Repositorio WSL limpio, `main` y referencia remota publicada en `c8d1a073880d6657b4e52297d04b084b17a077d1`. Se verificó mediante lectura de `/proc`, metadatos de lanzamiento y contenido ZIP:

| Dato | Resultado observado |
|---|---|
| Proceso objetivo | PID 93226, escucha en 18443 |
| JAR activo | `/home/nexbyteproveedor/.local/share/siger/nna-local-20261023/jars/11038950760953e166ed49d3bb15d9900a147ee84f4b0e80263b3de0773d41df.jar` |
| SHA-256 | `11038950760953e166ed49d3bb15d9900a147ee84f4b0e80263b3de0773d41df` |
| Proceso/selección/registro | Mismo JAR y SHA; PID y start ticks coincidentes; configuración sin cambios desde el lanzamiento |
| Funciones incorporadas | Identificación extranjera, campos de grupos NNA y catálogo de 249 países |
| Catálogo | Bytes del recurso incorporado idénticos al catálogo del repositorio revisado |
| Acceso desde Windows | `https://formulario.defensorianinez.gob.cl`, resolución 127.0.0.1, proxy existente hacia 18443 |

La confirmación del agente del Formulario sobre la instalación de `c8d1a07` concuerda con estas verificaciones. El JAR no incorpora `git.properties`: el SHA identifica inequívocamente el artefacto inspeccionado; no constituye por sí solo una atestación reproducible Git→binario. El laboratorio multimedia antiguo en 19443 y el contenedor antiguo no se toman como el proceso vigente. No se recompiló, reinstaló ni reinició WSL.

Se verificó `formulario.habilitado`, recepción y adultos habilitados. Las opciones de ciclo de vida online y eliminación permanecen falsas. SIGER no implementa retención pública ni depende del agente independiente para registrar/confirmar.

## Capacidades y compatibilidad

| Capacidad | Ya implementada | Evidencia de esta preparación / límite |
|---|---|---|
| HTTPS, Bearer técnico, sin redirects | `FuenteFormulario`; CA propia y validación de hostname | Windows HTTPS 200 y Java HTTPS 401/401/404 con CA explícita; sin bypass |
| Reclamar 1.0/1.1 | POST `/integracion/v3/formulario-completo/entregas/reclamar`, `X-Formulario-Versiones: 1.0,1.1`, límite 1 | Contraste con endpoint actual y tests HTTP con productor sintético; **sin POST real** |
| Documento íntegro | JSON completo, canonicalización, hashes separados, firma Ed25519 y claves confiables | Pruebas de contenido/procedencia/alteraciones y persistencia H2; sin nueva Comunicación pública real |
| Adulto extranjero sin RUN | Tipo `ADULTO_DECLARADO`; RUN/DV nulos, autenticación falsa | Pasaporte, identidad extranjera y otro; nacionalidad y excepción; texto `AB/123-xyz.45` conservado sin normalización, incluso tras reinicio |
| Grupo de NNA | Campos firmados `afectados-modalidad` y `grupo-nna-N-*` preservados | 1.0/1.1 grupal y 1.1 mixto; no se crean personas ficticias, Casos ni Requerimientos |
| Identidad | CU autenticada separada de SRCeI y declaración | Pruebas rechazan autenticación inventada, fuente registral elevada a autenticación y firma falsa; no hubo consultas gubernamentales |
| Originales privados | Reserva, streaming, tamaño, SHA-256, barreras NTFS, metadatos SQL | Regresión HTTP/H2 sintética: interrupción, corrupción, 12 MiB, reintento y reutilización; sin descarga del Formulario real |
| COM y ACK | COM único; comprobante por canal, COMMIT previo, revalidación de originales, conciliación | Tests de duplicados, reserva vencida, ACK perdido y reinicio. ACK adulto 1.0 / NNA 1.1 independientes de la versión documental |
| Presentación | Detalle conserva campos y procedencia | Se añadieron etiquetas de identificación extranjera/grupos; 51 tests frontend. Sin E2E nuevo de ambos procesos |

El consumidor V3 histórico se conserva. No se transforman ni refirman documentos recibidos para adaptarlos a un DTO nuevo. La identidad declarada extranjera no exige RUN, no consulta SRCeI ni adquiere autenticación por aportar un documento.

## Ajustes indispensables en SIGER

1. El bootstrap Windows anterior forzaba siempre el consumidor completo a falso y no admitía origen, credenciales ni CA. Ahora existe un **opt-in manual explícito**, limitado a modo documental QA, esquema QA/DML y hostname HTTPS autorizado resuelto a loopback. Exige ambas credenciales técnicas y CA privada. El valor predeterminado sigue siendo falso y `automatico` sigue forzado a falso. Graph real, consumidores históricos automáticos y Flyway permanecen cerrados.
2. El launcher puede consumir un perfil Windows privado. Reutiliza DPAPI CurrentUser y stdin anónimo. La credencial del Formulario se conserva cifrada, sin argumentos ni archivos de texto en Git. El token operacional es distinto del token público.
3. La confianza pública se obtiene del proceso activo y se combina con la confianza preservada. El launcher puede añadir el trust de los ingresos sintéticos QA existentes para seguir consultándolos. Una colisión de key ID con distinta clave falla; no reemplaza claves ni originales.
4. Se incorporó una sonda HEAD fija sobre UUID cero inexistentes. No construye un recuperador, abre una base institucional ni llama a reclamar/confirmar/fallo. No devuelve cuerpos registrales. El recurso inexistente fue contrastado con `recursoReservado`: retorna 404 antes de toda modificación o auditoría documental.
5. Se preparó una invocación operacional de una sola vez para la siguiente campaña. No se ejecutó. Requiere opt-in, run vivo propio, loopback/HTTPS y documental QA; no cambia controles operacionales ni reintenta automáticamente ante timeout.

No se incorporaron dependencias de runtime nuevas ni se modificó infraestructura. La inspección WSL usa Python `-B` exclusivamente para auditoría de lectura; SIGER continúa siendo Windows nativo.

## Verificaciones ejecutadas

Evidencia privada: `C:\req\evidencias\interop-formulario-2026-10-31-1ac9fc5812e34fddbb62cb590c20e935`.

| Verificación | Resultado |
|---|---|
| DNS / TLS Windows / capacidades | 127.0.0.1; HTTPS 200; multimedia/NNA/adultos true, sin omitir validación |
| TLS Java 25 con CA privada | Correcto; no redirects ni `trustAll` |
| Autenticación técnica real de lectura | HEAD sin Bearer 401, inválido 401, válido 404 |
| Confianza del ACK | Comprobante sintético firmado en memoria con clave Windows y verificado con confianza institucional del Formulario activo; **no enviado ni persistido** |
| Confianza de custodia pública | Key ID activo presente, PEM Ed25519 válidos y mapa privado preparado; no se ha verificado todavía un documento obtenido del proceso real |
| Backend Windows/H2, batería seleccionada | **41/41**, 0 fallos/errores/omitidas: 22 contrato/persistencia, 13 HTTP y 6 bootstrap |
| Frontend | **51/51**, lint y build correctos |
| Paquete Java 25 | Correcto, instalado por SHA fuera de `target`; no sustituye el JAR anterior |
| Arranque candidato cerrado | HTTPS localhost:8081 200, `publicRecovery=false`, `automaticPublicRecovery=false`, Graph real false; modo preliminar sin workers, detenido y secretos efímeros retirados |
| MySQL QA, solo SELECT/SHOW | TLS 1.3 `VERIFY_IDENTITY`, identidad app exclusivamente DML; 0 ACK pendientes, 0 COM duplicados, 0 Graph aceptados; conteos de negocio conservados |
| Histórico | **504 filas / 59 tablas idénticas**, 24 objetos / 16.869.263 bytes con SHA-256 idéntico, sin ACE inesperadas |
| Scripts PowerShell | Cinco parsers correctos; invocación sin opt-in rechazada antes de leer credenciales o hacer HTTP |

Se conservan los intentos intermedios: el primero usó el temporal predeterminado de Windows y produjo 35 errores de DACL; se corrigió la **invocación** usando el temporal privado previsto por la regresión existente. En el siguiente hubo un único fallo de limpieza (un elemento observado en `tmp`); no se determinó su causa, y pasó tanto aislado como en la batería final. No se cambió ese test ni la implementación de custodia, no se agregaron exclusiones ni esperas para obtener el resultado final. Los logs se conservan para investigar si reaparece.

No se repitió la batería MySQL destructiva ni la campaña documental/AV/U5 completa: el alcance fue interoperabilidad y la RAM no permite AV. Los éxitos históricos no se contabilizan como pruebas nuevas. El arranque con consumidor manual habilitado y la transferencia/ACK entre ambos procesos **quedan para la campaña autorizada**, no se deducen del smoke cerrado.

El control persistente de QA ya permite pull (`revision=1`, `NORMAL`, `pullEnabled=true`); no se cambió. Eso no activa una fuente: sin opt-in del bootstrap no existe consumidor habilitado, y su planificador siempre queda deshabilitado.

## Candidato conservado

`C:\req\bin\siger-9897abd7cd0bd57d.jar`

SHA-256: `9897abd7cd0bd57df6f0199e28904517c10d650bd7dd2795e34b2aa7b85084bf`.

El JAR previo `siger-7a50166482066535.jar` se conserva. El perfil de conexión está en `C:\req\config\formulario-wsl-integracion\perfil.json`; solo contiene rutas, metadatos públicos y estados. La credencial es DPAPI CurrentUser y sus ACL permanecen restringidas. Nunca adjuntar el archivo cifrado ni el directorio de configuración al repositorio o a informes.

## Procedimiento de prueba conjunta futura — no ejecutado

**Precondiciones:** autorización de E2E local; ninguna instancia SIGER usando la misma custodia/QA; ningún consumidor paralelo de la cola pública; no ejecutar regresiones destructivas; memoria suficiente para toda operación AV. El agente público debe confirmar el proceso vigente y que **solo las Comunicaciones sintéticas acordadas sean reclamables**. SIGER no ofrece selección por UUID ni debe vaciar o alterar la cola pública. Si no puede aislarse, detener esa fase y coordinar otra instancia/cola con su responsable.

1. Crear una raíz de evidencia nueva (cambiar el sufijo, no reutilizar una campaña). Auditar y preparar sin reclamaciones:

```powershell
New-Item -ItemType Directory -Path C:\req\evidencias\e2e-formulario-conjunto-CORRELACION
```
```powershell
& C:\req\repo\scripts\Windows-Inspeccionar-Formulario-WSL.ps1 -Evidencia C:\req\evidencias\e2e-formulario-conjunto-CORRELACION
```
```powershell
& C:\req\repo\scripts\Windows-Preparar-Conexion-Formulario.ps1 -Evidencia C:\req\evidencias\e2e-formulario-conjunto-CORRELACION
```
```powershell
& C:\req\repo\scripts\Windows-Preflight-Integracion-QA.ps1 -Evidencia C:\req\evidencias\e2e-formulario-conjunto-CORRELACION
```

En el primer enlace exigir cero ACK pendientes ajenos. Conservar custodia y confianza del QA existente; no regenerar fixtures. Ante cambio de clave/token/CA, el preparador falla sin sobrescribir silenciosamente: revisar la rotación con el responsable, mediante mecanismo privado. No pedir ni pegar secretos por chat.

2. Después de la autorización, iniciar desde otra consola el consumidor **manual**, sin scheduler. El siguiente comando es de la campaña futura y **no se ha ejecutado en esta preparación**:

```powershell
& C:\req\repo\scripts\Windows-Iniciar-QA.ps1 -Jar C:\req\bin\siger-9897abd7cd0bd57d.jar -Seconds 900 -Archivo C:\req\testdata\runtime-qa-b371266c31744ccc96f5f0668fcf7154\archivo -TrustPublicoQA C:\req\testdata\runtime-qa-b371266c31744ccc96f5f0668fcf7154\recepcion\publico-trust.json -DocumentalConfig C:\req\testdata\documental-win-4c816361d6ad4322a1bfd2e8c1dd2009\windows.properties -PerfilFormulario C:\req\config\formulario-wsl-integracion\perfil.json -HabilitarRecuperacionManual
```

3. El agente del Formulario presenta **una** Comunicación completamente sintética por su interfaz, idealmente adulto con documento extranjero y grupo NNA, o NNA sin RUN, más un archivo limpio conocido. No usar CU/SRCeI/Graph reales. Anotar UUID público, hashes, versión y correlación, sin datos personales en evidencias. Su responsable comprueba la cola antes de permitir reclamar.

4. Usar el `runtime` concreto devuelto por el launcher; invocar una sola vez. Sustituir `UUID_RUN` por ese run, no por un baseline viejo:

```powershell
& C:\req\repo\scripts\Windows-Recuperar-Formulario-Una-Vez.ps1 -Run C:\req\testdata\runtime-qa-UUID_RUN -AutorizadoE2ELocal
```

El helper verifica el control vigente y falla si está cerrado. No lo modifica automáticamente. La operación primero concilia confirmaciones pendientes y después reclama como máximo una entrega. Ante timeout, consultar ambos estados y COM antes de una nueva invocación; no suponer que no hubo COMMIT. La idempotencia es por UUID público, no por repetir el envío ciudadano.

5. En SIGER comprobar con cuenta autorizada: COM único, documento íntegro, fuentes individuales, ausencia de autenticación inventada/personas ficticias, archivo original tamaño/SHA idénticos, permisos negativos y representación segura. En MySQL comprobar enlace al registro y `ack_pending=false`. El agente público comprueba su estado y comprobante firmado: mismo UUID, COM, hashes y canal/versiones correctos. **Solo entonces** acreditar E2E de ese escenario.

6. Reabrir con el mismo JAR/custodia/trust y confirmar persistencia y ausencia de duplicados, sin repetir formularios. Probar fallos controlados solo en el aislamiento acordado. Detener el launcher propio mediante su archivo `stop` o límite de duración; volver a la operación cerrada por defecto. Repetir comparación del histórico y manifestar cero envíos Graph/proveedores.

## Reversión y límites

No hay migraciones que revertir ni datos transferidos desde el público real. Detener únicamente el run propio, conservar sus evidencias y usar el JAR anterior con los parámetros QA anteriores **sin** `-HabilitarRecuperacionManual` ni `-PerfilFormulario`. El perfil DPAPI queda inerte sin ese opt-in. No borrar custodias, originales, esquemas o respaldos; no resetear Git. Para retirar el código, conservar primero este working tree como trabajo local revisable.

No hay cambios que pedir al Formulario ahora. Quedan por coordinar: autorización de la campaña, exclusividad/aislamiento de cola, margen de RAM para AV y comprobación bilateral del ACK. Si la configuración/proceso cambian, repetir únicamente los preflights de lectura. Producción y operaciones U5 pendientes permanecen deshabilitadas.
