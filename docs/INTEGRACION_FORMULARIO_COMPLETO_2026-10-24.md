# Integración institucional del Formulario Público completo — 24/10/2026

La recepción institucional de documentos 1.0/1.1 está integrada con conservación privada de originales, procedencia íntegra, COM único y ACK verificable después del COMMIT. La verificación se realizó en laboratorio; no habilita producción.

## Estado y alcance

Repositorio `sistema-de-requerimientos`, rama `integracion-formulario-completo-multimedia-2026`, base `86cfe31`; implementación confirmada en `1c74688`. Se inspeccionaron rama, status, diff, archivos nuevos e historial y se continuaron las implementaciones en curso sin reset ni reconstrucción. Se conservaron las integraciones y el núcleo existentes.

La referencia publicada de `formulario-web` se comprobó mediante `ls-remote`: `a84d2e718a824e01cc8bd8cc78fba5e918d483d3`. Se leyeron las seis referencias obligatorias de cierre, baseline, custodia, contrato multimedia, visor y ADR. Ese repositorio quedó limpio y sin modificaciones de código o configuración privada. Los checkpoints antiguos RUN/SRCeI no se usaron como baseline.

## Implementación

- Consumidor HTTPS independiente con negociación explícita `1.0,1.1`; el consumidor V3 histórico y sus fixtures se conservan.
- Verificación de estructura, canonicalización, SHA-256, Ed25519 y vínculo entre comprobante y documento. Se conserva el JSON íntegro, sin truncamiento y sin elevar declaraciones o SRCeI a autenticación.
- Streaming de originales a almacenamiento privado con integridad, temporales, fsync, publicación atómica y bloqueo de escritor. MySQL guarda metadatos y relaciones, no archivos multimedia grandes en heap.
- Transacción institucional única para Comunicación, COM, documento, metadatos y comprobante firmado. Tras COMMIT se recupera el comprobante conservado y se verifican originales antes del ACK. La conciliación reutiliza el registro ante respuestas perdidas o reservas vencidas.
- V9 nueva para H2/MySQL: cuerpo ampliado, discriminador de documento, originales y auditoría de solicitudes. V1–V8 no se alteraron.
- Detalle de Comunicación y antecedentes con respuestas, personas, instituciones, procedencias y documentos del ingreso. Etiquetas legibles y conservación de campos futuros. Descarga autorizada, audio/video 1.1, Range y fallback seguro.
- Separación `INGRESO_CIUDADANO` / `GESTION_INSTITUCIONAL` determinada por servidor; segunda categoría preparada sin implementar carga funcionaria. Vínculos a Casos/Requerimientos referencian los mismos originales.
- No se cambiaron clasificación, distribución, admisibilidad, complejidad por Caso, derivaciones, cierres, reaperturas ni sus decisiones. Recibir no abre Casos/Requerimientos.

Contrato del receptor: [consumidor-formulario-completo-1.1.md](../contratos/consumidor-formulario-completo-1.1.md).

## Pruebas comprobadas

| Evidencia | Resultado |
|---|---|
| Regresión backend H2 | 118 pruebas, sin fallos ni omisiones |
| Regresión backend MySQL 8.4 | 118 pruebas, sin fallos ni omisiones |
| Unidad nueva completa/documentos/HTTP | 33 pruebas incluidas en la regresión |
| Frontend | 18 pruebas correctas; lint y build correctos |
| Transferencia controlada | HTTP de 12 MiB con buffer acotado; tamaño/SHA correctos |
| Contratos | Adultos CU simulados/declarados, NNA con/sin RUN, documentos 1.0/1.1, cuerpos >64 KiB y campos nulos/futuros |
| Fallos controlados | Firma/contenido/archivo alterados, transferencia interrumpida/timeout, fallo SQL tras publicación de archivo, duplicados, concurrencia de dos receptores, ACK perdido, reserva vencida y reinicio |
| Autorización HTTP | Sin sesión/HTTP, otra unidad, documento ajeno, contexto de antecedente/Caso, logout, descarga, HEAD y Range |
| E2E de aplicaciones reales | Adulto declarado 1.0 sin adjuntos y NNA sin RUN 1.1 con cinco originales; tres circuitos correctos |

Se excluyeron explícitamente tres métodos históricos de concurrencia/carga: `vectorInteroperableYDuplicadosConcurrentesTienenUnaConfirmacionEstable`, `doscientosFoliosDistintosBajoConcurrenciaYFormatoHistoricoEstable`, `consumidorParaleloAcotadoConservaUnicidadYConfirmaCadaRegistro`. No son tests omitidos por condición: no se seleccionaron para respetar la restricción de carga. Se mantuvieron las demás regresiones. No hubo estrés ni consultas a proveedores gubernamentales.

Avisos no bloqueantes existentes: Flyway informa que H2 2.4 es posterior a su versión comprobada, Mockito advierte sobre adjuntar agentes en JDK futuro y una prueba frontend anterior emite aviso React `act`. Ninguno produjo fallo.

Las pruebas de red con `HttpServer` son dobles controlados y se distinguen del E2E real descrito a continuación. CU se probó exclusivamente con datos/firmas sintéticos; no se ejecutó autenticación gubernamental real.

## Laboratorio real

Se reutilizaron MySQL/Docker/HTTPS existentes. Se crearon únicamente los esquemas institucionales aislados `robust_inst_test` y `robust_inst_formulario_lab`, usuarios separados de migración/aplicación y una custodia nueva fuera de Git. La instancia pública multimedia existente en HTTPS 19443 mantuvo CU y PISEE deshabilitados y no se alteró su configuración.

La instancia institucional nueva escucha exclusivamente en HTTPS loopback 20443 y usa cookie propia `SIGERINTEG2026`; recepción automática desactivada. Los pull fueron operaciones técnicas explícitas y acotadas. Se reutilizaron copias privadas de la CA/certificado y de la clave institucional sintética ya reconocida por el origen, sin modificar sus confianzas.

Se recuperó primero una Comunicación NNA sintética pendiente 1.1 con un original y reserva vencida, comprobados sus marcadores de laboratorio. Después se ejecutaron tres circuitos de navegador con adulto declarado y NNA sin RUN: captura, revisión, envío, idempotencia ciudadana, recuperación institucional, conservación, ACK firmado, consulta funcionaria, integridad de originales descargados, audio/video nativos, Range y permisos negativos. TLS permaneció validado.

La comprobación SQL/filesystem encontró **siete Comunicaciones, siete folios únicos, dieciséis originales íntegros, cero ACK pendientes, cero Casos y cero Requerimientos**. El documento institucional coincide con el documento de origen y el comprobante persistido coincide con el ACK público. Flyway validó las nueve migraciones sin fallos. El reinicio controlado de la instancia institucional conservó los tres primeros registros y seis originales; el segundo agregó dos registros sin duplicar los previos y la regresión final del artefacto de cierre agregó otros dos.

Se inspeccionaron capturas de escritorio/móvil; no hubo desborde de página. La tabla de procedencia dispone de desplazamiento interno. Los originales PDF/Office no se activan dentro de la página.

Evidencias locales (fuera de Git): `.local/evidencias/integracion-completa/resultado.json`, `resultado-primer-circuito.json`, `resultado-segundo-circuito.json`, `custodia-mysql.json`, `custodia-antes-reinicio.json` y capturas PNG. Logs de baterías: `.test-regresion-h2.log`, `.test-regresion-mysql.log`, `.test-completo-mysql.log`, `.package-final.log`. No contienen credenciales ni respuestas registrales reales.

## Reproducibilidad

`Probar-Formulario-Completo.mjs` requiere `QA_PLAYWRIGHT_PACKAGE` apuntando a un package.json con Playwright disponible, `QA_MEDIA_FIXTURES` a archivos exclusivamente sintéticos existentes y `INSTITUCIONAL_LAB_PRIVATE` al directorio privado nuevo. En Node se usa `NODE_EXTRA_CA_CERTS` con su CA de laboratorio; Edge utiliza el almacén de confianza local. Cada ejecución genera dos Comunicaciones sintéticas nuevas y conserva su evidencia; no es una prueba de carga. El script impone hostname loopback y puertos 19443/20443, bloquea otros orígenes del navegador y nunca abre ClaveÚnica.

`Institucional-Completo-Linux.py` recibe directorio privado, `--jar` para copiar un artefacto compilado, `--migrar` para Flyway con usuario separado y `--detener` para SIGTERM del PID verificado. Rechaza sobrescribir un JAR activo. Las configuraciones y secretos residen en `/home/nexbyteproveedor/.local/share/siger/institucional-completo-lab`, con permisos privados.

La selección de regresión acotada es:

```text
mvn -B -ntp '-Dtest=*,!RecepcionInstitucionalJdbcTest#vectorInteroperableYDuplicadosConcurrentesTienenUnaConfirmacionEstable+doscientosFoliosDistintosBajoConcurrenciaYFormatoHistoricoEstable+consumidorParaleloAcotadoConservaUnicidadYConfirmaCadaRegistro' test
```

Para MySQL, proporcionar `ROBUST_DB_URL`, `ROBUST_DB_USER`, `ROBUST_DB_PASSWORD`, `ROBUST_DB_MIGRATION_USER`, `ROBUST_DB_MIGRATION_PASSWORD` en el proceso desde el almacén privado, nunca como argumentos ni en informes. Las pruebas rechazan esquemas distintos de `robust_inst_test`. El laboratorio integrado usa otro esquema y no lo limpia la batería.

## Límites y siguiente unidad

No se declara producción lista. La durabilidad comprobada es local, con filesystem privado, fsync y MySQL; backups externos, retención, capacidad, recuperación de desastre y alta disponibilidad requieren la campaña conjunta posterior.

Esta unidad ofrece vista nativa de audio/video 1.1. Imágenes, PDF, Office y medios históricos 1.0 tienen descarga autorizada. Queda para ampliación del visor la generación aislada de representaciones PNG con límites y auditoría, sin ejecutores Office ni conversores inseguros.

Quedan los documentos de gestión funcionaria y su ciclo documental; el modelo reserva su origen y autor, pero no incorpora cargas ni reconstruye actuaciones/bitácoras. Los temporales u originales sin vínculo tras un crash no se borran automáticamente; requieren revisión operacional y política de retención futura.

La auditoría añadida registra solicitudes autorizadas de originales/representaciones y rangos inválidos, no acredita que el navegador completó la lectura. La auditoría transversal de accesos, denegaciones, tareas y notificaciones sigue pendiente del levantamiento.

TI/accesos, jefaturas equivalentes por cargo, ingreso manual por cualquier funcionario, subrogancias y equipos por Caso permanecen como puntos de extensión en provisión de identidad, catálogo y autorización por recurso. No se inventaron aprobadores, plazos o facultades ni se fijó como definitiva la restricción provisional del responsable/rol de laboratorio. Esas definiciones no suspendieron esta integración.

No se hizo push, no se intervino Windows/hardware, no se modificaron servicios operativos anteriores ni código/configuración privada del Formulario Público.
## Artefacto de cierre

Implementación `1c74688`, corrección de errores/verificador `ec98dbf`. JAR de laboratorio verificado contra la copia activa fuera de target: SHA-256 `52b01ec2dd1895bc056bf49423a2af020357c49a26fb8c0178c17188d8382aea`. Resultados y huellas V9 en [RESULTADOS_FORMULARIO_COMPLETO_2026-10-24.json](RESULTADOS_FORMULARIO_COMPLETO_2026-10-24.json).

## Corrección final y manejo de la credencial local

Se añadieron errores técnicos sanitizados para documentos JSON malformados, tanto al recibir como al leer custodia: no se propagan fragmentos ciudadanos en excepciones HTTP. Una prueba específica lleva el total nuevo a 33 y la regresión a 118 por motor.

Un comprobador auxiliar se ejecutó antes de que terminara el arranque y Playwright imprimió el token técnico exclusivo de la instancia institucional nueva en su traza. Se detuvo esa instancia, se rotó `operacion.token` y se reinició; la credencial mostrada quedó revocada. No se afectaron el token del origen público, las claves de firma, los comprobantes ni las configuraciones anteriores. No se reproduce el valor en este informe. Se corrigió el verificador para omitir trazas/cabeceras sensibles y el lanzador ahora espera disponibilidad HTTPS antes de anunciar que está listo.

El artefacto final se volvió a comprobar en escritorio/móvil mediante consulta autorizada, etiquetas legibles, originales disponibles y recuperación vacía sin nuevos registros. Evidencia: `consulta-artefacto-final.json`. La revisión del contenido preparado para Git no encontró valores de configuración privados ni claves PEM.
