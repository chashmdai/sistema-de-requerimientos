# Datos, identidad y operación funcionaria

## Persistencia y migraciones

MySQL es la persistencia operativa; H2 es un doble de base para pruebas aisladas. Los scripts están en `backend/src/main/resources/db/institucional/{mysql,h2}`. Son dos implementaciones por versión, no dos bases simultáneas de negocio.

| Flyway | Capacidad |
| --- | --- |
| V1–V4 | Inbox, controles/eventos técnicos, COM/comprobante y conciliación |
| V5 | Comunicaciones y núcleo: destinos, Requerimientos, Casos y actores |
| V6–V8 | Complejidad, derivación aceptada, antecedentes y reaperturas |
| V9 | Documento ciudadano completo, MEDIUMTEXT, originales y relaciones documentales |
| V10 | Documentos de gestión, representaciones/páginas y eventos documentales |
| V11 | Cuentas individuales, roles/alcances/vigencias, territorio e historial |
| V12 | Tareas personales, notificaciones, outbox, campaña e intentos de correo |
| V13 | Preparaciones de Solicitud U5 y tareas independientes/de Solicitud |

No modificar checksums o SQL histórico. Migraciones futuras se agregan con versión nueva y pruebas de compatibilidad. Migrador y aplicación son identidades distintas. El runtime no ejecuta Flyway ni necesita DDL.

`institutional_inbox` conserva documento íntegro y recibo firmado; `communication_registry` impone unicidad por origen/UUID público y folio. `core_*` agrega gestión sin reescribir ese original. `institution_document` conserva descriptores y referencias de los originales físicos. Las relaciones con Casos/Requerimientos no copian archivos. Las migraciones no convierten una recepción en Caso.

Un RUN declarado o dato SRCeI no se convierte en autenticación. `ADULTO_CU`, `ADULTO_DECLARADO` y `NNA` conservan distinciones y procedencia por atributo; extranjeros sin RUN y grupos de NNA se conservan como tales. La vista humana no cambia el JSON firmado.

## Ámbitos y facultades confirmadas

Identidad estable del funcionario, cargo, roles y alcances son conceptos separados. `staff_account` y `staff_assignment` mantienen estado, vigencias, autor y revocación; `security_epoch` invalida sesiones después de cambios. Los permisos se resuelven en cada solicitud y por recurso, no solo al mostrar el menú.

| Función | Facultad dentro del alcance vigente |
| --- | --- |
| TI | Cuentas, perfiles, roles y organización; sin acceso operativo automático a contenido |
| Funcionario activo | Registro manual de Comunicación |
| Administrativo | Registro manual; no distribución por ese rol |
| Distribuidor principal | Distribución institucional |
| Coordinador regional | Jefatura en su ámbito y distribución de ingresos originados en su propia sede |
| Director/Coordinador | Facultades funcionales de jefatura en su ámbito; no distribución por cargo genérico |
| Profesional | Gestión de Casos autorizados y documentos bajo su contexto |

Una cuenta puede acumular roles, pero debe satisfacer el permiso y alcance de la operación concreta. TI no es superusuario. `nexbyte` es una cuenta multirrol sintética de QA; no se copia como matriz productiva ni justifica accesos cruzados. La propiedad resumida `rol` puede mostrar JEFATURA aunque existan otros accesos: consultar asignaciones efectivas, no inferir facultades desde esa etiqueta.

Región, Sede, Unidad y coberturas/vigencias son persistentes y distintas. Una sede puede cubrir varias regiones; cambiar cobertura no modifica el origen histórico. Un ingreso web sin sede no adquiere una coordinación territorial por defecto. Origen web, canal declarado, sede real, región del asunto y destino distribuido no se confunden.

Autorización documental se reutiliza por recurso: misma unidad/macrozona o conocer un UUID no concede acceso. La cuenta desactivada o con permiso revocado pierde acceso con su sesión anterior. Se mantienen controles negativos de TI sin función operativa y entre unidades.

## Buzón Comunicaciones y recorrido

En QA: `https://localhost:8081/` → login individual → **Comunicaciones**. No existe un segundo menú obligatorio llamado Clasificación y Distribución en esta versión.

1. Buscar por folio o consultar la bandeja autorizada.
2. Abrir la Comunicación: folio/estado, origen/canal/sede, identidad/procedencia y formulario completo.
3. **Original recibido → Documentos del ingreso**: descriptores, integridad, previsualización segura y descarga autorizada. Los originales no son editables.
4. **Destinos y Casos** e **Historial y vínculos** presentan gestión existente según permiso. Distribuir genera un destino/evento, no abre Caso o Requerimiento automáticamente; la evaluación de jefatura se conserva.
5. Las actuaciones, antecedentes, derivación con aprobación de destino, cierre profesional de baja complejidad y reaperturas mantienen sus reglas previas.

Las respuestas directas históricas de Comunicación/destino siguen sus reglas. No son la operación nueva `responder Solicitud` que está bloqueada por U5.

## Tareas y notificaciones

Las tareas son personales: creador y responsable coinciden; no las impone otra jefatura. En Casos nacen del Caso abierto y mantienen su vínculo. V13 admite además tareas independientes y de una preparación autorizada de Solicitud. Recordatorio, ventana válida y fecha límite son distintos. Completar requiere confirmación manual y no cierra Caso ni crea actuación.

Notificaciones son persistentes por funcionario, deduplicadas y resueltas desde facultades/responsabilidades vigentes. Leer una notificación no aprueba una gestión ni concede acceso al recurso enlazado. Inactividad/revocación vuelve a filtrar la consulta. Los envíos externos pasan por outbox transaccional; el correo no incluye contenido ciudadano reservado.

Graph está **SIMULADO** en la QA actual. Los 4 envíos históricos fueron aceptados y confirmados por el humano en su campaña; no se repiten ni se infiere entrega de un 202. No activar nuevas campañas por ejecutar un build, importar datos o restaurar un backup.

## Datos de desarrollo protegidos

`siger_institucional_windows_lab`: 504 filas históricas/59 tablas y 24 objetos protegidos. `siger_institucional_windows_qa`: QA integrada con cuentas, gestión y Comunicaciones E2E conservadas. El nombre QA no autoriza limpieza. No ejecutar fixtures contra ninguno; los tests H2 usan directorios privados nuevos.

La batería MySQL anterior fue ejecutada cuando el destino era descartable; sus resultados no autorizan borrar el estado actual. Para otra campaña destructiva, TI debe provisionar un esquema/identidades de prueba distintos y su allowlist debe revisarse antes de habilitarla. Véanse [gates](GATES_DESPLIEGUE_SIGER.md) y [estado H2](ESTADO_GATE_H2_SIGER_2026-11-01.md).
