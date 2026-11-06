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

## Territorio: siguiente capacidad, todavía no implementada

Reutilizar org_coverage y OrganizacionInstitucional para propuestas técnicas de cobertura; estas no acreditan competencia por sí mismas. Diseñar acreditación firmada1.8 por ámbito/vigencia/ref/hash y constancia territorial al nacimiento de REQ. Conservar firmas1.0–1.7, historial y principal de REQ previamente asumido. Cobertura por domicilio del requiriente, nunca NNA/hechos/captura; central no se regionaliza por inferencia. Ausencia de domicilio utilizable exige decisión fundada de jefatura, sin reescribir declaración.

Validación institucional confirmada, datos concretos no cargados. La restricción actual de crear solo sedes LAB en organización debe tratarse mediante configuración institucional acreditada en preparación operativa; no inventar IDs o titulares. No se declara esta parte desarrollada ni se crea V15.17 antes de revisar referencias concurrentes.

## Dependencia y bloqueo técnico localizado

Rama codex/siger-ingreso-territorio hija de PR #11/361e323. Fecha de primera gestión lista en corte local; falta consolidación/publicación del bloque. CI del padre42312378567: Windowspart2 falló con7 errores en inicio de fixture H2 después de cerrar pool de migración; no aserciones fallidas. Se conserva log completo privado de1174780bytes, SHA25beec3556986a08c66ee1eee11e34d48054f583461bee103db0f988dc803c10. Hay que corregir el traspaso de conexión del fixture, manteniendo timeouts/contratos y sin reintentos que oculten fallos, antes de acreditar el padre. No es una incidencia de QA o MySQL operativo.

Al completar las138 reglas o identificar sus límites concretos, se ejecutará revisión transversal/consolidación/candidata según instrucción vigente; todavía hay capacidades independientes implementables. Sin merge a main, despliegue o intervención operacional.
