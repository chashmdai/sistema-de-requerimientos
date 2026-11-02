# Estado GATE H2-SIGER · 01/11/2026

**Consolidación de código estable y traspaso de desarrollo; no habilitación productiva. U5 funcionalmente parcial.** Este registro se completará con los SHA de consolidación y pruebas de clone antes de publicar.

## Referencias de entrada

- [Traspaso principal](TRASPASO_CODEX_SIGER_H2.md).
- Main remoto previo `b322c9b905b533605fb53c8d706dbc144ed074a3` (U4.5), verificado con fetch sin divergencia al iniciar H2.
- Base de trabajo `48dd236368ca7e2229b1342c5a5a86933d9bff5b`, U5 estable/parcial y sus checkpoints.
- Runtime preservado `C:\req\bin\siger-ae70ab769605c1e9.jar`, SHA `ae70ab769605c1e93eab0de052ceee46f724a7b9dfe684a45227717d6a07c2f0`; proceso independiente del checkout. No se sustituyó en H2.
- Formulario referencia H1 publicada `bfb950690e85381b69e6dcfac167f0a237c80071`, consultada en GitHub; la QA integrada puede servir un artefacto anterior, y eso no instala Docker H1 automáticamente.

## Alcance consolidado

U1–U4.5, partes técnicas U5 vigentes y consumidor completo 1.0/1.1 automático: HTTPS/CA, reserva/streaming, firmas/huellas/procedencia, originales/COM/ACK, conciliación, exclusión QA y estado técnico. Visualización de documentos y campos extranjeros/grupos preservada. V1–V13 sin cambios. Tres decisiones U5 siguen cerradas y están [separadas](U5_PENDIENTES_INSTITUCIONALES.md).

H2 añade entrada AGENTS/README, manuales y topología correctos, build limpio sin secretos con temporales privados/Java 25 explícito, etiquetas de las 3 exclusiones históricas de estrés, CI Windows/Linux y rechazo de fixtures MySQL sobre la QA integrada preservada. No cambia los permisos institucionales ni abre modo producción.

## Pruebas y evidencia

- Build aislado inicial: frontend/lint/build correctos; backend 262 métodos, 253 correctos y 9 workers reales omitidos por falta de configuración explícita en ese build. Cero fallos/errores. No se presentan los 9 como aprobados allí.
- Calificación posterior separada con configuración/trabajos/evidencias nuevos y binarios privados existentes: **9/9 workers reales correctos**, sin errores/omisiones, memoria y límites conservados. No utilizó la raíz de trabajos del runtime ni modificó su configuración.
- Se añadieron 2 tests del consumidor: pérdida de exclusión antes de SQL y después de COMMIT; no ACK indebido y conciliación sin otro folio. Su batería/clon final se registrarán al cerrar H2.
- Auditoría inicial: 431 archivos de trabajo y 39 blobs de commits aún no publicados, sin material privado encontrado por las reglas; valores coincidentes no se imprimen. La auditoría final cubre el árbol a publicar y el historial nuevo.
- Guarda MySQL probada: rechazó la regresión antes de leer credenciales/ejecutar fixtures contra la QA preservada. No se ejecuta batería destructiva MySQL en H2. Sus evidencias históricas 245/245 siguen identificadas como históricas.

Evidencia privada H2: `C:\req\evidencias\gate-h2-siger-2026-11-01-34d146c29fa24c8d8f364ade6dcab71f`, puntero `C:\req\scratch\gate-h2-siger.txt`. Incluye referencias/estado/diff/untracked anteriores, logs y resultados. No se publica el contenido reservado de esa carpeta. El [registro automático](CHECKPOINT_CONSUMIDOR_AUTOMATICO_QA_2026-11-01.md) conserva los E2E y ventanas ya acreditados.

## Conservación

Conservar todas las ramas locales enumeradas en el cierre final, incluido U5 original. No se hizo reset/clean/forcepush. Main se integrará por fast-forward si el remoto continúa como se inspeccionó; ante divergencia se revisa y conserva su historia.

Los cambios Git/build no utilizan el JAR activo ni su custodia como destino. El histórico 504 filas/59 tablas y 24 originales sigue fuera de fixtures; la QA integrada tiene sus propios registros/originales y no se restaura/recrea. Se comprobará estado operativo al finalizar sin detener la aplicación.

## Límites institucionales

Preparación de nueva VM, no instalación. Modo institucional/SSO/supervisión/origen remoto, backup restaurado y facultades U5 pendientes figuran en [gates](GATES_DESPLIEGUE_SIGER.md). No hay nuevas llamadas CU/PISEE/Graph, recuperación manual, Comunicaciones generadas por agentes ni cambios de retención pública. La producción permanece cerrada.
