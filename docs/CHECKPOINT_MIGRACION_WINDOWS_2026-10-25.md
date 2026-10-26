# Checkpoint U4.5 — Windows Server 2022, 25/10/2026

**Estado parcial verificable. U4.5 NO cerrada; U5 no iniciada; producción deshabilitada.**

- Destino: C:\req\repo, rama migracion-windows-laboratorio-2026.
- Base WSL conservada e intacta: 78b7caf, rama notificaciones-tareas-graph-laboratorio-2026.
- Implementación de transferencia: b4a41ab. Un commit posterior incorpora reproducción/evidencias/documentación; consultar git log -2 --oneline.
- Sin push; no se modificó formulario-web, C:\agente, servicios compartidos o configuración global Java/MySQL.

## Terminado

1. Bundle completo, seis refs persistentes/HEAD y árbol cotejados; cuatro ramas históricas intactas; fsck correcto. Fuente limpia. Sin colisiones, symlinks ni rutas largas en tracked.
2. Java25 y React compilados nativamente. 42 frontend, lint/build correctos. JAR inactivo fuera de target: C:\req\bin\siger-a8bed7746c55ea4f.jar; SHA a8bed7746c55ea4f17a2c9edf31dbf9c4105931eb9825e43feb9d531bdb49d1e.
3. MySQL Windows 8.0.46 real, VERIFY_IDENTITY/TLS1.3 y DPAPI→stdin. Migrador aplicó V1–V12; app solo DML y CREATE rechazado 1142. Checksums históricos iguales, sin editar las 24 migraciones.
4. Snapshot consistente cifrado: 60 tablas/516 filas. Importados 59 tablas/504 filas de negocio; huellas de todas las tablas, FK y diez AUTO_INCREMENT iguales. Historia original Flyway archivada; despliegue Windows con historia propia y los mismos checksums.
5. Replay posterior al COMMIT: alreadyImported, cero filas añadidas. Fuente seguía igual al snapshot al comprobarla.
6. 24 objetos NTFS: 18 originales + 6 páginas, 16.869.263 bytes; tamaño/SHA/ACL verificados. Cero regeneraciones. Copias privadas de diez archivos esenciales bajo DPAPI CurrentUser y dos snapshots cifrados; sin plaintext de backup retenido.
7. Conservados 11 COM únicos, 7 ingresos públicos/0 ACK pendientes, 5 Casos, 3 Requerimientos, 9 cuentas, una nexbyte con 7 roles, 3 tareas de Caso y 16 notificaciones. Cuatro intentos Graph/confirmaciones humanas y presupuesto 4/4 intactos. Cero correos nuevos.

## No terminado / no declarar como aprobado

- No hay runtime institucional completo Windows, ni launcher HTTPS de aplicación ni E2E nativo.
- H2 Windows: 194 seleccionadas, 128 correctas, 61 errores, 5 omitidas. Los errores son 55 de custodia POSIX y 6 del fixture Graph POSIX. Los cinco workers reales NO se ejecutaron en Windows.
- Batería funcional MySQL completa pendiente de un entorno QA Windows separado; las pruebas borran fixtures y no deben usar la copia conservada.
- ArchivoInstitucional necesita adaptador NTFS de ACL/reparse points/publicación durable. force de archivo/ATOMIC_MOVE/lock funcionan; force de directorio y permisos POSIX actuales fallan.
- ClamAV/inspector/renderer aislados Windows no configurados ni validados. Poppler instalado no basta. No usar workers WSL para afirmar cierre nativo.
- Guards privados de bootstrap/mail basados en POSIX/.local necesitan adaptación Windows coherente con DPAPI/NTFS. No activar Graph real.
- CREATE VIEW temporal ya no es necesario: se avisó al usuario para revocarlo. Verificar SHOW GRANTS antes del próximo avance; no asumir revocación sin comprobarla.

## Próximo paso exacto

Continuar sobre esta rama y estos datos, empezando por el adaptador NTFS y el broker documental Windows con pruebas de aislamiento/fallo cerrado. Después preparar QA MySQL separado, arranque privado DPAPI/HTTPS/app DML y regresiones Windows completas. Graph solo simulado; recuperación pública deshabilitada. No volver a importar ni recrear esquemas/volúmenes para arrancar.

Cualquier futuro cambio al origen WSL para el cutover requiere la autorización específica indicada en el encargo. Ahora WSL sigue operativo e intacto; Windows es una copia validada sin motor activo y puede divergir si el origen recibe cambios posteriores. Comparar antes de promoverla; no sobrescribir divergencias.

## Evidencia y comandos de consulta

- C:\req\evidencias: bundle/ref/árbol, inventarios, manifests, hashes SQL, FK/import/replay, JDBC/permisos, Flyway/secuencias, DPAPI, suites y artefacto.
- C:\req\logs: package, frontend, H2 y compilación de helpers.
- C:\req\config: credenciales nativas DPAPI existentes, truststore propio, snapshots .dpapi y origen-dpapi.
- C:\req\storage\archivo\objects: copia privada de los originales/páginas.

Consultas reproducibles, cada comando PowerShell en una sola línea:

`& C:\req\repo\scripts\Windows-BaseDatos.ps1 -Mode audit -Identity app`

`& C:\req\repo\scripts\Windows-BaseDatos.ps1 -Mode validate -Identity migrator`

`& C:\req\repo\scripts\Windows-Verificar-Custodia.ps1`

`& C:\req\repo\scripts\Windows-Proteger-Snapshots.ps1`

[Informe completo](CIERRE_MIGRACION_WINDOWS_2026-10-25.md) · [Reversión](REVERSION_MIGRACION_WINDOWS.md) · [Evidencia](EVIDENCIA_MIGRACION_WINDOWS_2026-10-25.json) · [Manifiesto](MANIFIESTO_DOCUMENTOS_WINDOWS_2026-10-25.json).