# Entrega progresiva de lógica SIGER · 02/11/2026

## U0

[Diagnóstico inicial](LOGICA_U0_DIAGNOSTICO_2026-11-02.md), [matriz de 138 reglas](LOGICA_MATRIZ_REGLAS_2026-11-02.csv), [baseline de migraciones](LOGICA_BASELINE_MIGRACIONES_2026-11-02.csv). Documento rector verificado y leído íntegramente, sin incorporarlo a Git. G0 limpio `95cf2ae8173b4c6cab3eb8a1531659b8459dd898`; U0 registrado en `0a384cc`.

## Incremento U1.1 · límite regional

REG-DIS-01 y ACC-03: reutiliza `distributionAllowed`, concesiones vigentes, `identity_guard`, idempotencia y transacción existentes. Agrega validación de todos los destinos contra sede de origen antes de ejecutar y bajo bloqueo. No necesita DDL. La coordinación de dos sedes no permite mezclarlas para distribuir; una concesión central independiente sí autoriza el alcance institucional. React recibe el actor vigente y filtra la selección. Se sustituye una expectativa histórica de prueba que permitía distribuir desde A hacia B.

Archivos: `NucleoInstitucional.java`, `Comunicacion.tsx`, pruebas `IdentidadInstitucionalTest`, `IdentidadHttpTest`, `Distribucion.test.tsx` y [contrato incremental](../contratos/logica-institucional-incremental-v1.md). No hay migración ni cambio al Formulario.

Prueba antes del cambio: tres fallos backend acreditaron aceptación indebida o código distinto del 403 esperado; una prueba UI acreditó destino ajeno visible. Se corrigió primero el fixture de dialog de jsdom para distinguir un fallo de infraestructura de prueba de la brecha funcional. Tras implementar: **31/31 backend**, **23/23 frontend**, lint y build React correctos, sin exclusiones nuevas. Datos sintéticos sobre H2 y HTTP con MockMvc; UI con DOM simulado. No se presenta como E2E físico ni como prueba MySQL. Cobertura de origen/destino regional, lote mixto atómico, dos concesiones locales, facultad central independiente, revocación y reintento, HTTPS/CSRF, persistencia/idempotencia. No se cambia COM, folio ni se crean REQ/CAS por distribuir.

## Pendientes y límites vigentes

U1 todavía parcial: faltan autoridad/áreas, reservas, lectura de Direcciones por materia, credencial temporal y otras concesiones; U2–U9 pendientes según U0. SOL con destino único, REQ sin Caso, semáforo REQ, cierres y agenda de equipo aún no están completos. Se conservan bloqueos U5 histórico. Esta entrega no certifica el resto del negocio por haber pasado regresiones históricas.

No se ejecutaron migraciones ni fixtures sobre MySQL, QA/histórico/U4, consumidores reales, nuevos COM operacionales, proveedores, workers reales, cambios a JAR activos, procesos, servicios, infraestructura o worktree U4.5. La compatibilidad se verifica en código y pruebas sintéticas; inventarios físicos H2/U4.4 son evidencia histórica, no auditoría nueva. PE-01–14 siguen acotadas como en el diagnóstico. La incidencia WSL HTTP_503 de G0 no se corrige en esta rama.

## Preguntas institucionales

No se requiere una respuesta para los controles implementados. Las dependencias nominales de autoridad, reserva, calendario, habilitación y aceptación del rector serán necesarias para activar sus capacidades; no se repreguntan las decisiones funcionales ya confirmadas ni se habilita producción por publicar código.
