# Continuidad funcional · término de colaboración aceptada

## Alcance y corte

Rama `codex/siger-continuidad-funcional`, dependiente del PR #6 en `396422d69c29ace39f65ce84a5552b1a45c066f1`. Conserva toda la cadena PR #2→#3→#4→#5→#6. La validación funcional del 06/11/2026 rige; los pendientes se clasifican como código/pruebas/configuración efectiva, sin solicitar otra aprobación del levantamiento.

Completa el circuito posterior a aceptar una colaboración: principal propone término, destino decide, responsable del Caso solicita cierre y su jefatura confirma; REQ se cierra separadamente. Persiste historia, estado, fundamento y autorías. Equipo, agenda, Caso, originales y colaboración aceptada permanecen intactos al decidir la propuesta. No se confunde retirar petición pendiente con terminar intervención aceptada ni se otorga acceso al expediente mixto a su principal.

Backend `TerminoColaboracion`, controlador, ampliaciones acotadas de U2/continuidad/cierre y avisos; React `TerminosColaboracion`, propuesta desde REQ, bandeja/Caso e historia conectadas. V15.11 añade únicamente tabla e índices/relaciones, con DDL idéntico H2/MySQL y sin backfill. V1–V15.10 intactas. Las referencias publicadas observadas de main/U4.5 no contenían versión posterior; comprobar cambios no publicados al integrar, sin tocar ese worktree.

## Seguridad y consistencia

Ámbitos vigentes de principal y destino, área explícita, denegación por backend y autorización antes de estado/revisión para evitar vías indirectas. Profesionales del equipo consultan sin sustituir jefatura. Reintento idéntico no duplica; otra carga con la misma clave entra en conflicto. Aceptar y retirar compiten bajo identidad/COM. Fallo de historia revierte decisión y revisión REQ. No se genera actuación, tarea, nuevo Caso/REQ o acuse por estas decisiones. Una propuesta pendiente exige decisión antes de confirmar cierre; aceptación mantiene controles ordinarios y revisión independiente de persona.

Los avisos se proyectan una sola vez por evento y destinatario autorizado. Las denegaciones de destinatarios se convierten en resultado negativo dentro de la comprobación transaccional, evitando marcar la proyección como rollback-only al filtrar una cuenta sin permiso. No se suprimen errores de persistencia ni se envían correos reales.

## Pruebas y evidencia

- H2 dominio/continuidad **25/25**: propuesta, aceptación sin efectos colaterales, rechazo/retirada/nueva petición, cierre posterior explícito, negativos, idempotencia, carrera, rollback y avisos. Primero se detectaron ocho errores por un código de recurso demasiado largo: se corrigió el código, sin ampliar columnas históricas. Después un error de transacción anidada al filtrar avisos: corregido preservando autorización y rollback de errores reales. Logs conservados.
- H2 HTTP/migración **15/15**: sesión/CSRF reales de prueba, AREA_A→AREA_B, principal sin expediente, profesional sin facultad de decidir, TI sin bandeja, propuesta→decisión→solicitud/confirmación de cierre y REQ todavía activo; once rutas V14/V15.1…V15.10→V15.11 y DDL/registro histórico conservados.
- MySQL nativo Windows 8.0.46 descartable **82/82**, cero fallos/errores/omisiones. Incluye comprobaciones finales que deniegan estado/revisión a TI antes y después de decidir, once clases de reglas/migraciones y rutas desde V15.10. Instancia propia cerrada; verificar ausencia en acta de cierre. No es MySQL operacional ni Ubuntu.
- Frontend: selección completa 123 con un error de aserción por un matcher no configurado; las otras 122 aprobaron. Se conservó la misma aserción usando Chai disponible; **cuatro nuevas DOM aprobadas**, lint y build correctos. Regresión integral siguiente verifica los 123 sobre el corte publicado; no se suman focales.
- Se añadieron después dos pruebas específicas de revocación de origen y cambio de principal: deben acreditarse en focal/CI posterior, sin atribuirlas al corte MySQL 82.
- Regresión integral local del bloque en ejecución, log privado `colaboracion-integral.log`; todavía no se atribuye resultado. CI Windows/Ubuntu del HEAD publicado se acredita en acta del PR dependiente de #6, con SHA y ambos logs; no reutilizar CI del padre.

Antecesor PR #6 acreditado: [CI 42213485912](https://github.com/chashmdai/sistema-de-requerimientos/actions/runs/42213485912), HEAD `396422d`, Windows 478/469 aprobados/9 omitidos y Ubuntu 478/454 aprobados/24 omitidos, frontend 119/119, cero fallos/errores, Maven 26:12/20:52 respectivamente. Esa evidencia corresponde al padre, no al código nuevo.

Evidencia privada bajo LOCALAPPDATA: carpeta de campaña `siger-build-4c652bfd9b7a41d5a8e0d659e180202d`, MySQL `siger-autoridad-mysql-e3aa63efd46b43fea379766f73ce3429`. H2/MockMvc/DOM/MySQL descartable no son E2E físicos. Dobles/omisiones y stress heredados conservados; sin cambios a timeouts funcionales, presupuesto CI o dependencias.

## Matriz, demostración y continuidad

EQU-03: cobertura de propuesta/aceptación inicial y cierre posterior de intervención, conjuntamente con equipo/agenda del PR #6. Actualiza también CAS-07, CAS-CIERRE, AUD-01/03 y NOT-07, sin declarar terminada la totalidad de esas reglas compuestas. La matriz conserva 138 IDs/orden y validación funcional.

[Contrato](../contratos/termino-colaboracion-institucional-v1.md). Guion: preparar los roles sintéticos de equipo/agenda, crear REQ en AREA_A, aceptar Caso colaborador en AREA_B; principal propone desde REQ sin poder leer Caso destino; jefatura B acepta manteniéndolo abierto; profesional B solicita término y jefatura B confirma; principal A cierra REQ expresamente. Variantes: rechazo/retirada, dos pestañas, revocación y consultas de TI/otro ámbito. Registrar esperado/observado e IDs privados. **Preparado, no ejecutado como navegador físico.**

Siguiente bloque: subrogancias y cierres específicos con evolución versionada del registro firmado, solicitud/aprobación independiente/ejecución técnica, vigencia, prórroga/retorno y autorización por Caso. Son requisitos validados pendientes técnicos, no una espera de nueva validación institucional.

QA, U4.4/U4.5, SMR, COM-000001, datos preservados, JAR/servicios, Formulario, proveedores y contratos/originales/firmas/COMMIT→ACK intactos. Sin merge, despliegue, correo real, migración operacional o E2E físico.
