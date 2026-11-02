# E2E interactivo personal · Fase B preparada

**ESPERANDO AL HUMANO. No se envió ni recuperó una Comunicación nueva.**

El usuario completará personalmente el Formulario. Su confirmación de envío habilita únicamente la verificación de cola (Fase C), **no autoriza todavía la recuperación**. Después de acreditar exactamente una entrega sintética se debe pedir una autorización humana separada para Fase D.

## Acceso disponible

- Formulario: `https://formulario.defensorianinez.gob.cl/?vista=inicio`, resolución local127.0.0.1 y TLS validado, no producción.
- SIGER: `https://localhost:8081/`.
- Buzón: `https://localhost:8081/#comunicaciones`. La pantalla se llama **Comunicaciones**; es el acceso de Distribución en esta versión, sin menú separado llamado Clasificación y Distribución.
- Cuenta QA existente **nexbyte**, activa y con `DISTRIBUCION / INSTITUCION` efectivo. Es multirrol del laboratorio; no se creó un superusuario ni se otorgaron facultades adicionales. Autenticación individual real y permiso de consulta comprobados en Edge.
- Contraseña existente verificada, **sin cambio ni reset**. Consultarla bajo `DEFENSORIANINEZ\nexbyte.proveedor` mediante un diálogo local inicialmente enmascarado:

```powershell
& "C:\Program Files\PowerShell\7\pwsh.exe" -NoProfile -STA -File C:\req\repo\scripts\Windows-Ver-Acceso-QA.ps1
```

El visor lee DPAPI CurrentUser y no imprime, guarda ni envía la contraseña. No lo ejecutar desde la automatización ni capturar su contenido. La persona puede revelar el dato en la ventana local y usarlo en SIGER.

La conexión del Buzón ya funciona: se comprobó la fila **COM-002497** y su detalle **Original recibido · solo lectura**, incluido el formulario completo. Su enlace es `https://localhost:8081/#comunicacion/893a552c-67cd-4f91-9495-1af5f12dae8f`. Se verificó la presencia de **Distribuir Comunicación**, sin pulsarla ni modificar el registro. No es necesario reconstruir otro buzón o esperar U5 para ver la nueva Comunicación después de su futura recepción.

## Procesos y límites

- SIGER nativo PID247148, launcher temporal PID74924. Run `C:\req\testdata\runtime-qa-c6ad91b5b15d4f9da964e2925060f4a8`.
- Mismo candidato `C:\req\bin\siger-9897abd7cd0bd57d.jar`, sin recompilar ni sustituir.
- Custodia preservada `C:\req\testdata\runtime-qa-b371266c31744ccc96f5f0668fcf7154\archivo`.
- Trust combinado privado para conservar firmas QA anteriores y COM-002497: `C:\req\testdata\runtime-qa-5e52e30513944a6c9204d94979499a1e\recepcion\publico-trust.json`.
- Perfil nuevo `C:\req\config\formulario-wsl-e2e-20d4349425a943378b12328fb67e5711\perfil.json`; perfiles anteriores intactos.
- Runtime documental habilitado, Graph simulado, scheduler de recuperación falso. Control **SIGER recuperar=false**, revisión2, cerrado por API operacional auditada. El endpoint manual existe pero no puede reclamar mientras ese control esté cerrado.
- Launcher de30min, sin servicio o tarea permanente: vencimiento conservador **31/10/2026 aproximadamente19:57, America/Santiago**. Si vence, relanzar únicamente el runtime propio con los mismos datos y controles cerrados; no borrar ni regenerar nada enviado por la persona.

El Formulario lo administra exclusivamente su agente, chat «Leer encargo maestro SIGER»:

- Nueva raíz `/home/nexbyteproveedor/.local/share/siger/e2e-siger-20d4349425a943378b12328fb67e5711`, JVM5256 del mismo JAR c8d1a07/SHA11038950760953e166ed49d3bb15d9900a147ee84f4b0e80263b3de0773d41df.
- MySQL/volumen QA nuevos, Caddy sin cambios y clamd real13310. La prueba anterior8e59… y su volumen/dump siguen intactos y detenidos.
- `aceptarNuevos=true`, **`recuperar=false`**, NORMAL; capacidades adultos/NNA, adjuntos clásicos y multimedia=false. CU/PISEE/retención deshabilitados. Cero recorridos/adjuntos/Comunicaciones/cola histórica al entregar su preparación.
- No crear datos ciudadanos desde ninguno de los agentes. El usuario puede usar adulto sin CU con identificación declarada/documento extranjero o NNA sin RUN, datos ficticios y un TXT limpio pequeño. No generar por él un borrador, archivo o envío.

## Evidencia y preservación

Campaña privada `C:\req\evidencias\e2e-interactivo-2026-10-31-11b2ff68b2fa4a16a029ce489dcc102f`. Punteros: `C:\req\scratch\ultimo-e2e-interactivo.txt`, `runtime-e2e-interactivo.txt`, `trust-e2e-interactivo.txt`.

- HTTPS público y SIGER válidos sin bypass; CA y firma ACK offline correctas. Sonda pública HEAD401/401/404, sin reclamar/ACK.
- Edge headless de solo lectura: nueve controles correctos (login, cuenta, rol, buzón, detalle/API y facultad visible). Contexto cerrado y logout realizado; no se usa la sesión del navegador humano, ni se guardan cookies, trazas o credenciales.
- QA Windows9COM/11documentos, un Caso/Requerimiento, cero duplicados/ACK pendientes/Graph reales, sin cambios por la preparación. COM-002497 preservada.
- Histórico504filas/59tablas idénticas;24originales con hashes/ACL conservados.
- Guarda4GiB respetada; última medición conjunta5,43GiB libres. Volver a comprobar antes de Fase D.
- Visor local: sintaxis y componentes WinForms STA comprobados **sin mostrar ni descifrar para pantalla el secreto**. La persona lo abrirá; la contraseña sí fue validada por el test de autenticación vía stdin privado.
- Rama `preparacion-interoperabilidad-formulario-2026-10-31`, HEAD48dd236, trabajo local preservado. No hay push, nuevas migraciones ni cambios a U5 pendiente.

## Punto exacto de continuación

1. **Ahora detener la actuación de los agentes en Fase B.** Entregar URLs/acceso/navegación y esperar confirmación humana de envío. No sondear ni recuperar su Comunicación en segundo plano.
2. Solo con esa confirmación: pedir al responsable público evidencia de la única fila reclamable de **20d434…**, su UUID/descriptor/huellas, y congelar nuevas recepciones QA si procede. No confundir con la raíz anterior8e59… ni con sus punteros de la prueba técnica ya terminada.
3. Con una sola entrega verificada, **preguntar al humano si autoriza la única recuperación de ese UUID**. El mensaje «envié» por sí solo no satisface esta autorización separada.
4. Solo después de autorización: preflight RAM, abrir controles QA mediante APIs auditadas en ambos lados, una invocación manual sin scheduler, comprobar COM/folio/ACK y devolver el enlace funcionarial de la nueva Comunicación. Mantener SIGER disponible para que la persona la inspeccione; no detenerlo automáticamente al terminar la recepción como se hizo en el E2E técnico anterior.
5. Para verificar no reutilizar sin adaptación el harness antiguo `VerificarComunicacionE2E`, que estaba fijado al escenario extranjero/grupo de COM-002497 y exigía parar SIGER. La nueva operación puede tener otro recorrido y debe comprobarse sin interrumpir su consulta UI: API autorizada, registro firmado y original, todos correspondientes al nuevo UUID.

No implementar ni activar automatización o decisiones U5 durante estas fases.

## Continuación 01/11/2026

El humano confirmó el envío de `d4b72201-26fe-4334-aabb-a910ec076fc3`. Fase C está verificada: es la única entrega pendiente de la misma QA, con dos originales íntegros. Nuevas recepciones y extracción públicas cerradas; extracción SIGER cerrada y automática deshabilitada. **La autorización humana de Fase D sigue pendiente; no ejecutar la recuperación hasta recibirla expresamente.** Continuar desde `docs/CHECKPOINT_E2E_INTERACTIVO_FASE_C_2026-11-01.md`; el runtime temporal de Fase B fue reemplazado por un nuevo arranque sobre los mismos datos y custodia, sin recuperaciones.
