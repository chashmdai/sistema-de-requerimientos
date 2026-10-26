# Checkpoint U4.5 — runtime QA nativo, documental bloqueado

26/10/2026. Repositorio `C:\req\repo`, rama `migracion-windows-laboratorio-2026`. Implementación **b95345f**, sobre e13e5ea/8411613. La documentación de este corte queda en un commit posterior, consultable con git log. Sin push. Git y checkpoints anteriores conservados.

**Decisiones actuales del usuario:** mantener100% nativo Windows; alternativa Linux descartada; conservar el bloqueo de recursos y el checkpoint por ahora. No iniciar U5 ni nuevos ensayos AV ampliados hasta retomar la ventana. No detener servicios o WSL para obtener memoria.

## Terminado y comprobado

- Custodia NTFS anterior conservada.59 tablas históricas y24 objetos iguales; V1–V12 intactas. CREATE VIEW histórico revocado.
- Arranque **PRELIMINAR_SIN_DOCUMENTAL** desde JAR inactivo por SHA en C:\req\bin; Java 25, HTTPS loopback, credencial DPAPI QA app y TLS VERIFY_IDENTITY. Migrador separado; runtime comprueba DML real.
- Rutas privadas Windows portables para bootstrap/allowlist. Custodia independiente de habilitar recuperación pública. Graph real y recepción pública bloqueados en el launcher.
- TLS localhost exclusivo aprobado por el usuario: AD60D34C9FEDA16E215EE5F223F35BA3E4371CF5, CurrentUser Root solamente; P12/password bajo DPAPI.
- H2/MySQL Windows: **214 correctas por motor, 0 fallos/errores, 5 documentales omitidas**. Frontend42/lint/build correctos.
- Edge real/HTTPS/MySQL QA: **59 comprobaciones** de operación; **45** en la pasada ampliada tras reiniciar JVM. Login nexbyte, TI, roles/revocaciones, COM/idempotencia, Caso/Requerimiento, tareas, avisos, outbox SIMULADO y denegaciones. No incluye un documental real operativo.
- ClamAV portable1.4.6 y bases oficiales verificados; scanner falla al cargar todo dentro768 MiB, nunca se toma como limpio. Sonda AppContainer/Jobs de6 modos y limpieza de ACL/procesos comprobada. No equivale a calificar ocho controles de los parsers reales.

## Estado preservado

Histórico: siger_institucional_windows_lab y C:\req\storage\archivo; no abrir como QA mutable ni reimportar.504 filas originales y24 objetos conservados. Graph histórico4/4 confirmado/presupuesto agotado,0 ACK pendientes; cero correos nuevos.

QA: siger_institucional_windows_qa. Deja3 cuentas,3 COM únicos,3 Requerimientos,3 Casos,3 tareas y12 avisos;1 intento SIMULADO y0 aceptados por Graph. Incluye una operación de desarrollo cuya prueba UI intermedia abortó; no borrar evidencias. Las regresiones MySQL limpian fixtures: no ejecutarlas con el runtime QA activo ni esperar conservar estos datos.

Runtime/Edge propios detenidos. Sin servicios/tareas programadas/listeners nuevos. Secretos efímeros retirados de las3raíces runtime. WSL original sin cambios. No hay dependencia Linux operacional nueva.

## Próximo paso exacto

1. Leer [informe](RUNTIME_QA_WINDOWS_2026-10-26.md), [dictamen nativo y decisión](DECISION_DOCUMENTAL_WINDOWS_2026-10-26.md) y [reversión](REVERSION_MIGRACION_WINDOWS.md). Revisar Git antes de modificar; conservar b95345f y este checkpoint.
2. Cuando se autorice retomar y exista margen, ejecutar **una vez** el ensayo guardado. Exige4 GiB libres y usa Job1,5 GiB/CPU 45 s/90s, sin cambios de host. El preflight actual dio BLOQUEADO_RECURSOS con 2.658.258.944 bytes disponibles y **no ejecutó scanner**. Los4 GiB son una guarda conservadora de esta prueba, no una certificación del consumo final.
3. Si el scanner pasa limpio/EICAR/indisponible, completar el launcher nativo para herramientas fijas y el inspector/renderizador. No convertir la sonda genérica en API de ejecución arbitraria. Probar lectura/escritura/red/procesos/recursos/timeout/crash/fallo seguro antes de conectarlo a documentos.
4. Reutilizar AV/parsers privados y APIs Windows; no activar Defender ni tocar el proveedor Kaspersky. AMSI inicializa en proceso ordinario, pero no en este AppContainer; no se escaneó contenido fuera del aislamiento.
5. Implementar configuración completa solo después de la calificación; ejecutar los 5 tests documentales reales, H2/MySQL/frontend y E2E autorizado de descarga/representación/reinicio. El launcher actual fuerza documental=false: su arranque no cierra U4.5.

## Comandos reproducibles (una línea cada uno)

Comprobaciones de solo lectura; no requieren reiniciar runtime:

```powershell
& C:\req\repo\scripts\qa\Windows-Auditar-Runtime.ps1
```

```powershell
& C:\req\repo\scripts\Windows-BaseDatos.ps1 -Mode audit -Identity app
```

```powershell
& C:\req\repo\scripts\Windows-Verificar-Custodia.ps1
```

Ensayo **aplazado por decisión del usuario**, para la siguiente ventana:

```powershell
& C:\req\repo\scripts\qa\Windows-Probar-Capacidad-AV.ps1
```

Preparadores existentes ya aplicados; no duplicar paquetes/certificados/cuentas. El de identidades es idempotente y nunca reemplaza passwords; solo trabaja en QA. Su classpath de compilación está en target/classes, no es el runtime permanente.

Para repetir un runtime preliminar en una nueva ventana, comprobar primero ausencia de otra instancia y preservar los datos QA:

```powershell
& C:\req\repo\scripts\Windows-Iniciar-QA.ps1 -Jar C:\req\bin\siger-20ce34eea019f4f9.jar -Seconds 600
```

Desde otra consola, únicamente sobre el run indicado por el launcher:

```powershell
& C:\req\repo\scripts\qa\Windows-E2E-Runtime.ps1 -Run ((Get-Content C:\req\scratch\ultimo-runtime-qa.txt).Trim())
```

Cada ejecución operate crea un fixture ciudadano sintético nuevo; su reintento dentro del test conserva un COM. after-restart reutiliza el baseline de la operación anterior y no crea otra.

```powershell
& C:\req\repo\scripts\qa\Windows-E2E-Runtime.ps1 -Run RUTA_QA_NUEVA -Mode after-restart -Baseline RUTA_QA_ANTERIOR\e2e-operacion.json
```

No lanzar dos runtimes sobre QA. El puerto elegido debe estar libre y el archivo stop solicita terminar solo el hijo de ese launcher; el tiempo máximo también lo detiene. La reapertura puede usar `-Archivo RUTA_QA_ANTERIOR\archivo`, que rechaza el almacén histórico.

## Evidencias

- `C:\req\logs\backend-{h2,mysql}-runtime-2026-10-26.log`:219 seleccionadas / 214 correctas / 5 omitidas por motor.
- `C:\req\logs\frontend-{test,lint,build}-windows.log`.
- `C:\req\evidencias\historico-runtime-2026-10-26.json`, `auditoria-runtime-historico-2026-10-26.json`, `custodia-runtime-final-2026-10-26.json`, `migrador-runtime-historico-2026-10-26.json`.
- `C:\req\evidencias\auditoria-runtime-qa-2026-10-26.json`, `sonda-proceso-appcontainer-2026-10-26.json`, `capacidad-av-windows-2026-10-26.json`, `clamav-portable-2026-10-26.json`.
- E2E operación59: `C:\req\testdata\runtime-qa-226fcde6e7414c778f9721237b6b9a05\resultado-edge-operate.json`.
- E2E reinicio45: `C:\req\testdata\runtime-qa-ee33881039754c90b409baeb9301eb97\resultado-edge-after-restart.json`.
- El último baseline exitoso está en e2e-operacion.json del run operación59; el almacén reabierto está en `C:\req\testdata\runtime-qa-58efdf4c7f214755964d1c1228a18ddc\archivo`. No contiene los24originales históricos.

Artefacto: SHA256 `20ce34eea019f4f91dc0e4f8ebb8e1870024e466c31a720656d037014b1da7b1`. Baseline público consultado:bc6489cd75c3dff9ff8462b8d0275d52f1045f29, contratos1.0/1.1 conservados. No hubo pull/E2E nuevo contra esa versión ni retiradas/retención pública desde SIGER.
