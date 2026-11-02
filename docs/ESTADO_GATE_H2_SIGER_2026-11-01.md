# Cierre GATE H2-SIGER · 01–02/11/2026

**GATE H2 acreditado para consolidación y traspaso de desarrollo. Producción no habilitada. U5 sigue funcionalmente parcial.** Verificación final el 02/11/2026, continuando el corte del 01/11 sin reconstruir el laboratorio.

## Entrada y referencias

- [Traspaso principal para una sesión nueva](TRASPASO_CODEX_SIGER_H2.md); leer también `AGENTS.md` y los manuales vinculados.
- Main remoto de entrada: `b322c9b905b533605fb53c8d706dbc144ed074a3` (U4.5), verificado mediante fetch sin divergencia antes de consolidar.
- Base U5 conservada: `48dd236368ca7e2229b1342c5a5a86933d9bff5b` y sus cuatro commits posteriores a U4.5.
- Commit de implementación H2: `610bf73a252bb4c58e717e62c801e0af18943aa4`. El código institucional probado se conserva; los commits posteriores completan documentación y corrigen la selección de ejecutables del build cuando PATH contiene varias instalaciones.
- La referencia exacta publicada es el HEAD de `main`, consultable con los comandos siguientes y en el historial de GitHub. El comprobante local de publicación se conserva fuera de Git; el documento no intenta incluir su propio SHA.

```powershell
git rev-parse HEAD
```

```powershell
git ls-remote origin refs/heads/main
```

Formulario H1 publicado: [`bfb950690e85381b69e6dcfac167f0a237c80071`](https://github.com/chashmdai/formulario-web/commit/bfb950690e85381b69e6dcfac167f0a237c80071). No se modificó su repositorio o runtime en H2. Publicar H1 no instala automáticamente su artefacto en la QA compartida.

## Alcance consolidado

U1–U4.5, partes técnicas U5 verificadas y consumidor completo automático 1.0/1.1: HTTPS/CA, reserva/streaming, firmas/huellas/procedencia, originales/COM/ACK, conciliación, exclusión QA y estado técnico. Se conservan campos extranjeros y grupos de NNA sin reinterpretar identidades. V1–V13 permanecen intactas.

H2 añade entrada AGENTS/README, arquitectura y topología correctas, manuales de módulos/datos/roles/buzón, Windows, certificados/DPAPI, PULL/ACK, respaldo/restauración y gates. El build limpio usa Java 25 explícito y temporales privados. La CI declara Windows y Ubuntu; sus ejecuciones remotas se comprueban después de publicar y no sustituyen la calificación documental Windows.

Las tres exclusiones históricas de estrés se mantienen mediante tag `stress`; no se agregaron exclusiones de errores. La regresión MySQL rechaza la QA integrada preservada antes de leer credenciales o ejecutar fixtures. No se cambia la matriz institucional ni se abre producción.

## Pruebas comprobadas

| Verificación | Resultado y alcance |
| --- | --- |
| Clon local nuevo, sin configuración privada, commit `610bf73` | Checkout limpio; `Build-Siger.ps1` correcto en Windows/Java 25. React: **51/51**, lint/build correctos. Backend H2: **264 métodos, 255 correctos, 9 omitidos**, cero errores/fallos. Las omisiones corresponden a workers reales sin configuración en ese build |
| Cadena documental real, ejecución separada | **9/9 correctas, cero omisiones**: cinco métodos de `ServiciosDocumentalesRealesTest` y cuatro de `ServiciosWindowsRealesTest`, con configuración/trabajos/fixtures nuevos, herramientas privadas verificadas y guarda RAM/límites conservados |
| Pérdida de exclusión del consumidor | Dos pruebas nuevas: antes de SQL no crea COM/ACK; después de COMMIT conserva recibo y concilia sin otro folio. Batería HTTP/planificador/bootstrap **29/29**, incluida después en el clon completo |
| Equivalencia con QA activa | **239 clases Java y tres archivos web idénticos por SHA-256** entre el JAR del clon y el artefacto activo; este último no fue sustituido |
| Guardas de regresión MySQL | Rechazo `QA_INTEGRADA_PRESERVADA` comprobado antes de fixtures. **No se ejecutó regresión destructiva MySQL en H2**. Las 245 pruebas MySQL de U5 son evidencia histórica, no resultados nuevos |
| Scripts y documentación | 55 scripts PowerShell parseados sin errores; 66 enlaces locales de 13 entradas comprobados, sin enlaces rotos; rutas Git sin colisiones Windows ni symlinks |
| Publicación | Árbol e historial nuevo revisados sin material privado detectado; binarios, originales, DPAPI y evidencias reservadas permanecen fuera de Git. No se imprime ningún valor coincidente |

No se repitió el E2E ciudadano ni la campaña de aislamiento de ocho controles ya acreditados para la misma cadena; se reutilizan sus evidencias identificadas. No se presentan pruebas con dobles o sondeos vacíos como una nueva recepción real. Los E2E manual y automático previos están en el [checkpoint automático](CHECKPOINT_CONSUMIDOR_AUTOMATICO_QA_2026-11-01.md).

Evidencia privada H2: `C:\req\evidencias\gate-h2-siger-2026-11-01-34d146c29fa24c8d8f364ade6dcab71f`, puntero `C:\req\scratch\gate-h2-siger.txt`. Contiene estado/referencias/diff/untracked previos, clon/logs/Surefire, auditorías sin valores, comparación de clases, SQL y custodia. No se publica esa carpeta.

## QA preservada y disponible

- SIGER continúa en `https://localhost:8081/`; Comunicaciones en `https://localhost:8081/#comunicaciones`, cuenta individual QA existente `nexbyte`, sin reset de contraseña.
- Misma JVM PID **163036**, JAR `C:\req\bin\siger-ae70ab769605c1e9.jar`, SHA `ae70ab769605c1e93eab0de052ceee46f724a7b9dfe684a45227717d6a07c2f0`. Listener solo `127.0.0.1:8081`. Git/build no utilizan ese JAR como destino.
- Formulario local: `https://formulario.defensorianinez.gob.cl/?vista=inicio`. Ambas páginas respondieron **HTTP 200 con TLS validado**, sin omitir confianza o hostname.
- Consumidor automático sigue habilitado, revisión de control **11**, `NORMAL`, aceptar/recuperar activos. Lectura final previa a publicación: **9849 ciclos, cuatro recuperadas/cuatro confirmadas, cero fallos consecutivos y cero invocaciones manuales**. Los contadores avanzan con el proceso; son una fotografía, no valores esperados para reiniciar.
- Auditoría SQL QA de solo lectura: **cero ACK pendientes, cero COM duplicados, cero correos Graph aceptados en QA**, conexión TLS 1.3 con usuario app exclusivamente DML. No se modificaron permisos ni controles.
- Histórico separado: **504 filas en 59 tablas con recuentos y SHA iguales** al manifiesto de transferencia. **24 objetos, 16.869.263 bytes**, SHA-256 y ACL verificados; cero ACE inesperadas. No se sobrescribieron registros, originales o configuraciones.
- La QA `siger_institucional_windows_qa` es integrada y preservada, no descartable. El histórico `siger_institucional_windows_lab` tampoco es destino de fixtures. COM-002497, COM-002498 y recepciones posteriores permanecen.

SIGER, Formulario y el consumidor permanecieron activos. No se generaron Comunicaciones en nombre del usuario ni se ejecutaron llamadas CU/PISEE/Graph en H2. La aplicación pública tiene otro responsable.

## Ramas e historia conservadas

Main se consolida por fast-forward desde U4.5, sin reset, rebase de historia existente o force push. Solo se publica `main`; estas ramas originales permanecen locales sin cambiar su referencia:

| Rama | Referencia conservada |
| --- | --- |
| `integracion-formulario-completo-multimedia-2026` | `3e5af85` |
| `identidad-territorial-laboratorio-2026` | `f665a1c` |
| `notificaciones-tareas-graph-laboratorio-2026` | `78b7caf` |
| `migracion-windows-laboratorio-2026` | `b322c9b` |
| `feature/u5-solicitudes-tareas-independientes` | `48dd236` |
| `preparacion-interoperabilidad-formulario-2026-10-31` | `48dd236` |
| `verificacion-recepcion-sin-agente-2026-10-26` | `e13e5ea` |

La rama nueva `gate/h2-siger-2026-11-02` conserva la consolidación y documentación. Tags y otras referencias no se eliminan. El historial previo de U1–U4.5 y el trabajo sin confirmar recuperado quedan alcanzables desde la consolidación.

## Límites y siguiente paso

[U5 separado](U5_PENDIENTES_INSTITUCIONALES.md): responder, canalizar y escalar Solicitudes siguen cerrados por API `409 DEFINICION_INSTITUCIONAL_PENDIENTE` e interfaz. Publicar su preparación estable en main está autorizado para H2; no equivale a aprobar esas facultades.

La topología destino es DDN-UBUNTU para Formulario y futura VM Windows para SIGER; Windows+WSL es desarrollo en DDN-TEST-SIGER. Nombre/IP/identidad y red de la VM siguen pendientes de TI. [Gates de despliegue](GATES_DESPLIEGUE_SIGER.md) explicita que `HabilitacionNucleo` y `ArranqueWindows` aún son de laboratorio: necesitan un modo institucional revisado, no quitar guardas para arrancar producción.

El siguiente Codex puede reconocer/clonar/compilar sin secretos y preparar la futura instalación leyendo el [traspaso](TRASPASO_CODEX_SIGER_H2.md). El paso posterior es resolver entradas de infraestructura/identidad y los gates autorizados; las facultades U5 requieren definición institucional. No hay habilitación productiva, cutover, servicio permanente, restauración nueva acreditada ni campaña real nueva de proveedores.

## Verificación de CI publicada

La primera ejecución remota de Ubuntu detectó dos ubicaciones válidas de npm en PATH: PowerShell componía ambas como un solo ejecutable. Se corrigió la selección de Maven/npm para tomar la primera coincidencia, respetando PATH. No cambia código institucional, dependencias o configuración de la QA. La ejecución corregida de CI verifica Windows y Ubuntu; resultados y enlaces se conservan en evidencia privada y el historial de Actions, sin presentar una ejecución fallida como aprobada.

La ejecución Windows posterior identificó dos tests de bootstrap que dependían de la resolución hosts particular del equipo de desarrollo. Se aisló esa resolución en un fixture de Surefire mediante `jdk.net.hosts.file` ([mecanismo de pruebas del JDK 25](https://docs.oracle.com/en/java/javase/25/core/java-networking.html)). La JVM de tests solo resuelve loopback; no modifica hosts/servicios del runner, no contacta el Formulario ni relaja la comprobación DNS del bootstrap. El fixture está exclusivamente en src/test/resources y no se empaqueta en el JAR institucional. El código de producción y la QA activa permanecen intactos.
