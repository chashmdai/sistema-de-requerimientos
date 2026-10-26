# Evaluación de workers Windows — 26/10/2026

Este diagnóstico no certifica un sandbox ni habilita procesamiento documental. La custodia NTFS y sus tests simulados son un frente distinto. No se ejecutaron documentos no confiables mediante un proceso Windows sin aislamiento validado.

## Origen inspeccionado, sin cambios

Se leyeron infra/media/inspector.py (175 líneas), sandbox.c (49 líneas), Dockerfiles y clamd.conf del Formulario Público, y las implementaciones institucionales AntivirusDocumentoLocal, InspectorDocumentoLocal y PdfAislado. No se modificó WSL ni formulario-web, no se llamó a sus workers como runtime Windows.

- ClamAV usa INSTREAM binario por socket Unix privado, PING, respuesta acotada y deadlines. Exige AlertExceedsMax=yes, StreamMaxLength y MaxFileSize suficientes y MaxScanSize doble. MaxThreads=1, cola=2, MaxScanTime=60000, recursión 16 y hasta 2000 entradas. Un error de scanner no es limpio.
- Inspector: proceso no root y aislamiento Landlock ABI>=3/seccomp. Solo trabajo actual RW y herramientas/librerías confiables RO. Red, ptrace, mount, namespaces y señales peligrosas denegadas. CPU45s, memoria768MiB, salida16MiB y 64 descriptores. El padre termina el grupo en timeout.
- El inspector Python valida ZIP/OOXML/ODT con límites y defusedxml; qpdf inspecciona PDF activo/cifrado y Poppler crea PNG. FFprobe/FFmpeg validan contenedores, streams, codecs, dimensiones/duración y decodificación efectiva. No basta con magic bytes/extensión.
- El renderer institucional PDF usa además NPROC=1, plazo20s, 1–100 páginas, escala1600px y PNG máximo6MiB, y autotest obligatorio que deniega canario externo/socket/fork.

## Comprobaciones Windows reales

- Poppler (pdfinfo/pdftoppm) está disponible como herramienta nativa del runtime local, pero no está integrado en un worker institucional aislado.
- clamd/clamscan, qpdf y FFmpeg/FFprobe no se detectaron en PATH ni como herramientas de SIGER en C:\req\bin. No se inspeccionó C:\agente ni se reutilizaron sus servicios. Esto no afirma que no existan en alguna carpeta ajena del host.
- Una sonda creó un perfil AppContainer efímero de la cuenta actual, sin capacidades, y lo eliminó mediante DeleteAppContainerProfile: ambos HRESULT0. No dejó un perfil de SIGER instalado.
- CreateJobObject y SetInformationJobObject aceptaron máximo1 proceso, memoria128MiB y KILL_ON_JOB_CLOSE; se cerró el handle. La sonda NO lanzó hijos ni demostró aplicación efectiva de esos límites.
- No se instalaron características Windows, servicios, tareas, reglas de firewall o excepciones al antivirus del anfitrión. No se elevaron privilegios.

Resultado de la sonda: scripts/Windows-Sonda-Aislamiento.ps1; evidencia en C:\req\evidencias\sonda-aislamiento-windows-2026-10-26.json. Es disponibilidad mecánica de API, no prueba de equivalencia.

## Bloqueo de ingeniería concreto

El cliente Unix actual y el helper ELF/prlimit no se ejecutan como componentes Windows. Reemplazar sus comandos por cmd/PowerShell o lanzar los parsers como el usuario operativo daría acceso a secretos/custodia del servidor y no satisface el encargo. Tampoco basta con Job Objects: no son una política de acceso a archivos o red.

No se puede portar literalmente el inspector Python dentro de un Job de un solo proceso: llama subprocess para qpdf/FFprobe/FFmpeg/Poppler. Hace falta un broker confiable externo que seleccione una allowlist fija de binarios/argumentos y lance cada parser en su propio AppContainer/Job. Permitir procesos arbitrarios para conservar ese subprocess sería un cambio de garantía que no se adopta.

Hay que preparar un paquete nativo verificable (incluidas DLL, runtimes, licencias, firmas/versiones y bases AV), definir las ACL por trabajo, aislar red/credenciales, limitar la herencia de handles, imponer límites/timeout con proceso inicialmente suspendido y cerrar siempre el Job. La respuesta a Java debe estar limitada/autenticada y no puede ser una simple cadena de salud que autoafirme estar aislada.

## Alternativas y costo de complejidad

| Alternativa | Complejidad | Riesgos y condiciones |
|---|---|---|
| Broker nativo AppContainer + Job Objects, parsers en allowlist y AV nativo | Alta: nuevo componente de seguridad, distribución de herramientas y suite negativa obligatoria | Es la vía compatible con el objetivo nativo. Revisar permisos efectivos, acceso de DLL, capabilities, red/loopback, handles heredados y supervivencia de hijos. APIs disponibles no equivalen a garantía. Sin servicio permanente; ciclo de vida acoplado al proceso de laboratorio. |
| Token restringido + Job Objects sin AppContainer | Alta y garantía de red no resuelta | Token/Job por sí solos no acreditan denegación de red. Añadir WFP/firewall/políticas requiere diseño y autorización adicional del host. No se selecciona como atajo. |
| Worker en VM/entorno aislado dedicado con protocolo autenticado | Media/alta e infraestructura/operación adicional | Puede preservar aislamiento si se verifica, pero un worker Linux seguiría siendo dependencia crítica Linux: no cierra el criterio totalmente nativo. Requiere aceptación explícita de cambio de arquitectura y autorización de infraestructura; no se implementó silenciosamente. |

ClamAV ofrece un paquete ZIP oficial para Windows, por lo que registrar un servicio no es un requisito inevitable. No se instaló ni procesó contenido hasta disponer del diseño/validación del broker. No se eligió un antivirus simulado como sustituto.

Referencias primarias: [lanzar AppContainer](https://learn.microsoft.com/en-us/windows/win32/secauthz/implementing-an-appcontainer), [Job Objects](https://learn.microsoft.com/en-us/windows/win32/procthread/job-objects), [API de perfil](https://learn.microsoft.com/en-us/windows/win32/api/userenv/nf-userenv-createappcontainerprofile), [ClamAV Windows](https://docs.clamav.net/faq/faq-win32.html).

## Matriz de aceptación pendiente

| Prueba de worker real Windows | Estado |
|---|---|
| Denegar lectura de canario externo y secretos | NO EJECUTADA |
| Denegar escritura fuera del trabajo | NO EJECUTADA |
| Denegar red, incluida prueba loopback controlada | NO EJECUTADA |
| Denegar ejecución de auxiliares no autorizados | NO EJECUTADA |
| Aplicación efectiva de límites de memoria/CPU/procesos/salida | NO EJECUTADA |
| Timeout/crash sin procesos huérfanos ni temporales inseguros | NO EJECUTADA |
| AV/inspector caídos producen rechazo seguro, con procesos reales | NO EJECUTADA |
| Archivo inválido/malicioso real controlado no llega a publicación | NO EJECUTADA |

Las simulaciones H2/MySQL sí verifican rechazo del scanner, contenido inválido, SQL fallido, ausencia de publicación/ACK y permisos por recurso. No se confunden con esta matriz. Los cinco tests existentes de ServiciosDocumentalesRealesTest permanecen omitidos por falta del worker nativo, sin exclusiones nuevas.

## Decisión técnica siguiente

Implementar/revisar primero el broker de la primera alternativa y su harness de ocho pruebas negativas; solo después conectar AV, inspector, renderer y habilitar el runtime documental. Si aparecen necesidades de privilegios/servicios/cambios importantes al host, parar ese frente y pedir autorización específica con el cambio concreto. No hay una nueva definición funcional institucional que inventar ni se inicia U5.