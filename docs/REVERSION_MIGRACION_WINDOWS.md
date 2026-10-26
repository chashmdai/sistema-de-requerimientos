# Reversión no destructiva — U4.5 Windows

Estado al checkpoint: no hay aplicación SIGER completa Windows ejecutándose, servicio ni tarea programada nuevos. No se realizó cutover; WSL sigue siendo la instancia operativa original. No hay que detener MySQL80 ni deshacer datos para conservar la operación anterior.

1. Conservar C:\req\repo (todas sus ramas/commits), el bundle, C:\req\storage, las bases, evidencias y material privado DPAPI. No ejecutar reset, clean, Flyway clean/repair/DOWN, DROP/TRUNCATE, borrado de volúmenes, reducción de columnas o sustitución de hashes/firmas.
2. No iniciar los JAR de Windows como un servicio completo: los adaptadores de custodia/aislamiento siguen pendientes. No activar correo real, scheduler externo o pull del Formulario Público. No habilitar CU/PISEE. La copia importada no debe conectarse a la custodia o outbox mutable del origen.
3. El esquema siger_institucional_windows_lab contiene el snapshot confirmado: conservar sus 59 tablas de negocio y la historia Flyway nativa. No eliminarlo ni reimportar encima para compensar un error. Si una comparación diverge, guardar evidencia y conciliar; el importador se detiene ante un estado desconocido y reutiliza exactamente el corte ya confirmado.
4. Fuente WSL, documentos, datos, configuración y servicios no se modificaron. La operación sigue allí; no requiere restauración de secretos ni reinicios. No aplicar este procedimiento a C:\agente, MySQL80 compartido, nginx/relays, otros servicios o formulario-web.
5. CREATE VIEW fue temporal y exclusivo del migrador. Ya terminadas V1–V12, el administrador autorizado puede ejecutar en su canal privado MySQL: `REVOKE CREATE VIEW ON siger_institucional_windows_lab.* FROM 'siger_windows_migrator'@'localhost';`. No usar root como runtime ni acceder a credenciales de C:\agente. Comprobar después con la consulta de permisos del helper; app debe seguir solo con DML. La revocación no elimina vistas ni altera datos.
6. DPAPI CurrentUser vincula los materiales a la cuenta Windows actual. No moverlos a otra cuenta/servidor asumiendo que se descifrarán, ni imprimirlos, convertirlos a plaintext para Git o copiarlos a respaldos accesibles. Conservar CA/truststore y los originales Linux. No cambiar ni resetear contraseñas, claves, campañas, intentos o confirmaciones para revertir una migración técnica.
7. No sustituir originales por PNG ni actualizar sus SHA para aceptar diferencias. Los 24 objetos y sus metadatos se cotejan con el manifiesto. La copia actual no constituye un writer NTFS validado frente a cortes de energía; no emitir ACK desde ella hasta completar esa ingeniería.
8. No detener/reconfigurar WSL para el cutover sin la autorización específica exigida en U4.5 y sin tener la alternativa nativa probada. No hay una segunda instancia de aplicación activa ahora.

Comprobación de datos/Graph histórico, un comando PowerShell de una línea:

`& C:\req\repo\scripts\Windows-BaseDatos.ps1 -Mode audit -Identity app`

Comprobación de objetos y ACL:

`& C:\req\repo\scripts\Windows-Verificar-Custodia.ps1`

Comprobación del permiso temporal:

`& C:\req\repo\scripts\Windows-BaseDatos.ps1 -Mode inspect -Identity migrator`

Los scripts usan DPAPI→stdin y mensajes sanitizados; no agregar contraseñas al comando. Los backups cifrados locales no sustituyen respaldos externos o recuperación de desastre; no se declara producción lista.