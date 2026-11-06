# Guion manual por roles · continuidad institucional U2

Guion preparado para una sesión posterior de navegador. **No se ha ejecutado como E2E físico.** Usar exclusivamente un entorno nuevo y descartable, preparado e identificado para esta aceptación; nunca QA histórica, U4/U4.5, COM-000001, `C:\req\bin` ni Formulario Público. No ejecutar `Nucleo-Local.ps1 demostrar`: ese recorrido histórico no representa este contrato.

## Preparación y datos sintéticos

El operador registra URL privada, HEAD candidato, nombre/UUID de la base descartable, custodia nueva, responsables de preparación y fecha. Verifica migraciones hasta V15.8, proveedores/consumidor reales deshabilitados y ningún proceso/puerto/base compartido. La URL, contraseñas, claves y respaldos privados no se escriben en este guion ni en Git.

Preparar estas identidades **mediante cuentas individuales, cambio de clave temporal y el circuito solicitud → aprobación institucional independiente → ejecución TI**. Los nombres son sintéticos; no representan nombramientos reales. Los catálogos/mandatos de prueba se firman con una clave exclusiva del entorno, con huellas reales de las credenciales de esas cuentas, vigencias que cubran la sesión y las mismas estructuras versionadas de autoridad existentes. No insertar grants por SQL ni usar identidades `demo.*` heredadas como sustituto de aprobaciones. Si falta la acreditación firmada, detener la preparación; no abrir permisos.

| Cuenta sintética propuesta | Ámbito/facultad de prueba |
| --- | --- |
| `acep.ti` | TI institucional; crea cuentas y ejecuta instrucciones, sin lectura universal. |
| `acep.autoridad` | Mandatos firmados de aprobación/revocación para los ámbitos sintéticos; persona distinta de TI y beneficiarios. |
| `acep.distribucion` | Único distribuidor central vigente. |
| `acep.jefe.a` / `acep.prof.a` | COORDINADOR / PROFESIONAL de UPR, DEMO_SEDE_A, AREA_A. |
| `acep.jefe.b` / `acep.prof.b` | COORDINADOR / PROFESIONAL de UPR, DEMO_SEDE_A, AREA_B. |
| `acep.jefe.d` / `acep.prof.d` | COORDINADOR / PROFESIONAL de UPD, DEMO_SEDE_A, sin área formal en ese catálogo sintético. |
| `acep.direccion.materia` | DIRECTOR con consulta MATERIA MAT_UPR, sede regional sintética acreditada; sin gestión AREA_A/AREA_B. |
| `acep.alias.a` | Cuenta distinta de la misma persona acreditada que `acep.jefe.a`, con concesión independiente para jefatura AREA_B. No es una persona revisora independiente. |

Usar catálogos sintéticos de áreas y materias compatibles con el modelo 1.2; no cargar un organigrama real ni deducir cobertura territorial. Si DEMO_SEDE_A se configura regional, registrar ausencia de domicilio utilizable y decisión territorial fundada al asumir. No inventar domicilio para eludir la cobertura pendiente.

Preparar tres ingresos manuales pendientes desde `acep.prof.a`, texto y canal exactos:

| Etiqueta del ensayo | Canal | Texto sintético |
| --- | --- | --- |
| CONT-TRF | TELÉFONO | `CONT-TRF: orientación y dos líneas de trabajo ficticias, sin personas reales.` |
| CONT-DER | PRESENCIAL | `CONT-DER: intervención ficticia cuya continuidad necesita otra área.` |
| CONT-REA | TELÉFONO | `CONT-REA: gestión ficticia con cierre y revisión posterior por error documentado.` |

La aplicación asigna UUID/folios; **no usar valores predeterminados**. Mantener una planilla privada de etiqueta→COM/folio/SOL/REQ/CAS. Distribución clasifica cada ingreso al único destino AREA_A; J_A asume/asigna P_A y abre un Caso con objetivo independiente. Registrar capturas de estados sintéticos, sin credenciales. No crear ni importar un ingreso público en el Formulario real para este ensayo.

## A. Transferir principal sin trasladar gestión

1. En CONT-TRF, J_A abre dos Casos bajo el mismo REQ. P_A registra una actuación antes del acuse y una tarea personal ordinaria en uno de ellos. Guardar ID/folio, responsables y fechas de tarea.
2. Abrir ficha y aviso: el acuse continúa pendiente. P_A selecciona **Acusar mi asignación actual**; aparece identidad/fecha y la gestión anterior sigue conservada.
3. J_A, desde el REQ, solicita transferencia a UPD/DEMO_SEDE_A con fundamento `CONT-TRF: cambio fundado de coordinación` y resumen `CONT-TRF: paquete mínimo de evaluación`.
4. Antes de aceptar, J_D ve solo el paquete en la bandeja institucional. Intentar abrir directamente un Caso AREA_A: acceso denegado. J_A sigue principal y no puede cerrar el REQ mientras hay intervención/decisión pendiente.
5. J_D acepta principal. Verificar mismo REQ, COM y folio, principal UPD, dos Casos aún AREA_A/P_A, misma tarea/ejecutor/fechas, sin TAR-09 ni actuación copiada. J_D ve estados mínimos; abrir esos Casos/documentos sigue denegado. J_A conserva su competencia sobre los Casos y pierde la facultad principal del REQ.
6. Repetir con otro ingreso: rechazar o retirar fundadamente antes de aceptar conserva principal y no crea Casos. Dos pestañas con aceptar/retirar sobre la misma revisión: solo una decisión prospera; la otra informa versión/estado vigente.

## B. Derivación con sucesor y cierre posterior

1. CONT-DER: P_A registra `Actuación privada de origen que no debe viajar en el paquete`. P_A no dispone de facultad para iniciar derivación formal.
2. J_A solicita derivación a AREA_B, motivo `CONT-DER: intervención de destino`, resumen `Solo este resumen está autorizado para evaluación`, objetivo `Objetivo independiente del sucesor`.
3. J_B ve paquete/materia informada y objetivo; no ve la actuación privada, original, FIR ni documentos completos. Una Dirección por materia y TI no acceden a esta decisión por sus roles.
4. P_A puede registrar una nueva actuación mientras se decide. J_B acepta y asigna a P_B. Verificar sucesor nuevo, mismo REQ/folio raíz, vínculo al origen, bitácora propia vacía, principal sin cambios. Origen continúa EN_GESTION con P_A y su equipo/documentos/tareas intactos. No se copian documentos ni sus consentimientos.
5. P_A solicita **cierre explícito por derivación**; J_A revisa y confirma desde el circuito de término. Solo ahora origen CERRADO, modalidad DERIVACION y cierre conservado. Sucesor sigue abierto. La UI no ofrece reapertura ordinaria del origen.
6. Rechazo/retiro sobre otra solicitud pendiente deja origen intacto y ningún sucesor. No se puede retirar retrospectivamente una aceptada.

## C. Reapertura y coordinación de autoridades

1. CONT-REA: P_A solicita término ordinario y J_A confirma. Guardar cierre, revisión y fecha. J_A cierra expresamente el REQ; cerrar el último Caso no lo termina automáticamente.
2. P_A solicita **nuevo período de gestión**, fundamento `Error documentado en el alcance del cierre` y antecedentes `Revisión de antecedentes ya existentes; no se necesita COM nueva`.
3. J_A intenta aprobar sin autorización principal: denegación, Caso y REQ continúan cerrados. Desde el REQ, J_A autoriza reactivación coordinada con fundamento. La autorización por sí sola todavía no abre ninguno.
4. J_A vuelve al Caso y aprueba la solicitud actualizada. Verificar mismo Caso/REQ/folio, REQ ACTIVO, Caso EN_GESTION, nuevo período autorizado, cierre anterior visible e intacto.
5. Repetir rechazo: Caso cerrado, decisión e historia conservadas. Solicitar revisión posterior fundada con los mismos antecedentes es admisible; no se exige inventar una COM nueva.
6. Variante con principal transferido a J_D: J_D autoriza reactivación del REQ; J_A confirma reapertura del Caso. J_A no autoriza como principal y J_D no reabre un Caso de AREA_A por ser principal. No hay apertura parcial del Caso bajo REQ terminado.

## D. Negativas, revocación y límites

1. AREA_A y AREA_B: repetir detalle, búsquedas, bandejas/conteos, documentos, antecedentes y avisos desde P_B/J_B sin concesión al origen. Conocer UUID, compartir REQ o leer MAT_UPR no amplía permiso.
2. J_A solicita transferencia a AREA_B; `acep.alias.a` intenta aceptar. Debe rechazarse por misma persona, aunque sean cuentas y roles distintos. J_B independiente sí puede decidir si sigue vigente.
3. Autoridad revoca la concesión del solicitante/destino o deja expirar su vigencia. La sesión obsoleta pierde facultades; una aceptación no puede aprovechar la solicitud antigua para eludir revocación. TI no puede reponer el permiso sin aprobación independiente.
4. Cambiar área/materia/responsable durante una derivación vuelve obsoleta su aceptación. Retirar/rechazar y formular una nueva decisión, sin borrar la anterior. Una actuación ordinaria del origen no es por sí sola un cambio de responsabilidad.
5. Reasignar un Caso U2 con tarea no terminal: operación bloqueada por evolución TAR-09 pendiente. No modificar creador para forzarla ni marcar tarea cumplida ficticiamente. Sin tareas no terminales, reasignar crea una asignación y acuse nuevos; el anterior sigue visible.
6. Recursos históricos sin principal/área acreditados: conservar evidencia y bloqueo de conciliación. No se usa primer Caso como principal, cobertura técnica como competencia territorial ni roles ordinarios como FIR/extraordinaria.

## Registro del resultado

Por recorrido: HEAD, rol/cuenta sintética, fecha, IDs/folios reales del ensayo, pasos ejecutados, esperado/observado, evidencia de autorización negativa y resultado APROBADO/FALLIDO/NO EJECUTADO. Distinguir rechazo esperado de fallo. Guardar evidencias fuera de Git y sin datos reales. No marcar este guion aprobado por el solo resultado H2, MySQL, MockMvc o DOM. El cierre del entorno descartable corresponde al operador que lo preparó; no detener servicios ajenos.
