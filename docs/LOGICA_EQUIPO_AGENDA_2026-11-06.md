# Bloque A · equipo operativo y agenda del Caso

## Estado del hito

Desarrollo vertical sobre PR #5, rama `codex/siger-equipos-agenda`, base `ef4d378a4c5afb9e23d56b0d4fa93a1b7c2aade5`. La validación funcional del 06/11/2026 está registrada en [la decisión vigente](DECISION_VALIDACION_FUNCIONAL_2026-11-06.md) y en las 138 filas de la matriz. No equivale a cargar nombramientos reales ni a desplegar.

Código/API/persistencia/React confirmados en 06cc2ec. Las pruebas focalizadas están acreditadas y la calificación integral definitiva se registra sobre el HEAD publicado en el acta del PR dependiente de PR #5. Este documento distingue los intentos intermedios de esa evidencia final; no se atribuye aprobación a una ejecución fallida.

## Capacidades

Equipo por Caso con principal y colaboradores explícitos, incorporación local por principal/jefatura, facultad vigente por unidad/sede/área, retiro con reasignación de pendientes e historial. Listas y conteos usan la misma pertenencia efectiva que los detalles. No se obtiene COM raíz, otros Casos, gestión de la principal del REQ, FIR o facultad de cerrar/reasignar por participar. Documentos históricos sin política y documentos ajenos sin clasificar no se abren por la nueva pertenencia. El autor puede consultar su aporte nuevo; compartir contenido ordinario requiere la política correspondiente.

Agenda V12/V13 evolucionada: tareas del equipo, creador/asignador/ejecutor único, descripción y fechas opcionales, obligatoriedad con historial, intentos sin cumplimiento ficticio, reprogramación, cancelación fundada y confirmación manual del ejecutor. Tarea y actuación siguen independientes. Obligatoria no terminal bloquea cierre; ordinaria advierte. Pos cierre no admite alta ni nuevos intentos, conservando regularización administrativa de ordinarias anteriores.

Historia visible de retiro y agenda: la interfaz muestra motivo de retiro, autor, ejecutor/fechas/obligatoriedad anteriores e intentos, consumiendo los registros reales del backend.

TAR-09 atómico junto con la nueva asignación profesional: todas las no terminales, incluidas las de colaboradores y EN_CURSO, cambian al nuevo principal. Se conservan autor, identificadores, fechas, ventanas, recordatorios, intentos, obligatoriedad y estado; terminales intactas. No se dispara por transferencia del REQ, área o derivación. La generación de destinatario elimina entregabilidad de recordatorios antiguos sin duplicar tareas. Acuse sigue siendo manual y propio de cada asignación.

Espera explícita de antecedentes, con indicador en ficha/listados y filtro EN_ESPERA autorizado: solicitud con faltante/destinatario/fecha efectiva, confirmación/rechazo de jefatura, reanudación expresa. Conserva gestión abierta, responsable y relojes. Espera pendiente o confirmada impide cerrar mediante atajo. Avisos internos de pertenencia y espera reutilizan proyección durable, con permiso por recurso y sin habilitar correos reales.

## Arquitectura y persistencia

`EquipoCaso` concentra pertenencia y espera; `TareasPersonales` conserva su almacenamiento y contextos anteriores, ampliando solamente tareas CASO. `NucleoInstitucional` reutiliza autorización efectiva, `identity_guard` y cerrojo COM para asignación, agenda y cierre. Historial de flujo/core y de tareas conserva cada cambio; la promoción de colaborador a principal concluye su pertenencia anterior sin borrar autorías.

V15.10 Java H2/MySQL con DDL común, checksum explícito y orden posterior a V15.9. Sustituye exclusivamente el CHECK personal de tareas identificado por su definición (nombre generado por motor); conserva el límite creador=ejecutor fuera de CASO, claves foráneas y datos históricos. Añade pertenencias actuales/históricas, intentos, espera, asignador, obligatoriedad, fundamento y generación de recordatorio. Sin backfill de equipos, inferencia de obligatoriedad, edición V1–V15.9 ni outOfOrder. Durante una revisión de cierre, los cambios de agenda conservan mediante una nueva referencia la modalidad DERIVACION de esa solicitud, sin reescribir la anterior ni permitir reapertura ordinaria por perder la modalidad.

No se observaron versiones posteriores publicadas en main ni en referencias conocidas de U4.5 al elegir V15.10. No se inspeccionó/modificó su directorio. Se requiere verificar cambios no publicados al integrar; binarios anteriores no deben ejecutarse contra este contrato de autorización ampliado.

## Verificación y evidencia

- Selecciones iniciales H2 de agenda/migración aprobadas. Una selección intermedia de 23 tuvo dos fallos y dos errores por expectativas personales y revisiones antiguas; evidencia preservada. Se adaptaron a la semántica de equipo sin retirar negativas. Selección posterior 43/43.
- Regresión integral local del primer corte: **474 backend, 463 aprobadas, dos fallos y nueve omitidas**, cero errores, Maven 16:34. Ambos fallos eran expectativas HTTP que negaban lectura/asignación de agenda a la jefatura competente. No se presenta este intento como aprobado.
- Selección final ampliada: **63 métodos, 62 aprobados y un fallo**, sin errores/omisiones. Agenda 16, documentos 2, API equipo 2, migración equipo 2, continuidad 17, tareas 14 y áreas 2 aprobaron; la negativa HTTP del nuevo profesional sin pertenencia obtuvo CAMBIO_CLAVE_REQUERIDO antes de llegar al control de recurso. Se completó su cambio real de clave temporal en el fixture, conservando la expectativa 404. **NotificacionesHttpTest 8/8** aprobó después.
- Documentos finales **3/3 H2**: contenido ordinario explícito, FIR segregada, históricos y documentos ajenos sin clasificar, retiro, área y cuenta alias sin pertenencia. Durante la extracción de un auxiliar del fixture quedó un nombre de variable anterior: falló compilación antes de tests, se corrigió y se conserva la evidencia. No fue una prueba MySQL ejecutada.
- **Frontend final 119/119 DOM**, lint correcto y build TypeScript/Vite final correcto. Una inferencia opcional del DTO de prueba impidió un build intermedio; se hizo explícito el valor nullable y se repitió la compilación. No se excluyeron pruebas.
- **MySQL nativo Windows 8.0.46**: primer corte 9/9 y selección completa 69/69. Son cortes anteriores a las últimas pruebas de idempotencia, opcionales, espera/listados y modalidad DERIVACION. **Selección completa final 73/73**, cero fallos/errores/omisiones, sobre el código 06cc2ec. Incluye las últimas pruebas y actualizaciones V14/V15.1…V15.9→V15.10. Instancia propia cerrada y ausencia verificada.
- El intento MySQL que encontró el error de compilación del auxiliar documental no ejecutó pruebas; instancia propia cerrada. La compilación H2 posterior y los tres métodos documentales aprobaron. No se oculta ese intento ni se lo cuenta como éxito de motor.
- La CI de Windows/Ubuntu del HEAD publicado constituye la regresión integral definitiva, con lint, frontend, H2 y empaquetado. Los enlaces, SHA y totales de ambos jobs se registran en el acta del PR. No se reutiliza el éxito de PR #5 ni el de un SHA anterior.

No se suman selecciones solapadas como pruebas únicas. H2, MockMvc y DOM no son E2E físico. Documentos usan dobles AV/renderer declarados; workers reales/proveedores no se ejecutaron. Se conservan stress y omisiones heredadas, los timeouts funcionales y el presupuesto CI de 30 minutos. npm informó nueve vulnerabilidades moderadas heredadas; package-lock y dependencias se conservaron sin actualizaciones masivas.

Logs conservados en carpeta privada de campaña bajo LOCALAPPDATA: integral-equipo.log, focal-agenda-final.log, http-agenda-corregido.log, documentos-equipo-corregido.log, frontend-consolidado.log y frontend-build-final.log, frontend-historia.log y frontend-historia-build.log, además de cortes anteriores. Cada MySQL usa instancia/datadir/esquemas/credenciales/puerto nuevos, --no-defaults y comprobación de propiedad; no se reutilizan bases operacionales.
## Trazabilidad y demostración

EQU-01/02/04/05/06/07, TAR-01..09 y TAR-06B; actualización relacionada CAS-04, CAS-06, CAS-CIERRE y BIT-03. EQU-03 conserva cobertura parcial de colaboración interunidad: decisión de terminar una intervención ya aceptada pendiente técnica. Se actualizan 20 filas funcionales y cinco aclaraciones de estado técnico, manteniendo 138 IDs y orden. Las demás reglas no se declaran terminadas por este bloque.

[Contrato](../contratos/equipo-agenda-institucional-v1.md) y [guion de aceptación por roles](GUION_ACEPTACION_EQUIPO_AGENDA_2026-11-06.md). Guion preparado, no ejecutado en navegador físico. Backend y UI conectados sin depender del flag de notificaciones.

## Continuidad

Siguiente: publicar el hito con CI definitiva del HEAD y continuar con término de colaboración aceptada, cierres, subrogancias y continuidad excepcional mediante designaciones parametrizadas y aprobaciones efectivas. Pendientes técnicos B/C/D/E no se atribuyen a falta de validación institucional. No se cargaron datos nominales reales.

QA, U4.4/U4.5, SMR, COM-000001, bases históricas, runtimes/JAR, Formulario, proveedores, originales, contratos y COMMIT→ACK intactos. Sin merge, despliegue, migración operacional, correo real ni E2E físico.
