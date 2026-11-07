# Ingreso efectivo y territorio · 06/11/2026

## Corte recuperable de primera gestión

NucleoU2.PrimeraGestion, ruta privada explícita `/primera-atencion/gestion` y formulario conectado. COM+REQ finalizado sin Caso conservan recepción, respuesta efectiva, registro del servidor y resultado ORIENTACION/RESPUESTA_DIRECTA/REMISION_CONCLUIDA. Reutiliza la atención U2, metadatos V15.16 y bitácora; no duplica módulo o relato. No hay migración nueva para esta parte.

Sede de captura acreditada en cuenta separada del principal elegido; no inventa un lugar cuando falta. TI/otra unidad denegados. Fechas futuras, anteriores a recepción, resultado ACUSE, versión desconocida y respuesta vacía no crean COM/REQ. Captura tardía conserva hecho real. Idempotencia concurrente, original/folio y rollback de COM/REQ/historia/comandos verificados. Ruta antigua queda limitada a reintentos confirmados sin cambiar el hash previo.

Pruebas acreditadas por cortes:

- H2 dominio8/8,40s, log ingreso-gestion-focal-1.log.
- Ampliación27:25aprobadas/1fallo HTTP/1error HTTP; dominio8 y regresión U2 17 aprobaron. Fixture HTTP suponía sedeA aunque la cuenta tenía captura sin sede configurada, y ordenaba por columna seq inexistente en communication_registry. Corregidos a sede real de cuenta y columna numero; no se alteró autorización para permitirlos.
- HTTP corregido2/2,17s: login/CSRF reales, tipo/fechas/REQsinCAS e idempotencia; legacy nuevo409 y reintento confirmado200. No es navegador físico.
- **MySQL nativo Windows8.0.46 nuevo descartable8/8**,36s, cero fallos/errores/omisiones. Instancia228cd3236bc74daf80bde3fb43337e63 cerrada/proceso propio0. Datos sintéticos, sin QA/históricos/proveedores.
- DOM Distribución9/9 adaptado al formulario real. Regresión frontend completa/lint/build y CI del nuevo hijo pendientes al corte; no se atribuyen158/CI del padre a esta interfaz cambiada.

Contrato [primera-gestion-institucional-v1](../contratos/primera-gestion-institucional-v1.md). Evidencia privada en campaña siger-build-4c652bfd9b7a41d5a8e0d659e180202d, logs ingreso-gestion-*. No envíos reales, E2E físico ni cambios operacionales.

## Territorio desarrollado en corte local

CatalogoTerritorial y registro firmado1.8 acreditan propuesta org_coverage existente con huella semántica, región/sede/periodo/ref/hash/estado. Root1.0–1.7 no aceptan campo nuevo y conservan contratos. Sin cobertura vigente nueva asunción regional utilizable falla cerrada. Excepción fundada tipada por domicilio ausente/contradictorio/extranjero/inutilizable exige fundamento y referencias, sin borrar declaración. Central no se regionaliza por domicilio. Asunción de SOL y propia jefatura conectadas a API/UI, preflight propio comprueba autor/jefatura/área. TI/materia/otraárea no deciden.

La constancia territorial inicial usa historial transaccional existente, formato1.0, mismo REQ; no modifica Casos ni recalcula principal al cambiar catálogo. Se reutiliza authority_batch para certificados firmados y institution_flow_history para constancia, sin tabla/index/catálogo duplicado. Decisión técnica posterior al diseño preliminar: NO V15.17, conservaV1–V15.16 y evita migración innecesaria. Ningún backfill de histórico; ausencia de constancia explícita.

H2 focal29/29 (territorio11+U2 17+upgradeBitácora1),02:45. HTTP posterior4/4 (territorio2+ingreso2),31s. DOM14/14 (territorio5+distribución9). MySQL nativo Windows8.0.46 descartable **13/13** (12territorio+1compatibilidad1.7→1.8), cero omisiones/fallos/errores, instanciaf606251577c84852987fb16ef936d4e8 cerrada/proceso propio0. La actualización de contrato preserva fila/hash/firma1.7 y concesión documental vigente; no concede Caso/alias y conserva distinción materia funcional/unidad. **Frontend completo163/163,lint/TypeScript/Vite aprobados**. **H2 final4/4** (central1+compatibilidad1+HTTP2),37s, ci propia pendiente. No se atribuye CI del padre al hijo.

## Dependencia CI del padre

PR #11/35b27af corrigió límite de ciclo de vida H2:15/15 local y MySQLupgrades2/2. NuevaCI42316699961 aprobó Windows2/3 yUbuntu2; Ubuntu3 falló177/1aserción/0errores en fixture PISEE que esperaba429 aunque la respuesta anterior excedió250ms desde reserva. Sin PISEE real. Log completo privado1268476bytes,SHA256dc8906feecb7a8c83912c0fb2d8a3177b454a6e2f79d7ed164573f5e26551b65. Se preserva este corte local antes de volver al padre para hacer determinista la fuente temporal sin cambiar límite funcional. CI padre e hijo nuevas obligatorias.

Al completar138 o límites concretos se ejecutará revisión transversal/consolidación/candidata. Todavía hay funciones independientes programables; sinmerge/despliegue/operaciónreal.

## Integración y trazabilidad

Contrato [territorio-institucional-v1](../contratos/territorio-institucional-v1.md). Backend CatalogoTerritorial/AutoridadesInstitucionales/NucleoU2/Controller, preflight propio auditado; frontend DecisionTerritorial y vistas SOL/asunción propia/REQ conectadas. Reutiliza módulo territorial, autoridad privada e historia: ninguna migración nueva, V1–V15.16 intactas. Branch codex/siger-ingreso-territorio dependiente de PR #11/39df7b9, conserva commits cd0adc9,d62b8c4 y merges controlados40bf7d2/81aaf1f. No se han publicado cambios de los padres anteriores a #11.

Matriz138 conserva IDs/orden/validación06/11: ING-03,SOL-02/03,DIS-02,REG-TER-01/02 y evidencia relacionada BIT/PLA/ORG-12/DIS-05/08. No redefine reglas antiguas como completas por parametrizar; distingue mecanismo probado y valores nominales no cargados. Persisten capacidades programables: no revisión/candidata final prematura.

## Guion de aceptación por roles, preparado y no ejecutado

1. Entorno nuevo descartable, personas/cuentas/roles/áreas sintéticos mediante aprobación independiente; raíz de confianza de prueba. Preparar región/sedes existentes y cobertura propuesta por TI, sin datos operacionales.
2. Funcionario registra teléfono con recepción/gestión efectivas diferentes, resultado ORIENTACION y respuesta concreta. Comprobar COM+REQ finalizado sinCAS/SOL, bitácora con ambas fechas y ejecutor. Un ACUSE no sustituye respuesta. Reintentar conserva folio y resultado, no duplica.
3. COM web sintética local→central→SOL regional con domicilio del requiriente clasificado. Antes de certificado, sede no asume por tener rol. Firmar registro1.8 del contenido exacto de cobertura y comprobar orientación acreditada; jefatura asigna a profesional: mismo circuitoSOL→REQsinCaso.
4. Abrir dos Casos del REQ; comprobar objetivos/asignaciones independientes y una sola responsabilidad principal. Suspender cobertura: no cambia ese principal ni mueve Casos, pero nueva asunción regional utilizable queda cerrada.
5. Sede equivocada: redirigir SOL no asumida, conservando domicilio/historia y sin crear REQ. No confundir con transferencia de principal, que sigue solicitud/aceptación independiente cuando REQ existe.
6. Ante domicilio contradictorio/extranjero/inutilizable, jefatura evalúa condición, fundamento y referencias; conservar región declarada. Sin información suficiente, dejar pendiente. AUSENTE no permite borrar una región ya registrada.
7. Propio ingreso de jefatura: preflight real de cobertura y asunción directa con SOL asumida/REQsinCaso, sin central artificial. Otra área, profesional solo lector, TI y cuenta alias sin facultad no reciben preflight o decisión. Verificar detalles/listados del REQ y ausencia de acceso al Caso por SOLO_DOCUMENTO.
8. Cambiar/finalizar propuesta TI después de firmar: nueva asunción queda cerrada hasta acreditación nueva del contenido. Expirar/retirar certificado también. Nuevo registro1.8 conserva registros/firma1.7 y accesos documentales expresos; ninguna facultad documental se transforma en Caso/territorio.

Las pruebas automatizadas usan H2, MockMvc, DOM y MySQL real aislado; este guion no acredita navegador físico. No Formulario real, proveedores, correo ni bases preservadas. Carga nominal, catálogos oficiales, conciliación histórica explícita y recorridos físicos siguen pendientes técnicos/operacionales separados.
