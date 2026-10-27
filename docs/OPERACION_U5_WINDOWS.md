# Operación U5 en Windows QA

Leer primero el [contrato interno](../contratos/solicitudes-tareas-u5-laboratorio-v1.md). Las decisiones de respuesta, canalización y escalamiento están pendientes por instrucción del usuario y cerradas en backend.

Repositorio `C:\req\repo`; rama `feature/u5-solicitudes-tareas-independientes`. Java25 explícito, dependencias documentales privadas de U4.5, MySQL QA con `VERIFY_IDENTITY`, credenciales DPAPI, HTTPS localhost y aplicación DML. Sin WSL, producción, correos reales ni recuperación pública. El histórico Windows permanece en V12 y no se usa para operar U5.

## Validación reproducible

Detener primero el runtime propio de QA mediante su archivo `stop`; no ejecutar regresión MySQL mientras opere contra ese esquema. Cada comando PowerShell ocupa una línea.

```powershell
& C:\req\repo\scripts\Windows-Regresion.ps1 -Mode H2 -DocumentalRun ((Get-Content C:\req\scratch\ultimo-documental-win.txt -Raw).Trim())
& C:\req\repo\scripts\Windows-Regresion.ps1 -Mode MySQL -DocumentalRun ((Get-Content C:\req\scratch\ultimo-documental-win.txt -Raw).Trim())
& C:\req\repo\scripts\Windows-Regresion.ps1 -Mode Frontend
& C:\req\repo\scripts\Windows-Regresion.ps1 -Mode Package
& C:\req\repo\scripts\Windows-Preparar-Artefacto.ps1
& C:\req\repo\scripts\qa\Windows-Preparar-Identidades-Runtime.ps1
```

MySQL migra V13 exclusivamente con el migrador QA. Pruebas y aplicación utilizan DML. Las guardas rechazan el esquema histórico. No crear otro esquema, importar datos ni obtener contraseñas. Guardar el artefacto por su SHA devuelto; ejecutar desde `C:\req\bin`, no desde `target`.

ClamAV requiere 4 GiB físicos disponibles; AppContainer/Job y límites no cambian. Si la guarda impide un ensayo, se registra como no aprobado por recursos; no se cambia el mínimo ni se detienen procesos ajenos. Una batería sin configuración documental no sustituye la validación real.

Usar `Windows-Iniciar-QA.ps1` con el JAR instalado y la configuración documental indicada en `ultimo-documental-win.txt`, conforme a [operación documental](OPERACION_DOCUMENTAL_WINDOWS.md). Los ingresos y la custodia QA de una batería anterior no deben reutilizarse después de una limpieza SQL de fixtures sin conciliación: preparar un runtime y productor sintético nuevos, conservando los anteriores como evidencia.

`Windows-E2E-Runtime.ps1` incorpora modos `u5` y `u5-restart`. El primero utiliza Edge real, TLS validado, UI y API autorizada; conserva `e2e-u5-operacion.json` en la raíz del runtime propio. El segundo recibe ese archivo mediante `-Baseline` después de reiniciar sobre el mismo esquema y custodia. No incluir credenciales en los argumentos. Ambos reutilizan el mecanismo DPAPI por stdin existente. `u5-documentos` recibe también `-IngresoRun` y prueba los ingresos sintéticos y su custodia; si la vista falla conserva `u5-custodia-ciudadana-parcial.json` y termina con error. Ese archivo no acredita una previsualización correcta.

Las evidencias privadas quedan en `C:\req\evidencias` y `C:\req\testdata`; no subir capturas, documentos ni archivos de configuración privada a Git. El informe de cierre enumera exactamente los ensayos realizados.

## Uso funcional

**Mis tareas** reúne tareas personales de todos los contextos, con filtros de estado y contexto. Permite crear una tarea independiente sin crear un Caso. En el detalle se consultan fechas, ventana, plazo e historial; completar requiere confirmación manual.

Desde una Comunicación autorizada, abrir **Solicitudes en preparación**, guardar título y descripción. El detalle permite editar, preparar un borrador, registrar seguimientos, consultar relaciones autorizadas e historial y crear una tarea personal vinculada. Las tres decisiones finales permanecen deshabilitadas y también se rechazan por API. No hay envío de respuesta implícito al guardar.
