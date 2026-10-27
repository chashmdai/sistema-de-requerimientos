# U5 · Validación técnica Windows acreditada

**27/10/2026: validación técnica pendiente por RAM ACREDITADA en QA Windows nativo. U5 continúa FUNCIONALMENTE PARCIAL.** Responder, canalizar y escalar Solicitudes permanecen deshabilitados, pendientes de definición institucional. No se habilita producción ni se publica U5.

## Candidato y alcance

Se retomaron `a507761` y el código `7b318f2`, sin reconstruir módulos. Rama `feature/u5-solicitudes-tareas-independientes`; `main` permanece en `b322c9b905b533605fb53c8d706dbc144ed074a3`, sin modificaciones ni push.

Se utilizó el mismo artefacto del checkpoint: `C:\req\bin\siger-7a50166482066535.jar`, SHA-256 `7a5016648206653517a1625d89bcde241fa4af1e970ef8fb99387cfb52593402`. Las **235 clases** del JAR coinciden por SHA-256 con las clases del candidato probado. No hubo cambios en backend, frontend institucional, migraciones, límites, credenciales, herramientas o políticas del host.

El único ajuste del harness, commit local `98fd092`, guarda `u5-custodia-ciudadana.json` con `preview=APROBADA` **después** de comprobar la visualización positiva. Mantiene la evidencia parcial y la salida de error ante fallo. No cambia controles de aprobación ni comportamiento del producto.

Java25 explícito, MySQL8.0.46, JDBC `VERIFY_IDENTITY`/TLS1.3, secretos DPAPI y HTTPS localhost con certificado confiable. Migrador y aplicación separados; runtime exclusivamente DML, Flyway deshabilitado en la aplicación. Se reutilizó V13 de QA, sin migraciones nuevas. El histórico institucional conserva V1–V12.

## Resultados definitivos

| Prueba nativa Windows | Resultado |
|---|---|
| Antivirus Java real | **3/3**: Clean exit0, EICAR exit1, Unavailable exit2; rechazo seguro del error |
| JUnit `controlAntivirusRealSeRechaza` | **1/1**, cero omisiones |
| Calificación aislada del lanzador | **9/9**: frontera, memoria, CPU, crash, salida, timeout, mutex, interrupción y aborto de JVM/recuperación |
| H2 completo | **245/245**, cero errores, fallos y omitidas |
| MySQL QA completo | **245/245**, cero errores, fallos y omitidas |
| Cinco documentales reales anteriormente pendientes | **5/5 en cada motor**, incluidos PDF, imagen y EICAR que antes bloquearon por RAM |
| Cuatro pruebas adicionales de inspectores/formatos Windows | **4/4 en cada motor** |
| Frontend | **49/49**, lint y build correctos |
| Edge institucional con documental activo | **127/127**, operación real HTTPS/MySQL/NTFS/AV/inspector/renderizador |
| Edge institucional después de reinicio | **110/110**, documental activo, persistencia, descarga e idempotencia |
| Edge U5 documental ciudadano | **57/57**, previsualización positiva HTTP200 sin descarga manual; ocho originales íntegros y negativa TI |
| Edge U5 | **86/86**, tareas, preparación, avisos, autorizaciones y decisiones finales bloqueadas |
| Edge U5 después de reinicio | **26/26** |
| Edge territorial | **34/34** |

Se conservaron exclusivamente las tres exclusiones históricas de estrés. No se añadieron omisiones, exclusiones ni dobles para conseguir resultados correctos. Los cinco métodos documentales se ejecutaron con ClamAV, qpdf, Poppler, FFmpeg y el inspector Office/TXT nativos existentes.

El E2E documental incluyó PDF de tres páginas, zoom, navegación, vista móvil, imagen derivada, descarga con SHA-256/no-store y acceso autorizado. EICAR, PDF activo, extensión falsa y referencias Office externas se rechazaron sin publicar documentos. La indisponibilidad controlada de AV/inspector devolvió503 sin publicación; se retiraron los marcadores QA en `finally`.

Las operaciones finales de Solicitud fueron verificadas como bloqueadas tanto en UI como por API409, incluso para la cuenta multirrol. La acreditación técnica no habilita esas facultades.

## Memoria e intentos conservados

Al comenzar había aproximadamente3,97GiB. El primer control JUnit alcanzó la guarda interna con **4.205.371.392 bytes disponibles**, menos que **4.294.967.296**: quedó bloqueado sin ejecutar el scanner. Posteriormente la disponibilidad aumentó sin intervención nuestra sobre servicios o procesos ajenos y permitió completar los ensayos.

El primer intento de calificación coincidió con el harness AV todavía activo y fue rechazado por el mutex («Antivirus ocupado en otra instancia»). No se contabilizó como acreditación. Repetido de forma secuencial: nueve escenarios correctos. Se conservan ambos intentos, sin atribuirlos a Kaspersky ni ocultarlos.

El AV mantuvo el mínimo de4GiB, Job de1,5GiB, proceso único, capacidades0, CPU45s y reloj90s. Los picos privados reales fueron **1.037.422.592 bytes** en Clean y **1.037.418.496** en EICAR. Job asociado durante la creación, AppContainer y limpieza completa comprobados. Ningún límite se redujo ni se desactivó protección del host.

## Ocho controles documentales

| Control | Evidencia actual y alcance | Resultado |
|---|---|---|
| Lectura externa | La sonda bajo el mismo lanzador no lee un señuelo privado externo; los workers reales reciben su copia autorizada y token verificado. | Acreditado |
| Escritura externa | Denegadas escritura fuera del trabajo, modificación de entrada y ampliación de la DACL del señuelo. ACL restauradas al terminar. | Acreditado |
| Red y loopback | TCP IPv4/IPv6, UDP loopback y conexión externa denegados al hijo; controles positivos previos desde el padre. Sin cambios de firewall. | Acreditado |
| Procesos auxiliares | Segundo proceso denegado; Job de un proceso adjunto antes de reanudar. Operaciones del lanzador cerradas, sin shell. | Acreditado |
| Recursos | Memoria excesiva rechazada, CPU terminada por Job y salida excesiva rechazada; límites efectivos consultados. No se afirma cuota NTFS instantánea de disco. | Acreditado |
| Timeout/crash/limpieza | Timeout, exit37, interrupción, mutex y aborto de JVM recuperados; journal conciliado y sin procesos/trabajos propios pendientes. | Acreditado |
| Fallo seguro | AV real exit2 y AV/inspector indisponibles en HTTPS producen rechazo/error, sin publicación. | Acreditado |
| Contenido inseguro y conservación | Rechazos reales sin nuevos originales; pruebas de contrato/persistencia preservan integridad, COM único y ausencia de ACK prematuro. | Acreditado |

Se distinguen **sonda sintética del aislamiento**, **workers reales** y **pipeline institucional real de laboratorio**. El canario por sí solo no acredita parsers; aquí lo complementan las nueve pruebas documentales reales y el E2E completo. Los parsers mantienen sus perfiles de512MiB; no se ejecutan bajo el token del backend.

## Custodia, datos y transportes

Antes y después: **59 tablas/504 filas históricas idénticas por recuento y huella**, **24 originales/16.869.263 bytes idénticos**, cero permisos ACL inesperados. No se alteraron el esquema histórico, sus claves, folios, comprobantes, ACK ni los cuatro envíos Graph confirmados históricamente.

La regresión MySQL utiliza exclusivamente el QA descartable. Se archivó su estado previo y las evidencias anteriores; después se prepararon nuevas cuentas/ingresos sintéticos mediante los helpers existentes, sin reemplazar contraseñas privadas ni restaurar datos históricos. Los baselines QA por UUID del26/10 quedan como evidencia de aquella campaña, no como referencias de la nueva base.

Estado final QA: ocho COM únicos, un Requerimiento/Caso, siete preparaciones, cuatro tareas, diez originales documentales (ocho ciudadanos y dos de gestión), cinco representaciones y ocho páginas. Veintiséis avisos, tres intentos de correo **simulados**, cero aceptados Graph, cero COM duplicados y cero ACK pendientes. Los dos orígenes documentales se conservan separados.

Los cuatro ingresos públicos de esta campaña cubren adulto CU **simulado**, adulto declarado y NNA con/sin RUN, documentos1.0/1.1. El productor y la aceptación del ACK son **simulados en proceso** con firmas y originales sintéticos. Esto no es una nueva integración real con el Formulario Público ni con proveedores gubernamentales. Graph real, recuperación pública y producción permanecieron desactivados.

Se verificó reinicio de JVM, no reinicio de Windows ni corte eléctrico. No se certifica hardware ni preparación productiva. No se modificaron Kaspersky, WSL, políticas Windows, MySQL80 global, otros servicios o el Formulario Público.

## Evidencias reproducibles

Directorio privado de campaña: `C:\req\evidencias\u5-validacion-2026-10-27-6f6bc769756245548d2bf0bebb5c8556`.

- `h2.json/.log`, `mysql.json/.log`, `frontend-test.log`, `frontend-lint.log`, `frontend-build.log`, `resumen.json` y `todas-clases-candidato.json`.
- `av-java.json`, `av-preflight.json`, `av-junit.json`, `aislamiento.json` y `workers-evidencias.json`. Salidas completas privadas: `C:\req\testdata\av-java-3a823f6419f74da48b188fcc8671d00c`; sonda definitiva: `C:\req\testdata\calificacion-doc-5afa84ab0fdd4e5196407865a5ffec7f`.
- Intentos iniciales conservados: `C:\req\evidencias\junit-av-real-bf1fc0589ea14c68947264584d0fb860.json` y `C:\req\testdata\calificacion-doc-6df95d6904b6441b96e821509881b69c\qualification.json`.
- Runtime de operación: `C:\req\testdata\runtime-qa-1abbbf44e3674b1ab49bb316e1148350`. Resultados127/57/86, baselines de operación/U5, capturas sintéticas de PDF e imagen y `u5-custodia-ciudadana.json` aprobado.
- Runtime de reinicio: `C:\req\testdata\runtime-qa-ff693f3022fb4575bd4cbeb426e0f455`. Resultados110/26/34. JSON de resultados copiados también a la campaña.
- Custodia sintética vigente: `C:\req\testdata\runtime-qa-b371266c31744ccc96f5f0668fcf7154\archivo`; su `e2e-ingreso.json` y trust público QA se conservan en la misma raíz.
- `historico-antes.txt`, `historico-despues.txt`, `qa-antes-regresion.txt`, `qa-antes-reinicio.txt`, `qa-final.txt`, `jdbc-app.json`, `jdbc-migrator.json` y `parada.json`.

Runtime y Edge propios detenidos; cero workers, listeners QA, trabajos pendientes y secretos efímeros remanentes. Evidencias, ejecutables, credenciales, certificados y originales permanecen fuera de Git.

## Dictamen y siguiente paso

**Queda acreditada la validación técnica que faltaba por RAM. U5 sigue funcionalmente parcial** hasta confirmar e implementar las facultades de responder, canalizar y escalar. No hace falta repetir esta campaña salvo cambios de código/entorno o un fallo nuevo. El siguiente trabajo es resolver esa definición institucional, no reconstruir tareas, custodia o infraestructura.

Ver [checkpoint vigente](CHECKPOINT_U5_2026-10-27.md), [operación](OPERACION_U5_WINDOWS.md) y [reversión no destructiva](REVERSION_U5_WINDOWS.md). No hacer push ni habilitar producción.
