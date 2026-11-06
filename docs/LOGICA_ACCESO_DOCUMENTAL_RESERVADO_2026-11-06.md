# Acceso documental FIR con doble respaldo · 06/11/2026

## Resultado y alcance acreditado

La jefatura del Caso propietario propone una cuenta/documento/Caso/período; Gestión Institucional acreditada de otra persona decide. La aprobación permite exactamente metadatos, consulta y/o descarga, según lo concedido. No agrega roles, gestión, reclasificación ni acceso a otros documentos/Casos. Retirada, rechazo, revocación, expiración y suspensión/cambio de respaldo se aplican en backend.

Registro firmado explícito1.6, con facultad de Gestión distinta de cierre1.5; identidad/credencial/persona canónica, ámbito exacto y vigencia. Alias no heredan la concesión nominal. Una persona con ambas facultades no puede darse ni aprobarse el acceso. Se impiden ciclos de aprobación con las instrucciones U1.5 y las concesiones documentales vigentes que verifica el circuito. El respaldo de jefatura y el de Gestión quedan conservados con hash, raíz firmada e historial.

`ContenidoInstitucional.permisos` conecta el permiso con listados/visores/originales/Range/historial permitido y reevaluación en streaming existentes. No se abre FIR por compartir REQ, Caso, materia o enlace. Nueva protección/área/materia invalida la decisión por revisión/secuencias, incluso al volver al valor previo. Se oculta por igual un documento inexistente y uno fuera del Caso autorizado.

El acceso previo al Caso es precondición de este corte. **No implementa todavía la consulta de un documento aislado por TI sin acceso al expediente**, ni una ficha sectorial completa, nuevas clases de reserva, retención/purga o exportación irrestricta. Esa dependencia es técnica, no falta de validación funcional. Sí permite completar SUB-CONT-01 con antecedentes FIR cuando el revisor tiene consulta legítima del Caso y recibe este respaldo documental independiente.

## Componentes y persistencia

- `AccesoReservado/Controller`, ámbito de confianza1.6 en `AutoridadesInstitucionales`, integración en `NucleoInstitucional` y `ContenidoInstitucional`.
- React `AccesosReservados`, propuesta en protección de un documento FIR, bandeja real por facultades del backend, permisos de contenido/descarga separados y vigencias explícitas.
- Avisos mínimos por propuesta/decisión/expiración, outbox de correo durable, deduplicación y reautorización de listas/enlaces. Cero envíos reales; no se confunden consulta, correo o actuación.
- **V15.14**, H2/MySQL idénticas, nuevas y aditivas. FK, CHECK de separación/fechas/estados/descarga y unicidad de concesión no terminal. Sin modificar V1–V15.13, outOfOrder, originales, firmas, transporte, COMMIT→ACK o datos preservados. No hay backfill.

Las decisiones se serializan con identidad→COM; autorización, persistencia, auditoría y evento son atómicos. Propuesta sellada, revisión obsoleta rechazada, comando idempotente sin resucitar una concesión revocada. No se inventan autoridades o identidades nominales. Contrato: [acceso-documental-reservado-v1](../contratos/acceso-documental-reservado-v1.md).

## Pruebas y cortes

- Primera selección H2/MockMvc55:54 aprobados y un fallo HTTP del fixture, que intentaba autenticar por UUID en lugar de nombre de usuario. Dominio13/13, migración18/18 (14 rutas V14/V15.1…V15.13→15.14 y cuatro checksums), Contenido16/16 y RevisiónExcepcional7/7 aprobaron. Se conserva el intento; no se presenta como verde.
- Corregido el login de la prueba, sin alterar autenticación/CSRF. Focal posterior **2/2**: recorrido HTTP y nuevo control contra oráculo documental entre Casos. No hubo bypass ni nueva exclusión.
- **MySQL nativo Windows8.0.46 descartable:32/32**, cero fallos/errores/omisiones: dominio final14 + migraciones18. Incluye FIR→SUB-CONT→cierre EXCEPCIONAL del mismo Caso/REQ, aliases/multirrol, reciprocidad, revocación, suspensión/expiración, retirada/rechazo, manipulación de instrucción, concurrencia/idempotencia, metadatos/contenido/descarga, retiro del equipo y avisos dobles sin envío real. Proceso propio cerrado y ausencia verificada.
- Frontend **137/137**, lint, TypeScript/Vite aprobados. Focal14/14 antes de la regresión completa. DOM con dobles no acredita navegador físico ni backend real.
- Regresión integral del nuevo HEAD en CI Windows/Ubuntu pendiente de publicación/comprobación. No se reutilizan518 del padre como si calificaran este código. Se exige inventario JUnit completo y disjunto por OS. Presupuesto, aserciones, transacciones, timeouts y exclusiones históricas intactos.

Evidencia privada: campaña `siger-build-4c652bfd9b7a41d5a8e0d659e180202d`, logs `reservas-*`; MySQL `siger-autoridad-mysql-5662a875d9f5491ba6fc0b44eadefca3`/tests.log, Maven03:42. H2/MockMvc, MySQL, DOM y guion manual se distinguen; no se ejecutaron workers/E2E físicos ni envíos reales. No se considera la recepción técnica como revisión humana.

## Padres cerrados y cadena

PR #8 HEADfeb9f01fdf46d5002cae8abb74b36d5c1ee7a06e, [CI42267102474](https://github.com/chashmdai/sistema-de-requerimientos/actions/runs/42267102474): seis jobs aprobados,71 raíces, Windows506/497/9,Ubuntu506/482/24; frontend129/lint/build/package. Intento cancelado42233659071 conservado: Windows446/cero fallos/9omitidos, faltaron60; Ubuntu sí aprobó. No se reetiquetó ese intento como verde.

PR #9 HEAD5e674363e63a091af7407847dec36501b037bc63, [CI42267451118](https://github.com/chashmdai/sistema-de-requerimientos/actions/runs/42267451118): seis jobs aprobados,73 raíces, Windows518/509/9,Ubuntu518/494/24; frontend133/lint/build/package. Particiones243+275, MavenWindows15:19/17:31,Ubuntu13:59/14:32. Integral local del mismo corte518/509/9,frontend133,Maven22:20; temporales474683b20d424d7380ce312a29f11bfb. Clases/tests/estáticos compilados antes de iniciar C: esa evidencia no se atribuye al nuevo código. Actas PR #8/#9 actualizadas, ambos borradores.

Rama C `codex/siger-reservas-documentales`, dependiente de #9; matriz138 mantiene IDs/orden y validación funcional06/11/2026. Avanza ACC-01/05/09/10, DOC-01/02/04, SUB-CONT-01/CAS-AUT-02, NOT-01/05/07, AUD-01/03 con límites por regla. No se declaran138 reglas o U1–U9 completas. Main/padres/U4.4/U4.5/SMR/QA/históricos/JAR/Formulario/proveedores intactos; sin merge ni despliegue.

## Guion por roles preparado, no ejecutado

1. En instalación nueva aislada, configurar cuentas sintéticas y registro1.6 firmado con jefatura, Gestión y revisor como personas distintas. Conceder perfiles por U1.5. Crear dos Casos del mismo REQ y dos documentos FIR mediante sus controles legítimos.
2. Dar al revisor consulta explícita del primer Caso. Comprobar que no ve sus FIR ni el Caso hermano. TI y una cuenta alias tampoco reciben esos documentos.
3. Jefatura abre Protección del documento y propone solo metadatos, período finito y fundamento. Antes de aprobación, el revisor sigue sin ver el documento. Gestión consulta solamente la instrucción mínima; no obtiene contenido por ser aprobadora.
4. Gestión aprueba con fundamento. Revisor consulta metadatos; no visor, original, Range o historial de contenido. Revocar y comprobar la denegación con la misma sesión.
5. Nueva propuesta de contenido sin descarga; otra con descarga expresa después de revocar la anterior. Comprobar controles reales de páginas/medios/original según representación. No interpretar una apertura como lectura completada.
6. Repetir con alias, autoridad que acumula función de jefatura, reciprocidad, período vencido/suspendido y revisión obsoleta. Cambiar protección y comprobar que la decisión anterior no abre una nueva FIR.
7. En Caso propio de subrogante pendiente de término, conceder FIR al revisor; solicitar/aprobar SUB-CONT por otra persona acreditada, revisar antecedentes y confirmar manualmente. Se cierra solo ese Caso, conserva REQ/folio/historia y no se resuelve ninguna obligatoria por inferencia.
8. Retirar al revisor del equipo; comprobar documentos, lista y avisos con sesión vigente. Conservar historia/originales y verificar aviso interno/outbox sin ejecutar transporte real.

## Siguiente capacidad

Consulta documental aislada con doble respaldo, sin conceder expediente a TI u otra cuenta: requiere ámbito firmado explícito nuevo y ruta documental dedicada con los mismos controles de bytes/Range/revocación. Después, bitácora común con fecha efectiva/registro y rectificaciones inmutables para Caso y REQ sin Caso, territorio/conciliación y plazos/reportes. Son tareas de implementación y parámetros, no nuevas preguntas institucionales.
