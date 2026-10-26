# U4.5 — Runtime QA Windows verificado; documental pendiente

> Continuación posterior: [validación ClamAV](VALIDACION_CLAMAV_WINDOWS_2026-10-26.md) y [checkpoint vigente](CHECKPOINT_CLAMAV_WINDOWS_2026-10-26.md). Scanner independiente0/1/2 acreditado; adaptador Java por validar, sin modo documental completo. Los resultados y decisiones del corte anterior permanecen abajo.

Corte 26/10/2026. Implementación `b95345f`, rama `migracion-windows-laboratorio-2026`. **No es cierre de U4.5:** funciona un runtime institucional preliminar nativo, con el documental deshabilitado explícitamente. No hay producción, cutover, servicio permanente ni U5.

Al recuperar Git se encontró el avance posterior `e13e5ea` en `verificacion-recepcion-sin-agente-2026-10-26`. Se conservó y se incorporó por fast-forward a la rama solicitada. No se perdieron cambios, no se repitió bundle/importación ni se modificó main. Las evidencias de recepción sin agente siguen disponibles y se distinguen de este corte.

## Runtime implementado y probado

- `ArranqueWindows` recibe configuración por stdin anónimo, con tamaño/estructura/propiedades limitados y errores de entrada sanitizados. Rechaza esquema histórico, migrador, otros puertos, propiedades no admitidas y modos completos no calificados.
- Fuerza perfiles `jdbc-reception,nucleo-lab,windows-lab`, laboratorio individual, cookies Secure, HTTPS y binding 127.0.0.1. Fuerza correo SIMULADO/automático=false y recuperación pública/V2/documental=false. Flyway no se ejecuta en el runtime.
- La conexión runtime exige realmente CURRENT_USER=siger_windows_qa_app@localhost, DATABASE=siger_institucional_windows_qa y SHOW GRANTS exclusivamente DML. La URL usa VERIFY_IDENTITY sin fallback, con el truststore SIGER. Las migraciones de regresión usan otra identidad QA, con sus privilegios DDL autorizados.
- `ConfiguracionDatos` admite confianza CA local explícita PKCS12 con gramática cerrada; rechaza duplicados, aliases de TLS, fallback y destinos remotos del truststore. `public-trust` es la contraseña de integridad pública del almacén de CA, no una contraseña MySQL.
- `ConfiguracionPrivada` mantiene la custodia POSIX y admite raíz privada Windows explícita, ACL/reparse comprobados y fuera de Git. Bootstrap y allowlist reutilizan esta frontera. El runtime QA no ejecuta bootstrap histórico ni reemplaza contraseñas.
- La custodia primaria se configura independientemente de habilitar un cliente público. El arranque QA puede abrir su ArchivoInstitucional sin construir una FuenteFormulario ni disponer de token público.

`Windows-Iniciar-QA.ps1` ejecuta Java 25 explícito desde el JAR por SHA de C:\req\bin; target solo se usa para compilación/preparadores QA. Depura el entorno del hijo, no pasa contraseñas por argumentos y no cambia Java 21/PATH globales. Lee únicamente la credencial DPAPI QA app, el material específico de firma/confianza/allowlist y el TLS QA. El lanzador final **no descifra application.properties del origen ni el secreto Graph**.

Las claves/P12 necesarios mientras vive Java se materializan en una raíz efímera privada de ese run. Los passwords viajan por stdin y no se escriben en propiedades plaintext. Al terminar el hijo se retiran únicamente esos archivos exactos; se conservan datos, custodia sintética y logs. Las tres raíces de runtime utilizadas quedaron con cero archivos privados efímeros. Esto no pretende acreditar borrado forense de bloques NTFS.

### TLS exclusivo de SIGER QA

El P12 importado era válido para el hostname ciudadano de ClaveÚnica, no para localhost. Se detuvo ese primer arranque y se preparó un certificado local distinto, mediante las APIs criptográficas existentes de Windows/.NET: RSA2048/SHA256, SAN localhost/127.0.0.1/::1, serverAuth y CA=false, vigencia30 días. La clave/P12 y su password se conservan juntos bajo DPAPI CurrentUser.

Windows presentó su aviso de confianza. El usuario aprobó expresamente la huella **AD60D34C9FEDA16E215EE5F223F35BA3E4371CF5**; se comprobó CurrentUser Root=true y LocalMachine Root=false. Vence 25/11/2026. No se cambiaron hosts, DNS, confianza de máquina ni certificados anteriores. Edge y Node verifican cadena/hostname; Node usa `--use-system-ca`. No se utilizaron ignoreHTTPSErrors, excepciones SPKI o bypass del aviso TLS.

La intervención manual también respetó la habilidad operativa [Computer Use, SKILL.md](C:/Users/nexbyte.proveedor/.codex/plugins/cache/openai-bundled/computer-use/26.1021.31345/skills/computer-use/SKILL.md), que remite a su guía: «Do not automate Windows security or anti-malware apps». Es una restricción del agente durante la operación, no una política institucional inventada. El usuario resolvió el aviso de Windows.

## Resultados realmente ejecutados

| Verificación nativa Windows | Resultado final |
|---|---|
| Backend H2 | 219 seleccionadas: **214 correctas, 0 fallos, 0 errores, 5 omitidas** |
| Backend MySQL QA 8.0.46 | Igual: **214 correctas, 0 fallos, 0 errores, 5 omitidas** |
| Frontend | **42 correctas**, lint y build correctos |
| Configuración/arranque nuevos | 3 tests de arranque +2 de raíz privada +1 de confianza TLS, incluidos arriba |
| E2E Edge 156.0.4314.42/HTTPS/MySQL | **59 comprobaciones** en la pasada ampliada de operación |
| E2E tras reiniciar JVM y reabrir custodia QA | **45 comprobaciones**, COM/folio/tarea y clave idempotente conservados |
| Sonda AppContainer/Jobs | 6 modos canario reales; no se cuentan como métodos JUnit ni como ocho controles documentales aprobados |
| ClamAV real | Paquete/bases comprobados; carga completa falla dentro de768 MiB; base ausente devuelve2 |
| Ensayo AV ampliado | **No ejecutado**, guarda de memoria bloqueada y checkpoint solicitado por el usuario |
| Workers documentales completos / sus 5 tests | **Pendientes**, sin aprobación artificial |

Las cinco omisiones siguen siendo ServiciosDocumentalesRealesTest: PDF activo/extensión falsa, imagen/antivirus real, PDF multipágina, Office/referencias y control antivirus real. No se añadieron exclusiones. Se preservaron las tres exclusiones históricas de estrés por nombre. No se contabilizan como éxitos pruebas Linux anteriores.

La batería MySQL final se ejecutó sola, antes de provisionar las identidades de demostración. Una corrida de desarrollo coincidió con la provisión QA y se descartó como evidencia aislada; se repitió completa y pasó. Los tests destructivos operaron exclusivamente sobre windows_qa. No volver a lanzarlos mientras exista un runtime QA activo ni esperar que conserven los fixtures de demostración.

### Qué acredita el E2E

Se ejecutó Edge instalado, en modo headless, con frontend React real y requests HTTPS al backend/MySQL reales. Playwright existente se reutilizó sin instalar otro navegador. Se bloquearon peticiones del navegador a otros orígenes; los secretos de login entran al harness por stdin y los errores se sanitizan sin trazas, tokens o cookies.

- Login UI de nexbyte, siete roles de una identidad persistente QA, administración TI, cookies Secure/HttpOnly/Strict y rechazo CSRF.
- Registro manual, reintento con igual clave, distribución y apertura expresa de Caso/Requerimiento mediante HTTP autenticado. Recepción/distribución por sí solas no los abren.
- Consulta UI de Caso/contexto de Requerimiento, tarea personal dentro del Caso abierto, vista reunida de tareas y notificaciones.
- Outbox con transporte SIMULADO; el destinatario se resuelve desde la cuenta MySQL. Repetir la operación ya simulada no genera otro intento. Cero solicitudes a Microsoft Graph.
- Otra unidad no consulta el Caso/tarea ni administra TI. TI sin facultad operativa no lee Caso/Comunicación.
- Otorgar/revocar roles y desactivar la cuenta invalidan su sesión previa. La cuenta restringida se rehabilita con su perfil y roles previos; no se reemplaza su contraseña. Logout invalida la sesión.
- Parada/reinicio de la JVM, reapertura del mismo almacén QA y replay de la misma operación conservan el COM, folio y tarea.

Son comprobaciones del harness, no 59/45 métodos JUnit ni una certificación de todas las pantallas. Central/regional, derivaciones, reaperturas y autorización documental también se cubren en la regresión H2/MySQL; **no se presenta cada una como un recorrido Edge completo**. No se validaron descarga/representación documental mediante este runtime deshabilitado.

El desarrollo del harness dejó3 Comunicaciones,3 Requerimientos,3 Casos y3 tareas sintéticas, con3 claves públicas de operación diferentes y cero COM duplicados. Una pasada intermedia conservó su operación al fallar un selector que encontraba dos títulos iguales; se corrigió para identificar el enlace de la tarea por su ID. Los registros no se borraron para ocultar el intento. La evidencia final de operación tiene59 comprobaciones correctas; la de reinicio45. La pasada inicial reducida33 se conserva como antecedente.

Auditoría QA final:3 cuentas,12 asignaciones incluidas las revocadas,12 notificaciones/outbox,1 intento SIMULADO y0 ACCEPTED_BY_GRAPH. El histórico importado, en otro esquema, conserva sus4 intentos reales y4 recepciones humanas.

## Conservación comprobada

Las59 tablas de negocio del histórico coinciden íntegramente con `sql-destino-importado.json` por recuento/hash. Se mantienen11 Comunicaciones/COM,5 Casos,3 Requerimientos, una identidad nexbyte con7 roles,3 tareas,4 intentos/confirmaciones Graph y presupuesto 4/4, cero ACK pendientes. No se utilizó este esquema para fixtures o arranque web.

Los24 objetos históricos mantienen tamaño/SHA y16.869.263bytes totales. Auditoría de ACL/config/storage/scratch: cero ACE inesperadas. CREATE VIEW del migrador histórico sigue revocado, app sigue DML; QA conserva su migrador separado autorizado. V1–V12 no cambiaron. No se modificó WSL, formulario-web, C:\agente, MySQL80 global, servicios institucionales, dominio, GPO, firewall o puertos ajenos.

## Baseline público vigente

Se consultó por el conector GitHub el commit **bc6489cd75c3dff9ff8462b8d0275d52f1045f29** y se comparó con b6ab36a, usado en la evidencia anterior sin agente. La diferencia incluye la política/migración pública V12; **no cambia archivos de contratos**. El informe publicado confirma que PULL, reservas, streaming, documentos firmados1.0/1.1 y ACK mantienen su contrato.

SIGER no implementa retención pública, no depende del agente para COM/ACK y no activa72horas/eliminaciones. La prueba HTTPS entre aplicaciones de e13e5ea sigue siendo evidencia histórica con otro baseline y dobles AV explícitos: no se repitió contra el público actualizado ni se declara tal E2E por inferencia. En este corte no hubo pull real, agente, proveedor gubernamental o correo real.

## Bloqueo y reanudación

El usuario confirmó mantener **100% nativo**, descartó la propuesta híbrida y pidió **conservar el bloqueo/checkpoint por ahora**. El [dictamen](DECISION_DOCUMENTAL_WINDOWS_2026-10-26.md) registra alternativas, pruebas, fallo de capacidad y la limitación AMSI. No se interpreta esta decisión como permiso para cambiar Kaspersky/Defender, liberar memoria deteniendo servicios o rebajar aislamiento.

Cuando se reabra la ventana: comprobar memoria con el ensayo guardado, completar el launcher nativo fijo y AV/inspector/renderizador, probar los ocho controles y después ejecutar los cinco tests reales y el E2E documental. Una sonda exitosa no autoriza por sí sola el modo completo. El runtime actual solo acepta **PRELIMINAR_SIN_DOCUMENTAL** y QA.

Artefacto ensayado: `C:\req\bin\siger-20ce34eea019f4f9.jar`, SHA-256 `20ce34eea019f4f91dc0e4f8ebb8e1870024e466c31a720656d037014b1da7b1`. Los JAR anteriores se conservan. Al corte todos los procesos propios están detenidos y8080/8081 sin listener nuevo.

Reproducción y próximo paso: [checkpoint](CHECKPOINT_RUNTIME_WINDOWS_2026-10-26.md). Evidencia sanitizada: [resultados](RESULTADOS_RUNTIME_WINDOWS_2026-10-26.json). Reversión: [adenda vigente](REVERSION_MIGRACION_WINDOWS.md). U4.5 permanece abierta y producción deshabilitada.
