# U4.5 — ejecución real de AntivirusDocumentoWindows

26/10/2026, America/Santiago. Rama `migracion-windows-laboratorio-2026`, código **2a6c6d2**, desde `0684792`. No hay cierre de U4.5, push ni habilitación del runtime documental.

## Resultado y alcance exactos

El adaptador Java ejecutó realmente ClamAV en Windows: **Clean=0, EICAR=1 y Unavailable=2**, con AppContainer sin capacidades, Job de un proceso, memoria1,5GiB, CPU45s y plazo90s. No se redujo la guarda de4GiB, ni el heap256MiB del harness, ni los criterios de aprobación.

Esta pasada corresponde al adaptador con la corrección de propagación de ACL, **anterior al refuerzo posterior JOB_LIST**. El código actual incorpora ese refuerzo, compila y supera cinco guardas, pero su ejecución en vivo está pendiente: la RAM volvió a caer bajo el mínimo. No trasladar automáticamente la calificación de la pasada anterior a esa modificación ni declarar completo el adaptador definitivo.

| Escenario real Java | Exit | Pico privado del hijo | Tiempo | Resultado |
|---|---:|---:|---:|---|
| Clean | 0 | 1.037.467.648 bytes |17.501ms| Entrada actual OK |
| EICAR | 1 | 1.037.463.552 bytes |16.358ms| Eicar-Test-Signature FOUND |
| Unavailable | 2 |7.458.816 bytes|87ms| Base vacía, IOException; nunca limpio |

Los tres resultados registran cleanupComplete=true. Se comprobó retorno posterior al mismo almacén de trabajos sin `av-*` pendiente; no quedaron hijos clamscan. Las salidas acotadas se conservaron bajo una raíz de evidencia privada opt-in; no se copió el original al diagnóstico. Root:

`C:\req\testdata\av-java-e89ba516a4b649b2aa75dc6c9d27f290`

La auditoría posterior del paquete AV y las bases encontró **0ALLOW residuales** de los SIDs temporales. La custodia histórica mantiene24objetos,16.869.263bytes, todos los SHA/tamaños correctos y0ALLOW inesperados en1.932objetos auditados. No se conectó a MySQL institucional en esta unidad.

## Fallo concreto encontrado y corregido

El primer ensayo Java volvió a ser detenido por su guarda interna de memoria, aun cuando el preflight externo había superado4GiB. El harness ejecutaba la compilación source-file dentro de la propia JVM del ensayo. Ahora javac termina antes de iniciar la JVM que mide y ejecuta el adaptador. La guarda interna sigue siendo obligatoria; se añadió una excepción tipada con bytes disponibles/requeridos para conservar el diagnóstico de memoria. No se interpreta la disponibilidad del preflight como una reserva de RAM.

Al conseguir margen, el hijo terminó **-1073741790 /0xC0000022 (acceso denegado)**, en117ms, con unos6,3MiB privados y sin stdout/stderr. No era el exit2 de ClamAV ni evidencia de firmas corruptas o interferencia de Kaspersky.

El adaptador usaba SetFileSecurity para conceder la ACE heredable al directorio de herramientas/bases. Esa API no propaga la actualización a los hijos existentes. Se sustituyó por SetNamedSecurityInfo, tanto al conceder como al retirar el permiso temporal, conservando el estado protegido/heredado original. La [documentación de SetFileSecurity](https://learn.microsoft.com/en-us/windows/win32/api/securitybaseapi/nf-securitybaseapi-setfilesecurityw) describe esa limitación; [SetNamedSecurityInfo](https://learn.microsoft.com/en-us/windows/win32/api/aclapi/nf-aclapi-setnamedsecurityinfow) propaga ACE heredables a objetos existentes. Un test NTFS comprueba concesión y retirada en un archivo preexistente anidado. Después de esta corrección el mismo scanner completó0/1/2.

El intento fallido con diagnóstico quedó conservado en `av-java-f226a15977fa4323a9224707e9ed05c1`, con entrada, journal y salidas vacías; perfil/proceso fueron retirados. No se borró ese directorio para fingir una conciliación. Otro intento previo no había conservado las salidas tras un exit anormal: se añadió evidencia privada opt-in y captura del último resultado. Un exit2 conocido y conciliado puede retirar sus temporales, conservar los logs y permitir reintento; nunca se convierte en limpio.

## Calificación adicional y cambios posteriores

- **Cinco guardas focalizadas correctas**, sin omisiones: manifiesto, alteración, raíces/estado pendiente y nueva propagación/restauración real de ACL. Log: `C:\req\logs\guardas-av-job-creacion-windows.log`.
- **Dos JVM y raíces diferentes** fueron rechazadas por el mismo mutex antes de crear scanner. Tras liberarlo, se alcanzó la validación del paquete. Es un test sintético de exclusión mutua, no otro escaneo real. Evidencia: `C:\req\testdata\av-mutex-ff86b24abe7548c1bd8fa829be43bd89\resultado.json`.
- Se detectó una ventana de orfandad potencial entre CreateProcess suspendido y AssignProcessToJobObject. El código actual usa **PROC_THREAD_ATTRIBUTE_JOB_LIST** para asociar el Job durante CreateProcess y comprueba IsProcessInJob antes de ResumeThread. Mantiene los mismos límites y KILL_ON_JOB_CLOSE. La [API oficial](https://learn.microsoft.com/en-us/windows/win32/api/processthreadsapi/nf-processthreadsapi-updateprocthreadattribute) soporta JOB_LIST desde Server2016. **Pendiente prueba real de este refuerzo y de crash/reinicio/recuperación; no se afirma que la documentación sustituya el ensayo.**
- ServiciosDocumentalesRealesTest admite el AV Windows real; inspector/renderizador se inicializan solo cuando el método los necesita. No se crean sustitutos para los workers ausentes. Los cuatro métodos restantes fallarán si se intenta seleccionarlos sin sus workers, en lugar de aprobarse artificialmente. Las cinco anotaciones Test y la condición histórica de configuración siguen presentes.
- Se ejecutó `controlAntivirusRealSeRechaza` con Surefire, en modo forked y sin fork. **Ambos intentos terminaron con un error de memoria,0éxitos y0omitidas; no se cuentan como aprobados.** La pasada0/1/2 anterior es un harness distinto, no una aprobación de ese método JUnit.
- Para evitar mantener Maven en memoria durante el scanner se preparó `Windows-Probar-Control-AV-Real.ps1`, usando el launcher JUnit6.0.3 ya instalado, de la misma versión que el engine. Compilación correcta; ensayo detenido en preflight con **3.742.486.528bytes disponibles**, requeridos4.294.967.296. No inicia scanner ni omite la guarda interna. No se descargó otra herramienta/dependencia ni se cambiaron políticas.

No se repitieron H2/MySQL/frontend completos: el baseline anterior218por motor/42frontend no es evidencia nueva de este corte. Las cinco pruebas documentales aún no están aprobadas. No hubo cambios en V1–V12, contratos, frontend, JAR activo, identidades o datos. No se usó WSL, Formulario Público, proveedores gubernamentales, correo real, otros servicios o políticas Kaspersky.

## Matriz pendiente antes del runtime

| Control | Evidencia del corte / pendiente |
|---|---|
| Lectura/escritura fuera del trabajo | ACL propagada y retirada probadas; negativos con el lanzador definitivo y cada parser pendientes. Los canarios C# previos no se trasladan a Java. |
| Red | Token AV sin capacidades comprobado; negativos TCP/UDP con el perfil definitivo pendientes. |
| Procesos auxiliares | Job de un proceso comprobado; negativo y nueva asociación atómica por probar en vivo. |
| Recursos | Límites consultados y picos de las tres ejecuciones reales; negativos completos y perfiles de parsers pendientes. |
| Interrupción/crash/recuperación | JOB_LIST implementado; ensayos y conciliación durable del journal pendientes. |
| Fallo de AV/inspector cerrado | AV vacío real rechaza con IOException; inspector Windows pendiente. |
| Contenido inseguro no publicado | EICAR real detectado; ingreso/visor/persistencia completos y cinco métodos reales pendientes. |

Poppler Windows ya existe en el bundle local del equipo; no está preparado como paquete institucional ni calificado dentro del nuevo lanzador. qpdf, ffprobe y ffmpeg no están en PATH. No se ejecutó ninguno sobre documentos fuera del aislamiento ni se creó dependencia operacional del bundle de Codex. La siguiente fase debe reutilizar herramientas fijadas y verificadas en C:\req, con un lanzador común de operaciones fijas cuando exista un segundo consumidor real; no una API de comandos arbitrarios.

Reanudación exacta y evidencia: [checkpoint](CHECKPOINT_ADAPTADOR_AV_WINDOWS_2026-10-26.md), [resultados](RESULTADOS_ADAPTADOR_AV_WINDOWS_2026-10-26.json). Se conserva el [corte anterior](CHECKPOINT_CLAMAV_WINDOWS_2026-10-26.md). **Runtime documental y producción permanecen deshabilitados.**
