# Reversión no destructiva U5

U4.5 publicado permanece en `origin/main` (`b322c9b905b533605fb53c8d706dbc144ed074a3`). U5 se conserva únicamente en su rama local. No hay cutover ni cambio del histórico institucional.

1. Detener el launcher propio de U5 mediante el archivo `stop` de la raíz registrada en `ultimo-runtime-qa.txt`. Comprobar que terminó y retiró los secretos efímeros, sin detener otros servicios.
2. Conservar el esquema QA, V13, los datos, logs, custodias y artefactos por SHA para diagnóstico. No bajar Flyway, quitar tablas, transformar contextos de tarea ni borrar Solicitudes.
3. La copia histórica V12 y sus 24 originales no requieren reversión: no se migran ni se usan en estas pruebas. El entorno previo se mantiene preservado, sin iniciarlo en paralelo ni modificar WSL.
4. No arrancar directamente un frontend U4 sobre datos QA U5: puede interpretar incorrectamente tareas sin Caso. Para una demostración U4, utilizar su entorno previamente preservado de forma aislada y autorizada; no restaurar sus fixtures sobre U5. La recuperación recomendada del QA evolucionado es corrección hacia adelante con el esquema conservado.
5. Para desactivar completamente U5 sin interpretar datos nuevos desde código antiguo, mantener detenido su runtime hasta disponer de la corrección. Esto conserva evidencia y datos; no habilita producción. No es necesario revertir la publicación aceptada de U4.5.

Los contratos públicos, COM, ACK, originales, firma institucional, infraestructura, credenciales y políticas de Windows no se cambian. Graph real y recepción pública permanecen cerrados tanto en arranque como tras reinicio.
