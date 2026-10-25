# Checkpoint — segunda unidad documental, 24/10/2026

**Unidad cerrada en laboratorio.** Imágenes y PDF se previsualizan dentro de SIGER, con páginas y zoom. El ciclo de documentos de gestión está operativo y separado de la custodia ciudadana. Producción no habilitada.

- Repositorio: `/home/nexbyteproveedor/proyectos/sistema-de-requerimientos`.
- Rama conservada: `integracion-formulario-completo-multimedia-2026`.
- Base de recepción conservada: `1c74688`, `ec98dbf`, `29428e0`.
- Implementación de esta unidad: `2e54edd`.
- El commit que contiene este checkpoint añade contrato, informe, resultados y reversión. Consultar `git log -2 --oneline`.

## Completado y verificado

1. Visor autorizado de PNG/JPEG/WebP y PDF: PNG derivado, navegación hasta 100 páginas, zoom 50–300%, carga/reintentos/errores y descarga opcional. Se probaron las tres páginas de un PDF real en escritorio y móvil al 200%, sin descarga manual del original.
2. Gestión funcionaria: carga privada por streaming, ClamAV real e inspección efectiva, SHA, autor/fecha del servidor, idempotencia, vínculo con Caso/actuación, consulta, descarga e historial del contexto.
3. Un original por documento: referencias entre Casos/actuaciones no copian binarios. Los originales ciudadanos, sus descriptores y sus comprobantes permanecen inmutables.
4. Política de autorización por recurso reutilizable, con adaptador a reglas actuales. No se fijó la matriz futura de TI, equipos, cargos o subrogancias.
5. V10 nueva en H2/MySQL; V1–V9, contratos públicos, firmas, COM y ACK intactos. Mismo esquema y almacén; mismos workers, sin servicios ni contenedores nuevos.

Regresiones finales: **142 H2, 142 MySQL, 26 frontend**, lint/build correctos. Los cinco tests de workers reales se ejecutaron expresamente, sin omisiones. Se mantuvieron las tres exclusiones históricas de carga/concurrencia. No hubo estrés ni llamadas gubernamentales.

Pruebas reales: carga/reintento, PDF multipágina, imágenes, zoom, actuación, referencia a otro Caso, lector distinto del creador, historial contextual, SHA de descarga, accesos negativos, CSRF, origen manipulado, contenido inválido y antivirus. Repetidas tras reinicio institucional sin duplicar registros.

## Estado conservado

- HTTPS institucional loopback 20443, configuración privada y JAR activo fuera de target en `/home/nexbyteproveedor/.local/share/siger/institucional-completo-lab`.
- Esquema integrado `robust_inst_formulario_lab`, exclusivo de pruebas `robust_inst_test`; Flyway en V10.
- `institucional.documental.habilitada=true`. Recuperación pública automática sigue desactivada.
- Siete Comunicaciones previas y dieciséis originales ciudadanos comprobados sin cambios; cero ACK pendientes.
- Dos originales de gestión y tres vínculos, seis páginas PNG derivadas. Dos Casos y un Requerimiento de QA creados expresamente con comandos funcionarios sobre el ingreso sintético existente.
- Temporales activos: cero al cierre; originales, páginas y metadatos íntegros.
- Evidencias privadas: `.local/evidencias/documental/`; estado reanudable en `estado.json` y baseline en `baseline-custodia.json`.
- `formulario-web` limpio; no se modificó código/configuración privada pública ni se hizo push.

El cliente Windows devolvió EPERM leyendo el fixture de control antivirus. Se conservaron sus protecciones y se comprobó el rechazo real 422 por HTTPS desde WSL. No se cambiaron Windows ni políticas de seguridad.

## Límites y continuidad

Gestión: máximo 25 MiB por archivo. Perfil PNG: hasta 1600 px/6 MiB por página; PDF hasta 100 páginas. Office se valida y descarga; no se ejecuta ni convierte. La auditoría registra solicitudes, no lectura humana terminada; el historial consulta los últimos 100 movimientos del contexto y conserva el resto en MySQL.

Próxima unidad, cuando corresponda: incorporar decisiones confirmadas de TI/equipos/subrogancias y auditoría transversal; campaña independiente de capacidad, retención, respaldo/recuperación y hardening conjunto. No ampliar facultades a partir de los roles sintéticos de laboratorio.

Para retomar: inspeccionar Git y este checkpoint, reutilizar esquema/custodia/workers y los scripts institucionales. No reconstruir recepción, no recrear volúmenes, no modificar migraciones históricas y no publicar cambios.

[Informe](CIERRE_DOCUMENTAL_2026-10-24.md) · [Resultados](RESULTADOS_DOCUMENTAL_2026-10-24.json) · [Contrato](../contratos/documental-institucional-v1.md) · [Reversión](REVERSION_DOCUMENTAL.md).