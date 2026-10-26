# Procesamiento documental: dictamen de ingeniería U4.5

Corte 26/10/2026. **Decisión expresa del usuario: mantener100% nativo Windows. La alternativa híbrida fue descartada.** SIGER QA arrancó nativamente en Windows; su procesamiento documental continúa cerrado. No se declara producción ni cierre de U4.5. Se conserva abajo la evaluación presentada para trazabilidad; no autoriza ni implementa una dependencia Linux.

## Evidencia nueva

La sonda anterior solo comprobaba disponibilidad de APIs. Ahora seis procesos canario reales acreditan, dentro del perfil probado: lectura/escritura externas privadas denegadas, creación de un segundo proceso denegada, memoria limitada, terminación por tiempo de CPU, timeout y crash sin hijo superviviente, y reapertura posterior. Se comprueba que el token es AppContainer y carece de capacidades. Un listener TCP loopback acepta al proceso confiable; el hijo no consigue conectarse en el plazo acotado y el listener no recibe esa conexión. Es una prueba parcial de red, no una certificación de todos los protocolos/destinos.

Se usan APIs existentes de Windows, .NET Framework/csc ya instalados y Jobs, sin driver, servicio, firewall o elevación nuevos. El código está en `scripts/qa/windows-isolation`; **es una sonda QA, no un broker institucional**. No acepta documentos del ciudadano ni está enlazado al backend.

ClamAV 1.4.6 LTS portable se obtuvo del release oficial Cisco-Talos en GitHub. El conector permitió revisar sus metadatos; la descarga binaria se hizo por HTTPS porque ese conector no descarga binarios. SHA-256 del ZIP: `57b6fd1d60cd87bafe800f97407ecdef0576d36b3900b8b7abcfbbabe88295fd`, coincidente con el digest publicado. Se instalaron solo los ejecutables necesarios, DLL y licencias bajo C:\req\bin\clamav-1.4.6, sin PATH global ni servicio. FreshClam, en una ejecución manual, descargó y comprobó main 63, daily 28165 y bytecode 339 en C:\req\storage\antivirus-defs. Un HEAD genérico al CDN había recibido403; **no era un bloqueo del actualizador oficial**, que terminó0.

La ejecución real de `clamscan` en AppContainer con el conjunto completo de bases terminó2. El diagnóstico acotado identifica `mpool_malloc(): Can't allocate memory` al cargar main.mdb; el texto final `Malformed database` es consecuencia de esa falta de memoria, no evidencia de corrupción de las bases. El Job estaba limitado a768 MiB. No se retiraron firmas, no se sustituyó el antivirus por una detección EICAR fabricada y no se aceptó un resultado incompleto como limpio. Otra ejecución real con base inexistente también terminó2 y restauró sus ACL. No se llegó a la prueba de detección EICAR.

El equipo tiene16 GiB visibles, pero durante estas comprobaciones se observaron aproximadamente1,3–2,1 GiB libres. No se amplió la memoria del worker sin margen ni se detuvieron servicios/WSL para forzar la prueba. El proveedor recomienda del orden de3–4 GiB para su conjunto estándar; **no se midió aquí la memoria mínima que permitiría completar la carga**, ni se demuestra que asignar una cifra concreta baste para cerrar toda la unidad. La portabilidad del inspector/renderizador sigue siendo un trabajo adicional.

Defender está instalado pero detenido, con motor/firmas no operativos según su consulta de estado. No se activó ni se cambiaron sus políticas. Windows Sandbox figura oficialmente como característica de Windows 10/11; no se presupone como solución soportada en Server 2022. Contenedores/Hyper-V o políticas nuevas del anfitrión tampoco se habilitaron.

La consulta adicional encontró un proveedor AMSI de Kaspersky registrado, versión21.26.4.275 y firma Authenticode válida. `AmsiInitialize` fuera del contenedor devuelveS_OK; esa sonda solo inicializa, **no analiza contenido** con permisos del backend. Dentro del AppContainer, con y sin inicialización COM explícita, devuelve0x80070103 antes de analizar el buffer. No se modificaron ACL/registros/políticas del producto ni se copió su DLL. Por tanto, su mera presencia no permite declarar resuelto el antivirus del worker.

Se preparó `scripts/qa/Windows-Probar-Capacidad-AV.ps1`: exige al menos 4 GiB físicos libres antes de un ensayo con Job limitado a1,5 GiB, CPU 45 s, un proceso y plazo 90 s. Es una guarda conservadora del ensayo, **no un nuevo mínimo institucional ni una memoria funcional ya probada**. El preflight detectó 2.658.258.944 bytes libres y registró BLOQUEADO_RECURSOS, scannerExecuted=false. El usuario eligió expresamente **conservar el bloqueo y el checkpoint por ahora**; no ejecutar la ampliación ni liberar memoria deteniendo otros servicios/WSL.

## Controles: estado preciso

| Control requerido | Lo comprobado / pendiente |
|---|---|
| Lectura externa | Canario privado rechazado por un proceso AppContainer real. Falta calificar cada parser/DLL y su política final. |
| Escritura externa | Canario externo no creado/alterado. El trabajo propio sí es escribible. Falta el perfil definitivo de herramientas y temporales. |
| Red | Control positivo del listener y ausencia de conexión TCP del hijo; timeout acotado. Faltan otras familias/protocolos y verificación final del paquete. |
| Procesos auxiliares | Segundo proceso rechazado por Job de un proceso. El inspector Linux actual necesita orquestación de herramientas separada. |
| Recursos | Memoria128 MiB y CPU 2 s del canario efectivas; límite768 MiB del scanner efectivo. Falta presupuesto funcional de AV, salida/disco/handles y perfil definitivo. |
| Interrupción | Timeout, exit37, ausencia del PID hijo y siguiente proceso exitoso. Las ACL/perfil se restauran; falta recuperación integral de workers reales. |
| AV/inspector fallan cerrados | ClamAV real termina2 ante falta de memoria/base. No está integrado al camino de carga; no se cuenta como E2E documental aprobado. |
| Contenido inseguro no se publica | Cubierto con dobles en regresión; detección/control reales y los cinco tests documentales continúan pendientes. |

Una auditoría descubrió que restaurar una DACL mediante el objeto .NET leído sin marcarlo como modificado no persistía la retirada de ACE temporales. Se corrigió rehidratando explícitamente el descriptor y probando la restauración. Se retiraron únicamente las ACE propias del paquete AV y sus bases; no habían alcanzado config o los originales históricos. La auditoría final del almacén/config/scratch y la del paquete AV terminaron con cero ACE inesperadas. Esta corrección y el episodio quedan registrados; no constituyen una certificación del prototipo para producción.

## Alternativas revisadas

| Opción | Complejidad y operación | Decisión |
|---|---|---|
| Windows nativo: lanzador acotado AppContainer/Jobs y parsers fijos | Sin VM nueva, pero requiere portar el inspector multiproceso, distribuir/verificar parsers, probar DLL/ACL/herencia/recuperación y mantener código propio de seguridad. Necesita presupuesto real de AV y los ocho controles completos. | Técnicamente viable por las sondas, **no terminada**. No se debe considerar un simple cambio de comandos. |
| Defender + parsers aislados | Podría reutilizar un scanner del sistema, pero actualmente no está operativo. Activarlo afectaría seguridad/servicios compartidos y no resuelve por sí solo el inspector. | No modificar el host por conveniencia; requiere evaluación institucional independiente. |
| Windows con worker Linux aislado dedicado | Reutiliza parsers, políticas y pruebas existentes, reduciendo el portado y el código nuevo de seguridad. Añade una plataforma operativa, red privada autenticada y su mantenimiento. | Se propuso para evaluación; **descartada por decisión del usuario. No implementar.** |

No se afirma que Windows sea incapaz de aislar procesos. El bloqueo actual combina capacidad AV insuficiente dentro del presupuesto probado y una cadena documental todavía no portada/calificada. Aumentar RAM no convierte automáticamente la sonda en un producto mantenible; tampoco una VM Linux acredita por sí sola los ocho controles.

## Propuesta híbrida presentada y descartada, no ejecutada

- Backend Spring/Java 25, frontend, identidades, MySQL/TLS, COM/firmas/ACK, outbox y custodia definitiva permanecen en la VM Windows Server 2022.
- Worker en una VM Linux dedicada o servicio equivalente aprobado, separado del WSL original y del Formulario Público. No compartir directorios del backend, secretos, MySQL o la custodia mutable.
- Transferir únicamente el archivo de cada trabajo por streaming, mediante un endpoint privado con TLS mutuo, límites de tamaño/plazo/concurrencia y correlación. El worker tiene solo temporales por trabajo, sin conservar originales institucionales.
- Reutilizar AV y parsers existentes con sandbox Landlock/seccomp y límites, adaptando únicamente la frontera de transporte. Probar de nuevo los ocho controles, rechazo, fallos de red y hashes de entrada/salida; no heredar una certificación de otra instancia.
- Windows conserva la decisión y las verificaciones de publicación, integridad y transacción. No se concede acceso documental por tener un UUID o una respuesta del worker. Un fallo remoto conserva el rechazo seguro y no adelanta ACK.
- Antes de implementar: aprobar dependencia híbrida, ubicación/recursos, responsables de actualización, conectividad privada y certificados. Sin abrir puertos/firewall ni crear VM/servicio en esta ejecución.

Riesgos añadidos: disponibilidad/red, gestión de certificados, residuos temporales, actualización del worker y otro componente que operar. La separación evita exponerle secretos/custodia, pero requiere validación efectiva. **Esta opción no puede llamarse completamente nativa y fue descartada.** Se mantiene el criterio nativo: al retomar habrá que completar el lanzador y su cadena de herramientas con presupuesto de memoria comprobado. No se propone cambiar nuevamente a Linux como continuación automática.

Referencias primarias: [AppContainer](https://learn.microsoft.com/en-us/windows/win32/secauthz/implementing-an-appcontainer), [Jobs](https://learn.microsoft.com/en-us/windows/win32/procthread/job-objects), [límites CPU](https://learn.microsoft.com/en-us/windows/win32/api/winnt/ns-winnt-jobobject_basic_limit_information), [ClamAV requisitos](https://docs.clamav.net/Introduction.html), [release oficial](https://github.com/Cisco-Talos/clamav/releases/tag/clamav-1.4.6), [Windows Sandbox](https://learn.microsoft.com/en-us/windows/security/application-security/application-isolation/windows-sandbox/).
