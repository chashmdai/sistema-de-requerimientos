# Cierre de la segunda unidad documental — 24/10/2026

El visor institucional abre imágenes y PDF dentro de SIGER, con páginas, zoom y errores controlados. La gestión funcionaria incorpora originales privados, validación antivirus, referencias a Casos/actuaciones, consulta, descarga e historial por contexto. **Cierre de laboratorio; producción no habilitada.**

Repositorio y rama conservados: `sistema-de-requerimientos`, `integracion-formulario-completo-multimedia-2026`. Base `29428e0`; implementación `2e54edd`. Antes de editar se revisaron Git limpio, historial, checkpoint, informe, V9 y reversión. No se reinició el proyecto.

## Resultados comprobados

| Verificación | Resultado |
|---|---|
| Backend H2 | 142 pruebas correctas, 0 fallos, 0 omitidas |
| Backend MySQL 8.4 | 142 pruebas correctas, 0 fallos, 0 omitidas |
| Frontend | 26 pruebas correctas; lint/build correctos |
| Nuevas pruebas backend | 12 de ciclo/persistencia, 7 HTTP y 5 con workers reales |
| Visor real en Edge | Imagen y PDF ciudadanos; imagen de gestión y PDF de 3 páginas, zoom 200%, escritorio/móvil, sin descarga manual del original |
| Ciclo real | Carga con actuación y autor de sesión, reintento idéntico, referencia a otro Caso, lectura por su profesional y descarga con SHA correcto |
| Seguridad real | 401 sin sesión, 404 otra unidad/otro Caso, 403 sin CSRF, 400 origen manipulado, 422 contenido rechazado |
| Antivirus/inspector reales | Control antivirus, PDF activo y falsa imagen rechazados; Office válido sin ejecución y Office con referencias externas rechazado |
| Persistencia y reinicio | V10 validada, originales y seis derivadas conservados; repetición tras reinicio sin documentos/vínculos nuevos |

Se mantuvo la exclusión explícita de los tres métodos históricos de carga/concurrencia de la unidad anterior: `vectorInteroperableYDuplicadosConcurrentesTienenUnaConfirmacionEstable`, `doscientosFoliosDistintosBajoConcurrenciaYFormatoHistoricoEstable` y `consumidorParaleloAcotadoConservaUnicidadYConfirmaCadaRegistro`. No se ejecutaron pruebas de estrés. Los fallos de scanner, SQL, filesystem y corrupción se ejercitaron con escenarios controlados H2/MySQL; no se detuvo el antivirus compartido para simularlos.

Los cinco tests de workers reales se habilitaron expresamente en ambas baterías mediante variables de proceso. Además se probaron fixtures generados íntegramente en este repositorio. Las pruebas sintéticas de servicios se distinguen del recorrido de navegador con backend, MySQL, almacenamiento y workers reales.

## Integridad del baseline

Una instantánea previa a V10 y una comparación posterior verificaron cuerpo de custodia, comprobante persistido, identificador, número COM, fecha, canal y huellas de **las siete Comunicaciones previas**. Permanecen iguales y con cero ACK pendientes. Se comprobaron también tamaño/SHA de **los dieciséis originales ciudadanos**. Los contratos y fixtures públicos, firmas, COM/ACK y migraciones V1–V9 no se modificaron.

El laboratorio contiene ahora **dos documentos de gestión, dos originales físicos y tres vínculos**: un PDF asociado a una actuación del primer Caso y referenciado al segundo; una imagen del segundo Caso. Hay seis páginas/representaciones PNG verificadas. Los dos Casos y un Requerimiento usados en QA se crearon expresamente mediante los comandos funcionarios existentes sobre una Comunicación sintética previa; la recepción no creó ninguno.

El segundo profesional puede consultar el PDF desde el Caso referenciado y recibe 404 al solicitar el contexto original que no le corresponde. El historial del segundo contexto no expone la creación ni los movimientos del primero. Se conserva autor y fecha originales del documento.

## Implementación y aislamiento

- `documental/AutorizacionDocumental` y `AutorizacionNucleo`: política reutilizable por recurso, delegación en permisos vigentes, sin nueva matriz de facultades.
- `DocumentosGestion`: staging/streaming, SHA, validación real, origen/autor/fecha de servidor, idempotencia, persistencia, referencias sin copia e historial contextual.
- `VisorDocumento`: derivadas separadas, páginas bajo demanda, integridad, caché privada y errores cerrados.
- `PdfAislado`: Poppler existente bajo helper Landlock/seccomp, NPROC=1, entorno sin secretos y solo el archivo actual. Autotest de denegación de archivo externo, socket y fork. No requiere nuevos contenedores.
- `DocumentosIngresoController`: incorpora las vistas de imágenes/PDF preservando acceso por Comunicación/antecedente y las descargas originales. PDF/Office activos no se sirven inline.
- React: visor de páginas, zoom accesible, carga de gestión y referencias, autor/procedencia, integridad e historial. Se reutilizan shell, Casos y actuaciones existentes.
- V10 agrega vínculos, comandos, representaciones, páginas y eventos. La nueva FK compuesta enlaza actuación y Caso sin alterar actuaciones históricas.

La carga de gestión se permite con la facultad/estado vigente de registrar actuaciones. Lectura de documentos e historial sigue la autorización del contexto; no exige que el lector sea el creador del archivo. El adaptador provisional puede evolucionar con equipos, subrogancias y permisos múltiples sin copiar documentos ni introducir autorizaciones del navegador.

No se crearon nuevas bases, volúmenes, servicios o contenedores. Se reutilizaron `robust_inst_formulario_lab`, `robust_inst_test`, el almacén privado, HTTPS 20443 y los sockets existentes del inspector/ClamAV. No se instalaron herramientas: Poppler y prlimit estaban presentes; el helper revisado se obtuvo del contenedor existente y se colocó como herramienta privada. El Formulario Público quedó limpio y su configuración privada se conservó.

## Límites técnicos explícitos

Gestión: un archivo por carga, máximo 25 MiB y solicitud multipart de 26 MiB, con temporales en disco privado. Allowlist de formatos en el contrato. PNG/JPEG/WebP y PDF tienen vista real; PDF admite hasta 100 páginas, 1600 px y 6 MiB por página derivada. Zoom 50–300% sobre representación raster. Documentos activos, cifrados, inválidos o fuera del perfil producen errores controlados; no se anuncian como vistas completas.

Office permanece como descarga validada; no se incorporaron ejecutores ni conversores. El historial muestra los últimos 100 movimientos del contexto y conserva el resto en MySQL. La auditoría registra solicitudes, no una confirmación de lectura humana o de descarga finalizada.

El cliente Windows devolvió EPERM al leer el fixture de control antivirus. Se mantuvieron las protecciones del equipo y esa comprobación se ejecutó por HTTPS desde WSL con el script institucional, obteniendo rechazo real 422. Las pruebas de PDF activo y extensión falsa se ejecutaron desde el cliente de navegador. No se consultaron servicios gubernamentales ni se investigó/intervino hardware o Windows.

Se corrigió una etiqueta de zoom que incluía las opciones dentro de su nombre accesible; el selector ahora tiene nombre explícito. La prueba real de páginas y zoom pasó después de esa corrección.

## Operación reproducible

Configuración privada existente: `/home/nexbyteproveedor/.local/share/siger/institucional-completo-lab/application.properties`. Activación: `institucional.documental.habilitada=true` y rutas privadas a sockets/configuración antivirus/helper. El preparador impone laboratorio loopback, esquema institucional conocido y helper revisado; no copia secretos a Git.

```text
scripts/Preparar-Documental-Linux.py <directorio-privado> --inspector-socket <socket-existente> --antivirus-socket <socket-existente>
scripts/Generar-Fixtures-Documentales.py <directorio-nuevo-de-fixtures>
scripts/Institucional-Completo-Linux.py <directorio-privado> --jar <jar-compilado-inactivo> --migrar
```

El preparador requiere el acceso normal autorizado a Docker para leer artefactos existentes. El backend ejecuta con el usuario sin privilegios; no utiliza Docker ni una API administrativa para renderizar. El JAR activo es una copia privada fuera de target; el lanzador verifica PID y espera HTTPS. Nunca compilar sobre ese JAR.

Para pruebas backend reales, establecer `DOCUMENTAL_REAL_CONFIG` y `DOCUMENTAL_REAL_FIXTURES` con rutas privadas en el proceso. En MySQL usar las cinco variables `ROBUST_DB_*` del laboratorio de pruebas ya existente; no pasarlas como argumentos ni imprimirlas. La batería usa únicamente `robust_inst_test`, distinta de la custodia integrada.

`Probar-Unidad-Documental.mjs` utiliza `QA_PLAYWRIGHT_PACKAGE`, `INSTITUCIONAL_LAB_PRIVATE`, `QA_DOCUMENTAL_FIXTURES` y `NODE_EXTRA_CA_CERTS`. Conserva claves/UUID de prueba en estado privado para reanudar sin duplicados, impone origen local 20443 y bloquea otros orígenes. No llama al Formulario Público. El control antivirus HTTP se ejecuta con `Probar-Antivirus-Documental-Linux.py <directorio-privado>` desde WSL.

Evidencias ignoradas por Git: `.local/evidencias/documental/`, con baseline, estado reanudable, resultado de navegador, control antivirus HTTP, comprobación de custodia y capturas PNG. Logs de pruebas: `.test-documental-h2.log`, `.test-documental-mysql.log`, `.test-fixtures-propias.log`, `.package-documental.log`. No se incorporan credenciales ni documentos privados a Git.

## Continuidad

TI/accesos, cargos equivalentes, ingreso manual universal, equipos, subrogancias, tareas y notificaciones conservan su levantamiento paralelo. No se inventaron facultades, aprobadores o plazos. La auditoría transversal y el hardening productivo quedan para su unidad específica; no bloquearon este ciclo documental.

Permanecen para esa campaña las políticas de capacidad/retención, revisión de huérfanos, backups externos, recuperación de desastre, múltiples nodos y actualización coordinada de herramientas/perfiles. Esta entrega mantiene un escritor del almacén y trabajos acotados. No declara producción lista.

[Contrato documental](../contratos/documental-institucional-v1.md) · [Reversión](REVERSION_DOCUMENTAL.md) · [Checkpoint](CHECKPOINT_DOCUMENTAL_2026-10-24.md).
Artefacto final privado verificado contra el build: SHA-256 `a82b6289d6bc2545991207abc44f8f626de1875ffff4f487f2ffd1337500b85e`. Huellas y resultados agregados: [RESULTADOS_DOCUMENTAL_2026-10-24.json](RESULTADOS_DOCUMENTAL_2026-10-24.json).
