# U4.5 — Checkpoint intermedio NTFS/Graph/QA, 26/10/2026

**Primera meta técnica completada. U4.5 sigue abierta: no hay runtime documental Windows completo ni E2E Windows/Edge certificado. U5 no iniciada.**

Repositorio C:\req\repo, rama migracion-windows-laboratorio-2026. Base preservada b4a41ab/17de7a1; implementación de esta continuación **6d2f6ee** y guarda de unidad local **3e1c2cd**. Un commit posterior conserva estas evidencias/documentación. Sin push.

## Alcance terminado

- Adaptador NTFS implementado y probado; implementación POSIX separada, con su fsync de directorio conservado.
- Archivos privados portables y los seis tests Graph corregidos, sin cambios a OAuth/estados ni transporte real.
- MySQL QA Windows exclusivo, provisionado por administración autorizada, con DML/DDL separados y TLS real. Guardas rechazan el esquema histórico.
- Regresiones nativas H2 y MySQL QA: **213 seleccionadas, 208 correctas, cero fallos/errores y cinco workers omitidos por motor**. Frontend: 42 correctas; lint/build correctos.
- Comparación independiente del histórico: las 59 tablas de negocio y los 24 objetos siguen idénticos. Cuatro intentos/confirmaciones Graph históricos, cero ACK pendientes y cero correos nuevos.

Los documentos y evidencias del corte 25/10 se conservan completos; este informe los complementa, no los sustituye.

## Custodia y durabilidad Windows

`ArchivoInstitucional` conserva su API. `PlataformaArchivos` selecciona POSIX o NTFS, con operaciones explícitas de privacidad, validación, sincronización y publicación. `Win32Archivos` usa JNA 5.19.1, dependencia de proyecto fijada desde Maven Central; no se instalaron herramientas del sistema ni se cambió Java global.

La limitación anterior era de la apertura Java NIO del directorio: **CreateFileW con GENERIC_WRITE y FILE_FLAG_BACKUP_SEMANTICS permite abrir el directorio y FlushFileBuffers devuelve éxito en este NTFS**. Se utiliza esa llamada real, se comprueba cada retorno y cualquier fallo se propaga. No se puso un no-op ni se consideró suficiente `exists`.

No se habilitan privilegios de backup/restore, ni se abren discos o volúmenes. El token operativo comprobado tiene SeChangeNotifyPrivilege y SeIncreaseWorkingSetPrivilege (este último deshabilitado), no SeBackup/SeRestore. BACKUP_SEMANTICS permite el handle de directorio con sus controles de acceso; no es una elevación.

Protocolo:

1. Rechazar UNC/dispositivos/ADS, nombres especiales, finales ambiguos y cualquier reparse point, junction o symlink en la cadena. Canonicalizar rutas Windows únicamente después de comprobarla; Linux conserva el rechazo de discrepancias con realpath.
2. Raíz fuera de Git, unidad local fija (GetDriveType=DRIVE_FIXED), NTFS y DACL privada protegida: usuario del proceso, SYSTEM y Administradores. Directorios objects/tmp privados y un lock de escritor. Los ALLOW efectivos de otras identidades y DACL desconocidas se rechazan. Se vuelve a validar la ubicación privada antes de leer/escribir.
3. Streaming a un temporal privado; tamaño/SHA-256 contra descriptor; force del contenido. El archivo no se publica si falla la integridad.
4. Rename en el mismo volumen mediante MoveFileExW con WRITE_THROUGH, sin COPY_ALLOWED ni REPLACE_EXISTING. No hay fallback a copia/borrado o sustitución de un original.
5. FlushFileBuffers del archivo publicado y de todos sus directorios hasta la raíz, además del padre de raíz. Solo tras completar estas barreras retorna el método que precede la transacción SQL.
6. Un original que existe tras interrupción se vuelve a verificar y sincronizar, incluso sus directorios. Disponibilidad/consulta de custodia también ejecutan la barrera: un flush fallido no permite presentar el original como listo para ACK. SQL/COM/idempotencia y la secuencia COMMIT→comprobante→ACK se conservan.

Las garantías aquí comprobadas son las APIs del sistema operativo y fallos/interrupciones controladas. No se realizó corte eléctrico, BugCheck, reinicio del host ni se certifica el comportamiento de cachés/controladores físicos. Esta limitación no se oculta como equivalencia de hardware. La implementación exige NTFS local y los permisos necesarios; no pretende soportar SMB/FAT/ReFS mediante un fallback silencioso.

Referencias primarias: [CreateFileW, directorios y WRITE_THROUGH](https://learn.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-createfilew), [FlushFileBuffers](https://learn.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-flushfilebuffers), [MoveFileExW](https://learn.microsoft.com/en-us/windows/win32/api/winbase/nf-winbase-movefileexw), [DACL protegida](https://learn.microsoft.com/en-us/windows/win32/secauthz/security-descriptor-string-format).

## Pruebas de almacenamiento y privacidad

Quince casos de ArchivoPlataformaTest, ejecutados sobre Windows:

- Interrupción en cuatro etapas: temporal escrito, contenido forzado, publicado y directorios forzados. Reapertura y reintento conservan un solo original.
- Fallo de barrera después del rename: existe el archivo, pero disponibilidad y reintento siguen fallando hasta que se puede sincronizar; no se sustituye el contenido.
- Proceso Java hijo termina abruptamente con Runtime.halt después de publicar. El padre, otro proceso, recupera los bytes y adquiere el lock liberado por el sistema. No se mata el anfitrión ni procesos ajenos.
- Segundo escritor rechazado; apertura posterior correcta.
- SHA/tamaño inválidos no publican; original alterado no se reemplaza ni se corrige modificando su huella.
- Cinco claves de escape/ADS/nombres inválidos rechazadas.
- Junction de destino y junction de raíz rechazadas antes de escribir o modificar su destino. En el código portable, Linux usa symlinks para esa misma intención; no se ejecutó una batería Linux nueva.

`ArchivoPrivado` mantiene el control 0600 de Linux y usa ACL/SID efectivos en Windows. Los fixtures de Graph protegen directorio y archivo vacío **antes** de escribir el valor sintético. Un ALLOW de lectura para Everyone se rechaza antes de cualquier llamada OAuth. Los seis casos Graph y los ocho de outbox simulado pasan; una reejecución final de esos seis fixtures también fue correcta. Tras la guarda adicional de unidad fija se repitieron 15 de almacenamiento y 6 de Graph: 21 correctas. No se cambió el tratamiento de 202, timeout ambiguo, caché, 429, destinatarios o confirmación humana.

## MySQL QA nativo

La administración atendió PREPARACION_QA_WINDOWS.md, comprobó/provisionó el esquema separado y las dos identidades. No se obtuvo root, no se leyó C:\agente ni se alteraron los usuarios o el esquema histórico.

| Identidad | Esquema | Privilegios efectivos comprobados |
|---|---|---|
| siger_windows_qa_app@localhost | siger_institucional_windows_qa | SELECT/INSERT/UPDATE/DELETE |
| siger_windows_qa_migrator@localhost | siger_institucional_windows_qa | DML y CREATE/DROP/REFERENCES/INDEX/ALTER/CREATE VIEW |
| siger_windows_migrator@localhost | siger_institucional_windows_lab | Privilegios anteriores, **CREATE VIEW ya revocado** |

Credenciales QA en los dos PSCredential DPAPI CurrentUser de C:\req\config, ACL solo usuario operativo/SYSTEM/Administradores. Preflight JDBC Java25 verificó CURRENT_USER, DATABASE, versión8.0.46, TLS1.3 y SHOW GRANTS para ambos usuarios. El runner resuelve secretos privadamente, los usa solo en stdin del preflight y en el entorno efímero de Maven; los elimina/restaura al terminar. No hay claves en archivos versionados, argumentos o informes.

`DestinoJdbcPruebas` acepta la URL Windows QA exacta, con VERIFY_IDENTITY, sin fallback, truststore SIGER y usuarios QA exactos. Rechaza histórico windows_lab, otros hosts, TLS degradado, parámetros adicionales/duplicados y root/cuentas históricas. Se mantiene el destino descartable Linux anterior; no se amplió a otros esquemas. Cuatro tests negativos/positivos verifican esa frontera.

La batería QA aplica/valida V1–V12 con migrador y usa aplicación DML. Los fixtures destructivos operaron únicamente sobre QA. No se volvieron a importar las 504 filas históricas, los 24 objetos o Git.

## Resultados verificables

| Verificación nativa Windows | Resultado |
|---|---|
| H2, selección funcional | 213 seleccionadas / 208 correctas / 0 fallos / 0 errores / 5 omitidas |
| MySQL QA 8.0.46, TLS | 213 seleccionadas / 208 correctas / 0 fallos / 0 errores / 5 omitidas |
| Nuevos casos | 15 almacenamiento + 4 guardas de destino, incluidos arriba |
| Frontend | 42 correctas, lint y build correctos |
| Package Java25 | Correcto, JAR inactivo fuera de target |
| Histórico Windows | 59 hashes de tabla y 24 hashes/tamaños documentales iguales al corte previo |
| Graph real | Cero envíos nuevos; histórico 4/4 confirmado y presupuesto agotado conservados |
| V1–V12 | Sin modificaciones |
| Runtime completo / HTTPS real / Edge | NO EJECUTADOS en esta continuación |
| Workers reales Windows | NO OPERATIVOS; cinco pruebas omitidas explícitamente |

No se añadieron exclusiones para obtener verde. Se conservaron las tres exclusiones históricas de estrés, comprobadas por nombre antes de Maven. Las cinco omisiones son de ServiciosDocumentalesRealesTest: PDF activo/extensión falsa, imagen/AV real, PDF multipágina aislado, Office/referencias y control antivirus real. No son pruebas aprobadas.

Durante desarrollo se detectaron el TEMP nativo fuera del área privada y un lookup NIO de SID literal no soportado; se ajustó el entorno QA privado y se resolvió el nombre de cuenta del SID mediante API, manteniendo la prueba negativa. Se retiraron mensajes de diagnóstico con identificadores/rutas. Las baterías finales no tienen errores.

El host mantiene Java21 global. Cada compilación selecciona Java25 explícitamente. Los JAR nuevos se copian con nombre por SHA, sin sobrescribir un artefacto activo. No se crearon servicios, tareas programadas o reglas de firewall. No se investigó hardware ni se ejecutó carga/estrés.

## Workers: evaluación y límite del checkpoint

Se inspeccionaron los parsers y el sandbox Linux por lectura. La sonda Windows creó/eliminó un perfil AppContainer efímero sin capacidades y configuró/cerró un Job Object. **No ejecutó un worker ni validó las ocho restricciones negativas**. API disponible no equivale a seguridad demostrada.

El inspector actual llama subprocesos para qpdf/FFmpeg/Poppler. Un Job de proceso único no permite portarlo literalmente; relajar esa restricción para que ejecute auxiliares arbitrarios no es aceptable. Hace falta un broker que seleccione parsers fijos, cree trabajos/capabilities/ACL/handle lists y límites antes de arrancar el hijo, y controle siempre timeout/terminación. ClamAV y herramientas complementarias necesitan un paquete nativo verificable y pruebas reales. No se instalaron como servicios ni se sustituyeron por un scanner simulado.

El detalle, costo de complejidad, riesgos, alternativas y las ocho pruebas pendientes están en [EVALUACION_WORKERS_WINDOWS_2026-10-26.md](EVALUACION_WORKERS_WINDOWS_2026-10-26.md). La vía preferida compatible con el objetivo es el broker nativo AppContainer/Jobs. Un worker Linux separado requeriría un cambio de arquitectura explícito y no se declararía completamente nativo.

## Reanudación exacta

1. Conservar este commit, esquema histórico/QA, manifiestos, originales, presupuestos y logs. No repetir transferencia/importación.
2. Implementar/revisar el broker nativo y sus ocho pruebas negativas antes de conectar AV/inspector/renderer. Solicitar autorización solo si aparece una necesidad concreta de host/privilegios/servicios fuera del alcance.
3. Adaptar los guards de configuración privada bootstrap/mail y el launcher Windows DPAPI/HTTPS, sin activar Graph real o recuperación pública. El helper de archivos privados ya es portable; la configuración de sus raíces aún necesita integración.
4. Con los workers reales disponibles, ejecutar las cinco pruebas omitidas, runtime completo, login nexbyte, recursos/revocaciones/documentos/tareas, E2E Edge y reinicios. Los éxitos H2/MySQL no sustituyen ese recorrido.
5. No realizar cutover, detener WSL o publicar producción sin las condiciones/autorizaciones del encargo. El origen sigue intacto y operativo; la copia histórica no se utilizó como QA descartable.

Comandos PowerShell de una línea:

`& C:\req\repo\scripts\Windows-Regresion.ps1 -Mode H2`

`& C:\req\repo\scripts\Windows-Regresion.ps1 -Mode MySQL`

`& C:\req\repo\scripts\Windows-Regresion.ps1 -Mode Frontend`

`& C:\req\repo\scripts\Windows-BaseDatos.ps1 -Mode inspect -Identity migrator`

`& C:\req\repo\scripts\Windows-Verificar-Custodia.ps1`

Evidencias: C:\req\evidencias\*2026-10-26*, qa-jdbc-app.json, qa-jdbc-migrator.json y C:\req\logs\backend-h2-ntfs-2026-10-26.log / backend-mysql-ntfs-2026-10-26.log. Resumen sanitizado versionado: RESULTADOS_NTFS_WINDOWS_2026-10-26.json.
Artefacto final inactivo: `C:\req\bin\siger-847cd77c5af327be.jar`; SHA-256 `847cd77c5af327be992bc4e46aa83038f35f9f7b67229829c661412857c3b471`. Incluye la guarda 3e1c2cd; no se arrancó el runtime completo.
