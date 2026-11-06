# Continuidad autónoma SIGER · 06/11/2026

## Instrucción vigente

La validación funcional fue confirmada por el responsable del proyecto el 06/11/2026. Ver DECISION_VALIDACION_FUNCIONAL_2026-11-06.md. Las 138 reglas conservan sus IDs/orden y registran esa decisión. No pedir nuevamente validación del levantamiento. Mantener identidad, aprobación independiente, ámbito, vigencia y segregación. Parámetros nominales reales ausentes se configuran; no se inventan. Validación funcional no autoriza despliegues.

## Estado actual posterior al PR #6

Rama actual codex/siger-continuidad-funcional, creada con árbol limpio desde 396422d69c29ace39f65ce84a5552b1a45c066f1. PR #6 en borrador, base PR #5; CI 42213485912 APROBADA en ambos jobs: Windows 478/469 aprobados/9 omitidos, Ubuntu 478/454 aprobados/24 omitidos, frontend 119/119, cero fallos/errores. Logs y acta del PR comprobados, Maven 26:12/20:52. No modificar ese HEAD.

Código posterior desarrollado de término de colaboración aceptada: TerminoColaboracion/Controller, V15.11 H2/MySQL, propuesta desde principal, decisión de destino y cierre separado por profesional/jefatura; retirada/rechazo, historial, paquete mínimo, avisos, bloqueo transaccional y protección de estado/revisión. UI en REQ/bandeja/Caso conectada con GET agregados. No descartar cambios posteriores al padre.

Acreditado del corte: H2 25/25 (8 nuevos + 17 continuidad), HTTP/migración 15/15 (áreas y once rutas), MySQL nativo descartable 82/82; proceso propio cerrado y ausencia verificada. Frontend 123 tuvo un matcher no configurado: corregido conservando aserción, cuatro nuevas DOM aprobadas, lint/build correctos. Regresión local integral en curso en colaboracion-integral.log; no se atribuye resultado todavía. Se añadieron después dos tests de revocación y principal transferido: pendientes de focal/CI, sin atribuirlos a los 82 MySQL anteriores.

Publicar este bloque en PR en borrador base codex/siger-equipos-agenda, verificar CI de su HEAD, completar evidencia local y esas dos pruebas. Continuar subrogancias/cierres específicos en rama dependiente limpia con registro firmado explícito 1.4 y migración posterior a V15.11. No detenerse tras aprobar un bloque. Fuentes/reglas validadas; faltantes son implementación y parámetros nominales, no otra validación funcional.
## Git y dependencias

Worktree exclusivo C:\req\worktrees\siger-logica. Rama codex/siger-equipos-agenda, base ef4d378a4c5afb9e23d56b0d4fa93a1b7c2aade5 (PR #5). Cadena PR #2→#3→#4→#5→nuevo PR de equipos/agenda, base feature/siger-u2-continuidad-institucional. Código principal confirmado en 06cc2ec; commit de cierre documental/UI contiene esta versión del checkpoint. Consultar git log -1 para su SHA; no autocitar su propio hash. Sin merge/despliegue.

Commits previos de esta campaña: 41c502d (validación funcional/matriz/checkpoint), 0985802 (checkpoint recuperable), 06cc2ec (equipo, agenda, V15.10, TAR-09, espera y pruebas). Padres/main/U4.5 conservados.

## Capacidades desarrolladas y focalmente acreditadas

Equipo local del Caso con facultad vigente de área, principal y colaboradores; retiro con resolución transaccional de pendientes; historia y revocación efectiva. Contenido ordinario autorizado sin acceso automático a COM raíz, otros Casos, FIR, documentos históricos sin acreditar o documentos ajenos sin clasificar. Alias de la misma persona no hereda pertenencia.

Agenda compartida V12/V13: ejecutor único, creador/asignador, campos opcionales, obligatoriedad, intentos, reprogramación, cancelación fundada y cumplimiento manual. TAR-09 transfiere todas las no terminales (también de colaboradores), conserva autor/fechas/ventanas/intentos, no duplica tareas y actualiza recordatorios. Acuse manual separado. Cierre comprueba obligatorias y revisión actual, conservando modalidad DERIVACION después de cambios de agenda. Pos cierre no admite alta/intentos nuevos; ordinarias previas se regularizan sin reabrir o fabricar actuaciones.

Espera fundada con decisión de jefatura y reanudación expresa, sin suspender responsabilidades/relojes. Indicador y filtro EN_ESPERA con ámbito en listados/conteos/detalles. Interfaz real Equipo y agenda, historia visible y avisos internos recuperables sin correos reales.

## Persistencia y pruebas

V15.10 Java por dialecto H2/MySQL, DDL común/checksum explícito; sustituye exclusivamente CHECK personal histórico en CASO. Sin editar V1–V15.9, outOfOrder, backfill o datos nominales. Java 25 explícito, Node 24, Build-Siger aislado.

- H2 selección 43/43 intermedia. Integral local posterior: 474 métodos, 463 aprobados, dos fallos de expectativas HTTP históricas, nueve omitidos, cero errores; NO se presenta como verde.
- Selección final ampliada: 63 métodos, 62 aprobados y una negativa que llegó primero al bloqueo de clave temporal. Se completó el cambio real de clave del nuevo fixture; HTTP final 8/8. Agenda 16/16, documentos finales 3/3, equipo HTTP/migración y continuidad/áreas/tareas aprobados en las selecciones correspondientes. No sumar solapamientos como métodos únicos.
- Frontend final 119/119, lint y build TypeScript/Vite correctos. Se corrigieron dos tipos opcionales de pruebas/historia sin relajar aserciones.
- MySQL nativo Windows 8.0.46 descartable FINAL 73/73, cero fallos/errores/omisiones; incluye V14/V15.1…V15.9→V15.10, conservación histórica, agenda 16 y rollback/concurrencia/espera/derivación. Proceso propio cerrado y ausencia verificada. Cortes 9/9 y 69/69 conservados.
- Un intento MySQL no ejecutó tests por error de compilación al extraer un auxiliar de fixture; corregido antes de la ejecución final. Logs preservados.
- CI Windows/Ubuntu del HEAD publicado: verificar/registrar en acta del PR. No reutilizar PR #5 ni éxitos de otro SHA. Presupuesto 30 minutos y exclusiones/omisiones históricas intactos.

Evidencia privada: siger-build-4c652bfd9b7a41d5a8e0d659e180202d y MySQL final siger-autoridad-mysql-cb2111e6377f4bb891d6a224bc74af4b bajo LOCALAPPDATA. Informe LOGICA_EQUIPO_AGENDA_2026-11-06.md identifica intentos, pruebas y límites. H2/MockMvc/DOM/MySQL no son E2E físico; AV/renderer con dobles declarados. Guion de navegador preparado, no ejecutado.

## Siguiente acción precisa

Confirmar documentación/matriz/UI final, push normal y crear PR en borrador dependiente de #5; verificar CI de su HEAD en Windows y Ubuntu. No detenerse al aprobar A: continuar en rama dependiente limpia con término de colaboración aceptada (EQU-03 pendiente técnica), cierres/subrogancias/continuidad excepcional (B) y después C/D/E.

Para B: evolucionar explícitamente el registro firmado (nueva versión; no reinterpretar 1.0–1.3) con función/ámbitos explícitos, titular, candidatos y autoridades configurables. Solicitud de persona preidentificada, aprobación independiente, intervalos finitos/revisión, prórroga nueva, revocación/retorno, exclusión de facultad simultánea y conservación de cartera/tareas. Revisar integración de bandejas y autorizaciones pendientes ligadas a funciones, sin dar acceso global ni ejecutar TAR-09 al activar/terminar. SUB-CONT-01 por Caso y autocierre regional titular requieren circuitos específicos, no eliminar por atajo la independencia del cierre ordinario. Estos son pendientes técnicos de implementación, no de validación funcional.

## Pendientes y preservación

EQU-03 después de aceptación: circuito desarrollado y acreditado focalmente en la continuación actual; completar CI/informe del HEAD. Subrogancias/autocierre regional/continuidad excepcional, catálogo territorial acreditado, conciliación explícita, antecedentes REQ sin Caso, FIR de actuaciones/reservas adicionales, plazos, búsqueda/reportes/exportaciones generales y cierre E2E siguen pendientes técnicos. Cuentas/mandatos/cobertura reales no cargados. SMR, QA, U4.4/U4.5, COM-000001, bases históricas, JAR/servicios, Formulario, proveedores, contratos/firmas/originales y COMMIT→ACK intactos. Sin operaciones reales ni E2E físico. Árbol debe quedar limpio tras el commit de cierre; cualquier avance posterior se conserva y se recupera mediante status/diff.
