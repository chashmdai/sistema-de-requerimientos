# Reversión no destructiva — U4.5 Windows

> Estado posterior 26/10: [runtime QA preliminar verificado](RUNTIME_QA_WINDOWS_2026-10-26.md) y [checkpoint vigente](CHECKPOINT_RUNTIME_WINDOWS_2026-10-26.md). El documental sigue cerrado. Mantener 100% nativo; conservar el bloqueo de recursos según decisión del usuario. El corte anterior se conserva abajo.

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
## Adenda 26/10/2026 — adaptador NTFS y QA

El runtime completo Windows continúa sin arrancar. El origen WSL no fue detenido ni modificado; no hay que revertir datos de negocio para conservarlo.

- Conservar 6d2f6ee y el checkpoint posterior. No convertir una regresión de código en pérdida de objetos ni modificar V1–V12. No usar el JAR anterior como writer NTFS: carece de estas barreras.
- Los cambios de custodia se probaron en directorios sintéticos privados de scratch/QA, sin abrir la copia histórica como writer ni cambiar sus bytes. No retirar ACL o integridad para reabrirla. Las representaciones y los originales ciudadanos permanecen separados/inmutables.
- CREATE VIEW del migrador histórico está revocado y verificado; no volver a concederlo para el runtime. Las cuentas QA son otras, limitadas al esquema descartable y autorizadas por administración. No intercambiar sus DPAPI con las del histórico.
- El runner MySQL borra fixtures únicamente en siger_institucional_windows_qa y rechaza el histórico. Conservar el esquema QA y sus evidencias al terminar, sin DROP automático. Ante duda, ejecutar solo las consultas de inspección sobre el histórico.
- El uso de JNA está fijado a 5.19.1. Conservar las dependencias verificadas del artefacto. No retirar el flag de acceso nativo del runner para ocultar un fallo ni introducir un fallback de FlushFileBuffers/rename.
- La sonda AppContainer creó/eliminó solo un perfil propio efímero; ambos HRESULT0. El Job se cerró sin ejecutar hijos. No hay servicio, perfil persistente, firewall o tarea que desmontar. No confundir esa sonda con un broker habilitado.
- Recuperación después de una interrupción: reutilizar la clave/descriptor original; verificar SHA/tamaño y completar las barreras. Un archivo existente no basta para autorizar SQL/ACK. Si el contenido está alterado, conservar evidencia y restaurar únicamente desde una copia íntegra autorizada; no sustituir la huella para forzar aceptación.

La evaluación de aislamiento y lo pendiente están en CONTINUACION_NTFS_WINDOWS_2026-10-26.md y EVALUACION_WORKERS_WINDOWS_2026-10-26.md. No hay autorización nueva para correo real, pull público, servicios permanentes, cambios al host o U5.

## Adenda 26/10 — runtime QA, TLS local y sondas nativas

Implementación b95345f. Al checkpoint los procesos propios de runtime/Edge/scanner están detenidos. No se ha realizado cutover ni hay servicio/tarea programada que desinstalar. No detener MySQL80, Kaspersky, WSL, Java/Node ajenos o servicios institucionales para revertir esta unidad.

1. Conservar repositorio/commits, los esquemas histórico y QA, C:\req\storage\archivo, los 24 objetos, las raíces/evidencias sintéticas, paquetes y DPAPI. No importar otra vez, hacer reset/clean, bajar migraciones, borrar filas para ocultar pruebas, sustituir originales o cambiar hashes/COM/firmas/ACK.
2. El launcher actual solo admite QA y PRELIMINAR_SIN_DOCUMENTAL. Para terminar un proceso propio futuro usar su archivo stop o esperar el plazo del supervisor; comprueba salida y retira únicamente los archivos privados efímeros que creó. No matar procesos por nombre ni reutilizar un PID sin el handle/identidad del launcher.
3. Un error del supervisor puede dejar claves/P12 dentro de su raíz privada. Conservar y revisar esa raíz; comprobar que su proceso terminó antes de retirar los archivos exactos listados en run.json. No aplicar un borrado recursivo a C:\req, storage, config o scratch. Los datos/COM del ensayo se conservan en QA.
4. El certificado exclusivo localhost es una hoja CA=false, con SAN local, huella AD60D34C9FEDA16E215EE5F223F35BA3E4371CF5, aprobada por el usuario y confiada solo en CurrentUser Root. Mantenerlo permite retomar durante su vigencia; no hay que tocar el certificado ciudadano, la CA anterior ni LocalMachine. Para retirarlo, el operador puede quitar **solo esa huella** después de detener QA. La clave/P12/password siguen bajo DPAPI en C:\req\config\windows-qa-localhost-tls.json.dpapi; conservarlos protegidos, no exportarlos a Git o backups abiertos.
5. ClamAV portable está únicamente bajo C:\req\bin\clamav-1.4.6, con bases en C:\req\storage\antivirus-defs y una configuración de actualización manual bajo config. No se registró servicio, PATH global ni actualización programada. No ejecutar el scanner ampliado mientras siga aplazado por el usuario; conservar versiones/manifiesto/logs para retomar, sin borrar paquetes o bases como una supuesta reparación.
6. Las sondas crean perfiles AppContainer efímeros, cierran Jobs y restauran DACL. No son un broker instalado. Se corrigió una restauración .NET que no persistía los cambios; las ACE propias de prueba del paquete AV/bases fueron retiradas y se verificó cero residuales. No agregar ALL APPLICATION PACKAGES/Everyone a config/custodia o al proveedor Kaspersky para hacer funcionar AMSI.
7. La consulta AMSI no cambió registro, ACL, configuración o estado de productos antivirus. Su prueba ordinaria solo inicializó el API. No hay modificaciones de Kaspersky/Defender que revertir; no desregistrar proveedores ni copiar sus DLL.
8. Graph automático/real, recuperación pública y documental están cerrados por el entrypoint QA. El historial real 4/4 y el presupuesto 4/4 pertenecen al esquema histórico intacto. QA contiene un intento SIMULADO; no conciliarlo contra Microsoft ni reiniciar una campaña real.
9. Los datos QA son sintéticos y su guard rechaza el histórico. Los tests MySQL limpian fixtures: no ejecutarlos con la aplicación QA activa ni para demostrar conservación de sus fixtures previos. La conservación histórica se comprueba con fingerprint/audit y el manifiesto, en modo lectura.

Ejemplos de una línea para el operador, no ejecutados como reversión al corte:

```powershell
$r=(Get-Content C:\req\scratch\ultimo-runtime-qa.txt).Trim(); if($r -notmatch '^C:\\req\\testdata\\runtime-qa-[a-f0-9]{32}$'){throw 'Raíz inesperada'}; [IO.File]::WriteAllText((Join-Path $r 'stop'),'stop')
```

Retirada opcional de **la hoja QA exacta**, sin tocar la confianza de máquina o certificados anteriores:

```powershell
$s=[Security.Cryptography.X509Certificates.X509Store]::new('Root','CurrentUser'); try{$s.Open([Security.Cryptography.X509Certificates.OpenFlags]::ReadWrite); foreach($c in @($s.Certificates|Where-Object {$_.Thumbprint -eq 'AD60D34C9FEDA16E215EE5F223F35BA3E4371CF5'})){$s.Remove($c)}} finally{$s.Close()}
```

Cambiar de rama o retirar la confianza no recrea bytes borrados ni sustituye una restauración. No hay autorización para degradar garantías, introducir una dependencia Linux, activar producción/U5 o modificar el origen WSL como parte de esta reversión.

## Adenda — reanudación ClamAV tras exclusión de testdata

Ver [informe](VALIDACION_CLAMAV_WINDOWS_2026-10-26.md) y [checkpoint](CHECKPOINT_CLAMAV_WINDOWS_2026-10-26.md). ClamAV independiente pasó0/1/2; el adaptador Java sigue en calificación y **no está habilitado en el runtime**. No se creó servicio, worker permanente, nuevo JAR de ejecución o migración.

- Conservar los JSON únicos, logs separados, entradas sintéticas, paquetes/bases y checkpoints previos. El resultado `capacidad-av-windows-2026-10-26.json` es un índice del último intento; las copias por GUID preservan los anteriores. No limpiar la evidencia para convertir un fallo en un pase.
- La exclusión de Kaspersky fue aplicada por TI y se limita a testdata. Esta unidad no la modificó. No desactivar Kaspersky ni retirar/ampliar su exclusión, cambiar AMSI, activar Defender o tocar políticas/servicios como supuesta reversión del código.
- Los procesos de ensayo están acotados por Job/plazo y restauran perfiles/DACL. Confirmar su terminación antes de tocar archivos exactos propios. No matar procesos por nombre ni modificar el paquete/bases mientras exista un scanner o una sonda activos.
- Un candidato Java con `av-*`/`pending`, mutex abandonado o fallo de restauración permanece cerrado. Conservar el journal privado y verificar identidad/proceso/ACL/perfil antes de una conciliación administrativa acotada. No eliminar esos marcadores a ciegas ni transformar el fallo en limpio. No habilitarlo como bean para sortear la validación pendiente.
- No hay datos históricos que revertir. El MySQL de regresión es únicamente windows_qa y sus fixtures son descartables; no reimportarlos en windows_lab. Los24originales, COM/ACK/firmas, identidades y Graph histórico permanecen fuera del ensayo.
- El JAR previo `siger-20ce34eea019f4f9.jar` sigue conservado e inactivo. El código nuevo no lo sustituyó. Para dejar esta unidad en reposo basta con conservar el checkout y los procesos propios terminados; no usar reset/clean/DROP o cambios en WSL como reversión.

## Adenda — adaptador Java, corrección de herencia y Job al crear

Código2a6c6d2; [checkpoint](CHECKPOINT_ADAPTADOR_AV_WINDOWS_2026-10-26.md). El AV Java pasó0/1/2 antes del refuerzo posterior JOB_LIST. Este último y la cadena completa aún requieren calificación; no habilitar el runtime ni sustituir su JAR.

- Conservar logs/evidencia privada por GUID, incluidos los errores, y los checkpoints. El fallo0xC0000022 era acceso del hijo a archivos existentes, no una causa para borrar/reparar las bases AV o modificar Kaspersky. No revertir a SetFileSecurity para permisos heredables: no propaga a los hijos.
- No modificar ACL de paquete/bases mientras haya un scanner o sonda activos. La actualización y retirada ahora usan SetNamedSecurityInfo y conservan la protección/herencia anterior. La auditoría de este corte terminó con0SID temporales residuales.
- El directorio fallido `av-java-f226a15977fa4323a9224707e9ed05c1` conserva journal/entrada y salidas vacías; no tiene hijo activo. No borrar el marcador para hacer aprobar el constructor ni reutilizar esa raíz como un análisis limpio. Una conciliación debe verificar identidad/perfil, ausencia de procesos y descriptores, sin eliminar originales.
- Los logs opt-in no incluyen copia del original y permanecen privados fuera de Git. Un fallo de limpieza sigue rechazando el resultado. La recuperación tras un aborto de JVM no está acreditada: conservar ese bloqueo y no asumir que JOB_LIST concilia por sí solo el filesystem.
- El launcher JUnit nuevo utiliza dependencias ya existentes, ejecuta un único método real y no modifica el mecanismo de sandbox ni sus límites. No mezclar versiones de engine/launcher ni bajar la guarda4GiB ante un error de recursos.
- No se creó servicio/tarea, puerto, JAR de ejecución o cambio de MySQL/WSL/Formulario Público. No hay datos de negocio que deshacer. Mantener las restricciones previas, las claves, los24objetos y todos los commits locales, sin reset/clean/importación o cambios a políticas del host.
