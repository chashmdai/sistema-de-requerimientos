# Guion por roles · equipo y agenda

Preparado para aceptación posterior en navegador. No ejecutado como E2E físico. Complementa el guion U2 de continuidad con las mismas identidades sintéticas y concesiones independientes; requiere entorno nuevo descartable y migraciones hasta V15.10. No reutilizar QA, U4/U4.5, SMR, JAR, Formulario Público, credenciales o bases preservadas. Registrar URL privada/HEAD/base/custodia en evidencia privada, no en Git.

## Preparación

Usar J_A/P_A de UPR/DEMO_SEDE_A/AREA_A y P_B inicialmente solo AREA_B. Crear P_C con concesión PROFESIONAL/AREA_A mediante solicitud→aprobación independiente→ejecución TI. Crear COM manual sintética, distribuir a AREA_A, asumir SOL, abrir Caso en el REQ. Guardar los IDs asignados, sin folios ficticios predeterminados.

## Recorrido integrado

1. P_C intenta detalle/lista/documentos del Caso antes de incorporarse: denegado. J_A intenta incorporar P_B sin facultad AREA_A: rechazado. TI tampoco puede leer el Caso.
2. P_A abre **Equipo y agenda**, registra fundamento e incorpora P_C. P_C obtiene ese Caso en su listado y puede registrar una actuación propia. No obtiene otros Casos/COM del REQ, reasignación de principal ni solicitud formal de cierre. Un documento ordinario propio del Caso puede leerse según su política; FIR sigue denegada.
3. P_A crea tarea obligatoria con ejecutor P_C, título y fundamento. Dejar descripción y fecha prevista vacías: se admite. P_A no puede confirmar su cumplimiento por ser creador. P_C registra un intento sin respuesta: tarea sigue pendiente y ninguna actuación aparece automáticamente. Reprogramar conservando historial e intento.
4. P_A solicita término; J_A intenta confirmar con la obligatoria pendiente: rechazado. P_C confirma manualmente el objetivo, con la casilla expresa. Crear además una ordinaria; advertencia de pendiente sin autocumplimiento.
5. Variante de reasignación: antes del cierre, tener tareas PENDIENTE/EN_CURSO de P_A/P_C y una terminal. J_A cambia principal a P_C. Comprobar mismo Caso/REQ, acuse nuevo pendiente, todas las no terminales en P_C, terminal intacta, creador/fechas/recordatorios/obligatoriedad/intentos anteriores conservados. No duplicar tareas ni avisos al destinatario antiguo.
6. J_A incorpora de nuevo P_A expresamente como colaborador si necesita continuidad. Retirar un colaborador con pendientes sin elegir nuevo ejecutor: rechazado. Elegir integrante vigente y fundamento: retiro y traspaso atómicos; el retirado pierde detalle/lista/documentos, conservándose sus autorías.
7. Solicitar espera indicando faltante, a quién y fecha efectiva. El colaborador no puede confirmarla como jefatura. J_A confirma; Caso conserva principal, tareas y gestión. Intentar cerrar: rechazado hasta registrar recepción/evaluación y reanudación expresa. La recepción de una COM por sí sola no cambia espera.
8. Completar/cancelar fundadamente las obligatorias; solicitar y confirmar término por personas habilitadas. La ordinaria queda pendiente y REQ sigue activo hasta cierre explícito de su autoridad. No crear tareas ni intentos después del cierre; regularizar explícitamente una ordinaria existente no reabre el Caso ni crea actuaciones.
9. Revocar el ámbito de un integrante y probar su sesión, búsquedas, conteos, documentos y tareas. Debe perder acceso incluso antes de regularizar la fila de equipo. J_A conserva medios para retirar y resolver tareas. Una cuenta alias no hereda por ser de la misma persona.
10. Dos pestañas: cambiar obligatoriedad frente a confirmación de cierre en la misma revisión. Solo una operación puede prosperar; nunca Caso cerrado con obligatoria pendiente creada antes de confirmar. Repetir Idempotency-Key con el mismo cuerpo no duplica tarea/intento/pertenencia; cambiar el cuerpo produce conflicto.

Registrar por paso esperado/observado, rol, IDs, revisión y evidencia sintética. H2, MySQL nativo aislado, MockMvc y DOM no equivalen a este recorrido físico. No marcar el guion ejecutado a partir de pruebas automatizadas.
