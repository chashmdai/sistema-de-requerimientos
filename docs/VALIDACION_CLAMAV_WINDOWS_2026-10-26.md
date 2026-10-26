# U4.5 — ClamAV Windows independiente acreditado; integración documental abierta

26/10/2026, America/Santiago. Rama `migracion-windows-laboratorio-2026`, desde `c224360`; implementación **82cf340**. Se conservó Git limpio antes de comenzar, sin repetir migración, infraestructura, importaciones o preparación del paquete. No hay push ni cierre de U4.5.

## Alcance y autorización

TI informó haber aplicado una exclusión de Kaspersky exclusivamente a `C:\req\testdata`. Esta ejecución no consultó ni modificó su consola, políticas, exclusiones, servicios o proveedor AMSI. No se desactivó ningún antivirus. Solo se generaron archivos sintéticos bajo ese directorio privado.

El control EICAR se creó en `isolation-27a727374a094306b1686e779864b0ac\work-av-capacity-eicar\input.txt`. Sus 68 bytes y SHA-256 se conservaron inmediatamente y después de diez segundos. Esto comprueba persistencia en esa observación; **no certifica toda la política de Kaspersky ni sustituye una detección de ClamAV**. No se colocó EICAR fuera del directorio autorizado ni dentro de Git.

## Resultados nuevos del scanner

La primera lectura del host mostró unos 2,9 GiB libres. Sin detener procesos ni cambiar servicios, la memoria disponible aumentó antes del ensayo y las guardas originales permitieron ejecutarlo. No se omitió ni rebajó la guarda de 4 GiB. La prueba original pasó los tres escenarios en `isolation-f8b7a69ca2594bbab36f853de9e3cfc1`.

Se amplió la instrumentación y se repitió una vez para validar esa modificación. Esta segunda pasada partió con **5.028.102.144 bytes físicos disponibles** y quedó registrada en `C:\req\evidencias\capacidad-av-475e581eb13d4ba38833037657a85b5f.json` y en la raíz de trabajo correspondiente.

| Escenario | clamscan | Resultado comprobado | Pico privado del proceso | Tiempo transcurrido |
|---|---:|---|---:|---:|
| Clean | 0 | `input.txt: OK` | 1.037.164.544 bytes | 17.419 ms |
| EICAR | 1 | `Eicar-Test-Signature FOUND`; entrada intacta | 1.037.139.968 bytes | 18.541 ms |
| Unavailable | 2 | Base inexistente; nunca clasificado como limpio | 6.578.176 bytes | 34 ms |

El ejecutable supervisor terminó 0 en los tres: ese código indica que pudo ejecutar y recuperar el hijo. **No confundirlo con el código de clamscan**, que fue 0/1/2. No hubo timeout ni PID hijo superviviente. Todos los perfiles se retiraron y la auditoría de raíz, paquete AV y bases encontró cero ACE ALLOW residuales de AppContainer.

Antes de reanudar cada hijo se consultaron su token y los límites del Job: AppContainer=true, capacidades=0, un proceso, CPU 45 segundos, KILL_ON_JOB_CLOSE. El presupuesto Clean/EICAR se mantuvo en **1,5 GiB** y el plazo en 90 segundos. El escenario histórico `av-unavailable` conserva su límite más estricto de **768 MiB**, porque no carga bases; la evidencia registra el valor efectivo, no 1,5 GiB ficticios para ese caso. El pico privado de unos 989 MiB no equivale a RAM física ni acredita capacidad para documentos mayores/concurrentes.

stdout y stderr se conservaron por separado como `salida.log` y `stderr.log`, junto a sus tamaños y SHA-256, salida del supervisor, entradas y listado de temporales. El límite conjunto de salida sigue siendo 1 MiB. La evidencia identifica PID, duración, memoria, límites consultados y retirada del perfil. No se reutilizaron esos logs sintéticos como logs de producción.

## Error anterior: qué se puede afirmar

Se leyó el intento conservado `isolation-814e07fc58c842529f5151143cddff27`. Clean había finalizado OK. En EICAR, ClamAV llegó a cargar las bases y luego registró `Can't open file ...: 32` y `Permission denied`; al inspeccionarlo, el archivo ya no existía. No era una detección EICAR ni evidencia de una base corrupta. La causa comprobable desde el scanner es **la imposibilidad de abrir su entrada**, que explica el exit 2 en vez de 1.

La eliminación por Kaspersky fue informada por el usuario/TI. La nueva conservación y detección tras la exclusión son compatibles con ese diagnóstico; **no se atribuye independientemente el evento a un producto a partir del número 32** ni se leyó su auditoría privada. El fallo aún más antiguo de 768 MiB era distinto: `mpool_malloc(): Can't allocate memory` durante la carga de firmas. Ambos antecedentes permanecen separados.

## Cambios y continuación de la integración

- `Windows-Probar-Capacidad-AV.ps1` conserva cada intento en un JSON único, respalda el resultado anterior y escribe evidencia parcial aunque un escenario falle. Comprueba también los textos positivos/negativos, entrada, orfandad y ACL. Un error de frontera detiene la secuencia.
- `Aislamiento.cs` consulta token/límites antes de ResumeThread y separa las salidas con una lista explícita de dos handles heredables. Conserva límites, creación suspendida, asignación al Job y limpieza. Sigue siendo una **sonda QA**, no un broker del backend.
- Se añadió el candidato `AntivirusDocumentoWindows`: implementa la interfaz Antivirus con JNA ya existente, sin shell, servicio o ejecutable supervisor nuevo. Verifica todo el paquete contra un manifiesto, rutas privadas/reparse, copia acotada del archivo actual, entrada de solo lectura, entorno mínimo, AppContainer/Job, SHA-256, límites/salida/temporales y limpieza. Usa un mutex Windows por identidad para proteger las DACL compartidas aunque existan distintas raíces/JVM. Un trabajo abandonado falla cerrado y requiere conciliación; no se borra a ciegas.
- Se añadieron cuatro tests de guardas y un harness reproducible exclusivo de QA: `Windows-Probar-Antivirus-Java.ps1` / `AntivirusWindowsReal.java`. **El candidato no está conectado a un bean ni habilitado por ArranqueWindows.** No se afirma que ya esté calificado para originales ciudadanos.

El primer ensayo del candidato Java fue bloqueado antes de crear un scanner: volvió a faltar el margen de 4 GiB. Posteriormente se observaron aproximadamente 1–2 GiB libres. Se conservó el bloqueo, sin detener aplicaciones para liberar memoria y sin sustituirlo por un antivirus simulado. La prueba exitosa C# no se presenta como una prueba exitosa de este adaptador Java diferente.

Las pruebas nuevas verifican manifiesto completo, alteraciones, contenido extra, raíces anidadas y conservación del estado pendiente. No son pruebas de explotación del scanner ni completan los ocho controles de la cadena. La regresión de custodia demuestra el núcleo por separado; el inspector/renderizador Windows, recuperación integral y cinco pruebas documentales reales siguen pendientes.

## Seguridad, conservación y resultados de regresión

Consultar los resultados del corte final y las rutas en [RESULTADOS_CLAMAV_WINDOWS_2026-10-26.json](RESULTADOS_CLAMAV_WINDOWS_2026-10-26.json) y el [checkpoint](CHECKPOINT_CLAMAV_WINDOWS_2026-10-26.md). No sumar pruebas focalizadas y baterías que incluyen los mismos métodos.

- Regresión nativa H2 y MySQL QA: **223 seleccionadas,218 correctas,0 fallos/errores,5 omitidas por motor**. La comprobación final focalizada del candidato pasó sus4guardas, incluida la ruta que adquiere/libera el mutex nativo antes de rechazar un paquete alterado. No se está presentando esa guarda como ejecución de clamscan.
- Se repitieron los6canarios con la instrumentación actualizada: correctos; raíz `isolation-cf80cd9aa99447ceb4cc4261636a5db0`. Son sondas QA, no una transferencia automática de calificación a la implementación Java ni los ocho controles de los parsers.
- Fingerprint histórico: **59tablas y504filas**, recuentos y huellas íntegramente iguales a la importación. Custodia: **24objetos/16.869.263bytes**, todos los SHA/tamaños correctos;1.931objetos ACL auditados y0ALLOW inesperados. Una lectura de ACL durante la limpieza concurrente de JUnit encontró un temporal que ya no existía; se descartó y se repitió la auditoría una vez terminadas las pruebas.
- Última guarda Java: **2.211.160.064bytes disponibles frente a4GiB**, estado BLOQUEADO_RECURSOS, sin iniciar scanner. Evidencia `antivirus-java-70f848e5f19e413bb4a68348ddc56d5a.json`.
- Revisión acotada de cinco logs/resultados propios contra las dos contraseñas QA, en memoria: **0exposiciones**. No se leyó el secreto Graph. No hay scanner/supervisor propio ni listener QA al terminar.

No se modificaron V1–V12, frontend, credenciales, esquema histórico, originales, COM, firmas, ACK, Graph histórico ni Formulario Público. Las regresiones MySQL usan exclusivamente el QA descartable con TLS VERIFY_IDENTITY y sus dos identidades previas. No hubo correo real, pull público, proveedor gubernamental, WSL, cutover, tarea/servicio permanente, firewall o puerto nuevo. El JAR previo de runtime se conserva; no se compiló ni activó uno nuevo como runtime completo.

El baseline frontend de 42/lint/build y los E2E preliminares 59/45 pertenecen al corte anterior; **no son una nueva validación documental**. Permanecen cinco omisiones documentales y las tres exclusiones históricas de estrés, sin nuevas exclusiones para ocultar fallos.

Referencias primarias consultadas: [aislamiento AppContainer](https://learn.microsoft.com/en-us/windows/win32/secauthz/appcontainer-isolation), [lanzamiento AppContainer](https://learn.microsoft.com/en-us/windows/win32/secauthz/implementing-an-appcontainer), [códigos del scanner en la fuente de ClamAV](https://github.com/Cisco-Talos/clamav/blob/clamav-1.4.6/docs/man/clamscan.1.in). El inspector Linux de referencia se leyó por GitHub en `bc6489cd75c3dff9ff8462b8d0275d52f1045f29`, blob `13c058833837aa02c3462d734b44cb45eccc90ef`; no se ejecutó ni se modificó el repositorio público o WSL.
