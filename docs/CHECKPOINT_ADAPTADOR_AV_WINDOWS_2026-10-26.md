# Checkpoint U4.5 — AV Java real0/1/2, refuerzo de Job por acreditar

> Este corte quedó superado por el [checkpoint final U4.5](CHECKPOINT_U4_5_WINDOWS_2026-10-26.md). JOB_LIST real, cinco documentales, ocho controles, regresiones y Edge acreditados. Se conserva íntegro el contenido anterior; producción sigue deshabilitada y U5 no se inició.

26/10/2026. `C:\req\repo`, rama `migracion-windows-laboratorio-2026`. Código **2a6c6d2**; documentación en el commit posterior consultable con git log. Sin push. No iniciar U5 ni habilitar runtime documental.

## Lo comprobado

- Se corrigió un fallo real de herencia de ACL: SetFileSecurity no propagaba el permiso al scanner/DLL/bases existentes. SetNamedSecurityInfo sí lo hace y revierte los permisos temporales.
- El adaptador Java corrigió el fallo0xC0000022 y completó **Clean0/EICAR1/Unavailable2**, con límites originales, token sin capacidades, Job de un proceso y limpieza completa. Evidencia `C:\req\testdata\av-java-e89ba516a4b649b2aa75dc6c9d27f290`.
- Cinco guardas correctas; test de dos JVM/raíces con mutex correcto y sin scanner. Históricos y24objetos intactos;0ACE inesperadas. No hubo interacción con MySQL institucional, WSL, Kaspersky o Formulario Público.

## Diferencia importante con el código actual

Después de esa pasada se implementó asociación del Job **durante CreateProcess**, eliminando la ventana previa a AssignProcessToJobObject. El código compila, pero esta asociación atómica todavía no tiene una pasada real: la RAM volvió a bajar bajo4GiB. No declarar la implementación definitiva calificada por extrapolación.

La prueba original JUnit `controlAntivirusRealSeRechaza` fue ejecutada dos veces y ambas dieron **1error de memoria,0éxitos,0omitidas**. Sigue pendiente, igual que las otras cuatro. No contar el harness0/1/2 como aprobación de esos cinco métodos. No se repitieron regresiones completas.

## Próximo paso exacto

1. Revisar Git y procesos propios, sin tocar los ajenos. Conservar todos los intentos y no borrar `pending` a ciegas. El fallo más reciente de memoria registró3.742.486.528bytes disponibles frente a4.294.967.296; no se debe saltar la guarda ni liberar memoria deteniendo servicios.
2. Con margen, ejecutar una vez el ensayo0/1/2 con el código actual y comprobar `jobAtCreation=true`, stdout/stderr, firma EICAR, ACL, perfiles y ausencia de huérfanos. Ambos preflights y el adaptador mantienen4GiB; Job1,5GiB/45CPU/90segundos/un proceso y heap256MiB no cambiaron.
3. Ejecutar el método JUnit real mediante el launcher ya preparado, que utiliza JUnit6.0.3 existente sin mantener Maven en memoria. Está compilado, pero su última ejecución se detuvo antes del scanner. Si el error es de recursos conservarlo como bloqueo; si es de API/archivo diagnosticarlo con las salidas privadas.
4. Completar negativos del perfil Java: lectura, escritura, red, hijo arbitrario, recursos, timeout/crash, recuperación de journal y rechazo sin publicación. El cierre automático del Job no sustituye la conciliación de ACL/journal tras un aborto de la JVM. No declarar recuperable esa parte aún.
5. Solo después continuar inspector y renderizador Windows y los otros cuatro métodos, sin parsers con el token del backend. Poppler local está inventariado, no desplegado; qpdf/ffprobe/ffmpeg no están en PATH. Mantener 100% nativo y un conjunto pequeño de herramientas fijas, privadas y verificadas.
6. Cuando la cadena esté acreditada, corresponderán las regresiones H2/MySQL QA/frontend y E2E documental. El launcher institucional actual todavía fuerza documental=false; conservarlo así.

Comandos de una línea:

```powershell
& C:\req\repo\scripts\qa\Windows-Probar-Antivirus-Java.ps1
```

```powershell
& C:\req\repo\scripts\qa\Windows-Probar-Control-AV-Real.ps1
```

El segundo reutiliza el classpath del XML Surefire y requiere el launcher de la misma versión del engine, ya presente. Tras cambios futuros compilar los tests con Java25 primero; no ejecutar un JAR activo desde target ni mezclar versiones JUnit automáticamente.

## Evidencias y estado

- Éxito Java0/1/2 y logs: `av-java-e89ba516a4b649b2aa75dc6c9d27f290` bajo testdata.
- Fallo diagnosticado conservado: `av-java-f226a15977fa4323a9224707e9ed05c1`.
- Mutex: `av-mutex-ff86b24abe7548c1bd8fa829be43bd89\resultado.json`.
- JUnit/Surefire bloqueado: `av-junit-b15c7a0cf3534270b7ab57f9e1f00513\junit.xml`, `intento-forked.log`; log final en `C:\req\logs\av-real-junit-windows.log`.
- Launcher JUnit compilado: `av-compile-ed55359c69fc43ae94058e790bb67374`.
- `C:\req\evidencias\custodia-adaptador-av-2026-10-26.json` y `acl-adaptador-av-2026-10-26.json`.
- [Informe](REANUDACION_ADAPTADOR_AV_WINDOWS_2026-10-26.md), [JSON](RESULTADOS_ADAPTADOR_AV_WINDOWS_2026-10-26.json) y [reversión](REVERSION_MIGRACION_WINDOWS.md).

Procesos propios detenidos al corte, sin servicio/tarea permanente, correo real, pull público, nuevo JAR de ejecución o cambios al host. Conservar esquema histórico, custodia y entorno original. U4.5 abierta; producción deshabilitada.
