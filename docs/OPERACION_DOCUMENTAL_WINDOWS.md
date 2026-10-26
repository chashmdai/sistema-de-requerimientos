# Operación documental nativa Windows (QA)

Esta operación no instala servicios, no habilita producción y no hace cutover. El esquema histórico y sus 24 objetos permanecen separados de QA. No se necesitan WSL, Python, sockets Unix ni un broker de ejecución en el runtime. Python se usa exclusivamente para generar fixtures sintéticos en QA.

## Componentes

- Backend institucional Java 25, artefacto por SHA en `C:\req\bin`, nunca ejecutado desde `target`. JNA ya forma parte de sus dependencias.
- ClamAV 1.4.6 portable y bases oficiales privadas. qpdf 12.4.2, Poppler 26.09.0-0, FFmpeg 8.1 compartido LGPL. Versiones, fuentes y SHA de los ZIP están en `scripts/windows-documental/tools-lock.json`; cada instalación produce además un manifiesto de todos sus archivos.
- Inspector Office/TXT: módulo Java pequeño, incluido mediante jlink junto con `java.base` y `java.xml` del JDK autorizado. Su imagen privada tiene nombre derivado del SHA del código y manifiesto completo. No ejecuta Office ni extrae ZIP al filesystem.
- Un lanzador Java de operaciones cerradas: antivirus, comprobación PDF, metadatos, una página PNG, decodificación multimedia y Office/TXT. No hay shell, API de comando arbitrario, servicio adicional ni selección de ejecutable desde HTTP.
- Cada operación crea un AppContainer sin capacidades y un Job de un proceso asociado durante `CreateProcess` mediante `PROC_THREAD_ATTRIBUTE_JOB_LIST`. El backend no parsea PDF, XML Office, imágenes o medios bajo su token. Solo interpreta respuestas acotadas de los workers y las cabeceras PNG derivadas.

El antivirus conserva 4 GiB físicos libres, 1,5 GiB de memoria del Job, 45 segundos CPU, 90 segundos de reloj y 1 MiB de salida. Los parsers tienen 512 MiB de Job, 45 segundos CPU, 90 segundos de reloj y 8 MiB de salida; Office tiene además heap de 128 MiB. Entrada, archivos temporales, cantidad de archivos y tamaño de derivadas también están acotados. Hay un mutex por identidad Windows entre JVM y entre raíces de trabajos.

## Preparación reproducible

Los preparadores reutilizan y verifican instalaciones existentes; no sobrescriben paquetes alterados. No cambian PATH, JAVA_HOME global, servicios ni políticas. El JDK 25 institucional debe existir previamente.

```powershell
& C:\req\repo\scripts\Windows-Preparar-Herramientas-Documentales.ps1
```

```powershell
& C:\req\repo\scripts\Windows-Preparar-Inspector-Oficina.ps1
```

Para una campaña nueva, usar un Python Windows autorizado **solo como generador de QA**. El siguiente comando conserva el argumento explícito y no instala Python ni lo incorpora al runtime:

```powershell
& C:\req\repo\scripts\qa\Windows-Preparar-Fixtures-Documentales.ps1 -Python RUTA_ABSOLUTA_PYTHON_WINDOWS
```

Se crea una raíz nueva `documental-win-<uuid>`, fixtures sintéticos, configuración privada, directorios de trabajo y evidencia. La exclusión Kaspersky autorizada es exclusivamente `C:\req\testdata`; no se amplía ni se propone para originales ciudadanos reales.

## Calificación y regresiones

No mantener un runtime activo mientras se ejecuta la batería destructiva MySQL QA. No ejecutar scanners en paralelo. La guarda de memoria se evalúa antes de cada análisis AV; un bloqueo no es un resultado aprobado. Las tres exclusiones históricas de estrés se conservan.

```powershell
& C:\req\repo\scripts\qa\Windows-Probar-Antivirus-Java.ps1
```

```powershell
& C:\req\repo\scripts\qa\Windows-Probar-Control-AV-Real.ps1
```

```powershell
& C:\req\repo\scripts\qa\Windows-Calificar-Documental.ps1
```

El canario de aislamiento está identificado como tal y nunca se configura como AV de SIGER. Comprueba lectura, escritura, DACL, TCP/UDP/loopback y TCP externo con control positivo previo, ejecución de un segundo proceso, memoria, CPU, salida, timeout, interrupción, mutex y aborto de JVM. La prueba de CPU ocupa un solo proceso durante el límite autorizado; no ejecuta una campaña de carga.

```powershell
& C:\req\repo\scripts\Windows-Regresion.ps1 -Mode H2 -DocumentalRun ((Get-Content C:\req\scratch\ultimo-documental-win.txt).Trim())
```

```powershell
& C:\req\repo\scripts\Windows-Regresion.ps1 -Mode MySQL -DocumentalRun ((Get-Content C:\req\scratch\ultimo-documental-win.txt).Trim())
```

```powershell
& C:\req\repo\scripts\Windows-Regresion.ps1 -Mode Frontend
```

El migrador QA aplica/valida Flyway V1–V12; el runtime y las pruebas de negocio utilizan la cuenta DML. Las credenciales salen de DPAPI CurrentUser y viajan por stdin privado o entorno efímero del proceso de tests, nunca por argumentos ni variables globales persistentes. Se conserva `VERIFY_IDENTITY`. El esquema histórico está rechazado por las guardas de tests y del launcher.

## Runtime HTTPS y Edge

Después de calificar y compilar, instalar el JAR inactivo por SHA:

```powershell
& C:\req\repo\scripts\Windows-Regresion.ps1 -Mode Package
```

```powershell
& C:\req\repo\scripts\Windows-Preparar-Artefacto.ps1
```

Preparar identidades QA no reemplaza contraseñas. Preparar el productor de ingreso crea cuatro Comunicaciones sintéticas firmadas, ocho originales privados y comprobantes institucionales reales. Su **transporte de ACK es simulado en proceso**: no contacta al Formulario Público ni al agente de respaldo.

```powershell
& C:\req\repo\scripts\qa\Windows-Preparar-Identidades-Runtime.ps1
```

```powershell
& C:\req\repo\scripts\qa\Windows-Preparar-Ingreso-Documental.ps1 -Fixtures (((Get-Content C:\req\scratch\ultimo-documental-win.txt).Trim())+'\fixtures')
```

Arranque opt-in documental, exclusivamente sobre QA, puerto loopback previamente libre y certificados de localhost ya aprobados. Sustituir JAR_POR_SHA por el resultado del preparador. Mantiene recuperación pública, Graph real y envío automático desactivados. El trust sintético es una copia privada nueva, nunca reemplaza el trust institucional preservado.

```powershell
& C:\req\repo\scripts\Windows-Iniciar-QA.ps1 -Jar JAR_POR_SHA -Seconds 900 -DocumentalConfig (((Get-Content C:\req\scratch\ultimo-documental-win.txt).Trim())+'\windows.properties') -Archivo (((Get-Content C:\req\scratch\ultimo-ingreso-doc-qa.txt).Trim())+'\archivo') -TrustPublicoQA (((Get-Content C:\req\scratch\ultimo-ingreso-doc-qa.txt).Trim())+'\recepcion\publico-trust.json')
```

Desde otra consola, Edge real usa HTTPS sin ignorar errores TLS. Playwright es herramienta de QA; no es dependencia del runtime SIGER.

```powershell
& C:\req\repo\scripts\qa\Windows-E2E-Runtime.ps1 -Run ((Get-Content C:\req\scratch\ultimo-runtime-qa.txt).Trim()) -DocumentalRun ((Get-Content C:\req\scratch\ultimo-documental-win.txt).Trim()) -IngresoRun ((Get-Content C:\req\scratch\ultimo-ingreso-doc-qa.txt).Trim())
```

Para parar, escribir `stop` en la raíz exacta del run. El launcher termina únicamente su hijo y retira los secretos efímeros. El límite de duración también lo detiene. No se detienen procesos por nombre.

```powershell
New-Item -ItemType File -Path (((Get-Content C:\req\scratch\ultimo-runtime-qa.txt).Trim())+'\stop')
```

Reabrir con un launcher nuevo y el mismo `-Archivo`/trust, sin repetir recepción ni fixtures. Usar `-Mode after-restart -Baseline RUTA_RUN_ANTERIOR\e2e-operacion.json` para la comprobación de persistencia y reintentos. Cada `operate` constituye una campaña sintética nueva; no se repite para simular recepción de correos.

## Recuperación y límites de confianza

Antes de modificar ACL, el worker guarda un journal privado con descriptores y las identidades PID+fecha de creación. Usa archivo forzado, publicación `MoveFileEx WRITE_THROUGH` y `FlushFileBuffers` sobre un handle de directorio NTFS real. No sustituye fsync por una operación vacía. Al iniciar o procesar, bajo mutex, concilia únicamente su propio journal y un dueño terminado; no mata procesos por PID ni borra trabajos desconocidos.

El Job mata el hijo si cae la JVM. La recuperación valida paths exactos, SID derivado del perfil, DACL privada y ausencia del proceso original antes de restaurar ACL, retirar el perfil y borrar el trabajo propio. Se admite únicamente la ACE `CREATOR OWNER` heredable sin efecto directo que ya existía en la ACL guardada. Un journal ausente, alterado o un dueño todavía vivo falla cerrado y exige conciliación. No borrar estos directorios para conseguir un arranque verde.

El almacenamiento institucional NTFS conserva el protocolo validado de publicación/verificación/conciliación. Los originales son inmutables y se verifican por SHA; representaciones y referencias se mantienen separadas. Un error de AV/inspector devuelve fallo seguro y no publica documentos de gestión. El receptor público conserva su contrato firmado y la secuencia COMMIT/evidencia/comprobante/ACK; la portabilidad no modifica esos documentos ni añade retención pública.

La recepción ciudadana conserva la atestación `VALIDADO` del productor confiable dentro del documento firmado; no se introdujo una nueva política de rescaneo universal previo al ACK. El visor vuelve a comprobar AV/estructura antes de generar una representación nueva. Los documentos de gestión sí pasan AV e inspector antes de conservarse. Estas fronteras de confianza se mantienen respecto de las unidades cerradas.

Las pruebas acreditan el aislamiento del lanzador usado por todos los parsers y operaciones reales del pipeline; no equivalen a certificar la ausencia de vulnerabilidades de terceros ni a habilitar producción. Windows, JDK, ClamAV y paquetes documentales requieren mantenimiento de seguridad institucional.

Referencias: [atributos de creación y JOB_LIST](https://learn.microsoft.com/en-us/windows/win32/api/processthreadsapi/nf-processthreadsapi-updateprocthreadattribute), [AppContainer](https://learn.microsoft.com/en-us/windows/win32/secauthz/appcontainer-isolation), [Job Objects](https://learn.microsoft.com/en-us/windows/win32/procthread/job-objects), [SetNamedSecurityInfo y propagación](https://learn.microsoft.com/en-us/windows/win32/api/aclapi/nf-aclapi-setnamedsecurityinfow), [qpdf](https://qpdf.readthedocs.io/en/stable/installation.html), [FFmpeg y builds Windows](https://ffmpeg.org/download.html).
