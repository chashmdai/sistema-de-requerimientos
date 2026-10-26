# Matriz documental Windows — calificación U4.5

26/10/2026. Código de runtime `2db9783`, artefacto `97ec9558588182cdb67b68c8548ecf8fd5d6c0beb20c97aad7f2e9399c9270ea`. QA sintética, Windows Server 2022. No es certificación productiva.

Se distinguen tres niveles: **sonda** ejecutable sintético bajo el mismo lanzador Java; **worker real** ClamAV/qpdf/Poppler/FFmpeg/Java modular; **pipeline real QA** backend, HTTPS, MySQL, NTFS y Edge. Un canario aprobado por sí solo no acredita que los parsers o la aplicación funcionen.

| Control | Sonda negativa real | Worker/pipeline definitivo | Resultado |
|---|---|---|---|
| 1. Lectura | AppContainer no lee un señuelo privado externo existente. La lectura de su propia entrada funciona. | Todos los parsers reciben una copia privada de un único archivo; el token se verifica en cada creación. Los paquetes contienen herramientas, no secretos del backend. | APROBADO |
| 2. Escritura | No puede escribir fuera del trabajo, modificar la entrada ni ampliar la DACL del señuelo. | Entradas solo lectura; salida dentro de `work`; PDF/imágenes derivadas reales. Las DACL se propagan y retiran sobre archivos existentes. | APROBADO |
| 3. Red | TCP IPv4/IPv6 y UDP loopback rechazados; TCP externo a dirección resuelta de `www.microsoft.com:443` rechazado. Hay controles positivos previos desde el padre; no se envían datos documentales. | AppContainer sin capacidades; FFmpeg limita además protocolos a file/pipe. No hay cambios de firewall ni excepciones de loopback. | APROBADO |
| 4. Procesos | Intentar crear un segundo ejecutable produce denegación/cuota. | Job de un proceso, consultado antes de reanudar; JOB_LIST asociado durante CreateProcess, sin ventana de hijo fuera del Job. No hay shell ni API de ejecución arbitraria. | APROBADO |
| 5. Recursos | Commit pequeño permitido, commit de 2 GiB rechazado; proceso CPU terminado con `0xC0000044`; exceso de salida rechazado. | AV 1,5 GiB; parsers 512 MiB; 45 s CPU y 90 s de reloj. Límites efectivos consultados en cada operación. Office añade heap 128 MiB. Salidas se vigilan, no se aceptan ni capturan por encima del perfil; no se afirma una cuota NTFS de disco instantánea. | APROBADO |
| 6. Interrupciones | Crash exit37, timeout, interrupción de thread, exclusión por mutex y aborto de JVM recuperados. | Kernel Job elimina al hijo al cerrar la JVM; journal con PID+creación restaura ACL y perfil al reiniciar. Cero jobs y procesos propios remanentes al final de los ensayos. | APROBADO |
| 7. Fallo seguro | Un journal incompleto falla cerrado; no se borra automáticamente. | AV real indisponible exit2 lanza error; en HTTPS, indisponibilidad controlada del AV y del inspector devuelve503 y no crea documento. Error de parser/estructura nunca se considera contenido limpio. | APROBADO |
| 8. Rechazo/publicación/ACK | No se usan simuladores como AV del runtime. | EICAR, PDF activo, extensión falsa y Office externo son rechazados422 sin aumentar documentos publicados. Contratos JDBC/HTTP controlados verifican cero COM/ACK ante firma falsa, transferencia rota o original alterado. El ACK público exige evidencia íntegra y COMMIT; no se modificó esa secuencia. | APROBADO |

## Evidencia

- Sonda final: `C:\req\testdata\calificacion-doc-05856ea6cf8c4675bd9958e1d87e3d01\qualification.json`; copia `C:\req\evidencias\calificacion-documental-java-final.json`. Nueve escenarios correctos. El JSON conserva su alcance `CANARIO_LANZADOR_JAVA_NO_PARSER` y `fullU45Closed=false` porque es evidencia de una parte, no un cierre autónomo.
- AV Java final: `C:\req\testdata\av-java-f38b6cc64f604240aaf5203e0c7f2232\resultado.json`, preflight, stdout/stderr privados. Clean0, EICAR1, Unavailable2. Picos privados aproximados de1,04GB en los dos primeros; límites1.610.612.736bytes, proceso1, capacidades0, Job al crear y limpieza completa.
- JUnit AV dedicado final: `C:\req\testdata\av-junit-0120e08aeef440a092231f6eb6ac4b57`,1/1.
- Cinco documentales históricos y cuatro adicionales: incluidos sin omisiones en229 H2 y229 MySQL QA. Los parsers reales comprueban Office/TXT, PDF multipágina, PNG/JPEG/WebP, MP3/WAV/M4A/OGG y MP4/MOV/WebM, además de ZIP activo/traversal/symlink/XXE, referencias externas, texto binario, PDF roto y perfiles incompatibles.
- Edge definitivo: `C:\req\evidencias\edge-documental-operacion-u45.json`127; `edge-documental-reinicio-u45.json`110; `edge-territorio-u45.json`34. La falla segura en pipeline usa un marcador QA sin journal sobre la raíz exacta del worker, retirado en finally; no sustituye sus binarios ni sus límites.
- `C:\req\evidencias\candidato-documental-u45.json`:225 clases del JAR idénticas al candidato probado, raíces y manifiestos. `parada-documental-u45.json`: cero workers, jobs y listeners QA al terminar.

Los contratos de recepción utilizan productor/transporte controlados para casos negativos. La preparación de cuatro ingresos para el navegador usa documentos y comprobantes firmados reales, con **productor y aceptación del ACK simulados en proceso**. No es una nueva prueba E2E contra la aplicación pública publicada. La aplicación pública, el agente de respaldo, ClaveÚnica, SRCeI y Graph real no fueron llamados.

## Límites y mantenimiento

El AppContainer necesita leer DLL y archivos públicos del sistema y su paquete permitido; el control no promete prohibir esas lecturas necesarias. El señuelo privado acredita la denegación de acceso no autorizado. Los parsers reales usan la misma creación de token/Job/handles, no el ejecutable canario.

El reinicio verificado es de JVM. No se reinició Windows ni se simuló un corte eléctrico. NTFS utiliza barreras y conciliación verificadas; no se equipara una prueba funcional a una garantía del hardware de almacenamiento. Un estado desconocido conserva fallo cerrado y evidencia para intervención.

Paquetes terceros, bases AV y JDK necesitan actualizaciones verificadas. No se autoriza producción, confianza adicional de máquina, servicios permanentes, ampliación de exclusiones Kaspersky ni acceso a datos reales para futuras pruebas. Consultar [operación](OPERACION_DOCUMENTAL_WINDOWS.md) y [reversión](REVERSION_MIGRACION_WINDOWS.md).
