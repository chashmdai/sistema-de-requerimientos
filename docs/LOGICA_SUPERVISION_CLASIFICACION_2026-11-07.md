# Supervisión nominal sin acceso a expedientes · 07/11/2026

## Corte publicado acreditado

PR #19, HEAD263729b267ecd2948de4befc1f2ba6628a87a876. CI42378024688 cerrada con ocho jobs aprobados. Inventario completo y disjunto:95 clases/691 métodos por OS;Windows682 aprobados/9 omitidos,Ubuntu667/24,cero fallos/errores. Frontend202/202 una vez por OS,lint/TypeScript/Vite; JAR final con tres archivos idénticos porSHA256. Logs de agregación128425774622/128425774488 verificados para ese HEAD/run. No acredita el hijo documentalREQ, ejecución manual, proveedores o despliegue. PR permanece borrador, sin merge.

## Implementación

RegistroAutoridad1.9 incorpora concesiones por cuenta/persona,fuente de captura,vigencia y respaldo verificable bajo la confianza privada independiente existente. WEB_CENTRAL y MANUAL_SEDE son ámbitos explícitos; no herencia por TI,jefatura,unidad,área,materia o cuenta alias. Estados activos,suspendidos,revocados,retirada y expiración aplicados desde la raíz persistida y credencial actual. Se verifica una única raíz consistente por consulta.

SupervisionClasificacion filtra enSQL antes de conteos y paginación. Solo tomas activas pendientes con referencia,cuenta,fecha y captura. Detalle histórico de clasificación sin relatos,documentos,estado de expedientes o revisiones de gestión; no acciones de toma,liberación o recuperación. API/UI reales con sesión,no-store,auditoría y limpieza al denegar/cambiar contexto. El menú no otorga permisos: sin concesión efectiva devuelve denegación.

EV-02 incorpora supervisores nominales al aviso/outbox y revalida en detalle,listas,conteos y transporte. Cuenta alias no hereda aviso. Titular que perdió distribución pero conserva supervisión obtiene ruta de metadatos; el original sigue denegado. No genera acuses,gestiones,REQ,SOL oCAS por leer.

Se corrige además el cambio implícito a bandeja central al vencer la última coordinación: un indicio regional histórico solo conserva la restricción mientras se configura catálogo formal; no atribuye una función actual. Los datos y destinos anteriores no se alteran.

Contrato [supervision-clasificacion-v1](../contratos/supervision-clasificacion-v1.md). Avance ACC-07/08,DIS-03,NOT-01/03/07,AUD-03/04. EV-02 desarrollado para actuante y supervisión explícita; el catálogo global de avisos y auditoría transversal sigue parcial.

## Arquitectura y compatibilidad

Sin nuevo esquema: V1–V15.17 intactas,31migraciones. RegistroAutoridad1.9 añade una lista explícita y lector estricto; no reinterpreta bytes firmados1.0–1.8. Contextos de cierre,reserva,documento aislado,funciones,principalREQ y territorio conservan campos y semántica. La prueba1.7→1.9 conserva concesión documental y respaldo originales,sin concederCaso a TI y manteniendo materiaUPD/unidadfuncionalUPR separadas.

La concesión se provisiona por el canal privado firmado ya existente; no se construye un endpoint que acepte confianza o raíces arbitrarias desde la interfaz. No se cargaron titulares,cuentas o permisos nominales reales. Los controles de suspensión/revocación se probaron contra el registro efectivo en entornos descartables; no se actualizó una configuración operacional.

ClasificacionComunicaciones comparte construcción de metadatos sin permitir gestión desde supervisión. Se reutilizan intención/outbox,idempotencia,guarda de identidad y fuenteCOM conservada. No colas adicionales,backfill,despliegues o cambios del Formulario. La nueva sede de supervisor se valida como catálogo existente/vigente, sin inventar cobertura territorial.

## Pruebas y cortes

- Compilación de producción9s correcta. Primer testcompile rechazó import ambiguoVinculo;corregido calificando tipo,sin tests ejecutados en ese intento.
- H2 inicial23:17aprobadas/6errores por códigos de auditoría de33/36caracteres en columna32 y capturaB hecha por perfilA. Se usan códigos nuevos acotados sin alterar tabla; fixtureB con cuenta/concesión reales.
- Focal posterior10:5aprobadas/4fallos+1error por consultar canalWEB normalizado en lugar de procedencia persistida. Corregido usando FORMULARIO_PUBLICO_DDN,contrato real de recepción; se conservaron las aserciones de visibilidad/conteos.
- Focal9/9H2 tras corrección. Calificación posterior **13/13 H2/HTTP**,01:32:Supervision10,HTTP1,documento1.9compatible1,últimacoordinación1. Incluye lista/paginación/UUID ajeno,alias,credencial cambiada,expiración,suspensión,revocación,raíz anterior conservada,firmas alteradas rechazadas y consulta concurrente que espera revocación atómica. Sesión HTTP vigente pierde alcance sin adquirir gestión o contenido.
- **MySQL nativo Windows8.0.46 descartable12/12**,Supervision10+documento1.9+regional,instituciab5437a655640404abcee56c2bfd52d9a cerrada/proceso propio0;31migraciones. No bases QA/históricas tocadas.
- Corrección posterior de enlace del actuante sin rol: **2/2 H2 y2/2 MySQL**,positivo previo+regresión nueva,en otra instanciaef6cdb6131464fb1b8bde5476b038b4f cerrada/proceso0. No se presenta como una única selección13/13 MySQL ni se repite el corte12sin causa. La API original ya denegaba, y ahora la ruta informada conserva también la distinción.
- DOMfocal21/21. Frontend integral202/202,lint/TypeScript/Vite; después se muestra cuenta actuante sin cambiar contratoAPI: focal6/6+lint/TypeScript/Vite finales. No se repite toda la batería por esa presentación. La CI propia debe ejecutar202 sobre el HEAD publicado.
- CI propia Windows/Ubuntu todavía pendiente de publicación,ocho jobs/cobertura completa/disjunta. No se usa éxito del padre para el hijo.

Evidencia privada: campaña siger-build-4c652bfd9b7a41d5a8e0d659e180202d,logs supervision-*. Cortes fallidos conservados. Sin relajar aserciones,permisos,timeouts funcionales,transacciones o exclusiones. H2/MockMvc/DOM/transportes doblados y motorMySQL nativo no acreditan navegadorE2E,proveedores,correo real o despliegue.

## Cadena y pendientes reales

Rama codex/siger-supervision-clasificacion hija de PR18/d1fea2a2e0047f8530b9589feb1d6e215056dcd2. PadreCI42374808455 aprobada8jobs/93raíces677métodos porOS,Windows668aprobados/9omitidos,Ubuntu653/24,frontend196/lint/build/JAR3archivosidénticos. Acta e informe verificados,PR en borrador. No modificar padres/main.

Sigue pendiente programar documentos/antecedentesREQsinCaso,invalidación/duplicados e ingreso flexible,48h/calendarios,búsqueda/reportes/exportación,conciliación histórica y eventos restantes. Auditoría global de todas las denegaciones/exportaciones todavía parcial. Carga nominal y actualización operacional del registro privado separadas de implementación; validación funcional del levantamiento confirmada06/11,no una nueva pregunta.

No138completas ni candidata final. Al completar lo programable o identificar limitación concreta por regla, realizar auditoría transversal de clases/dependencias heredadas,retirada de rutas provisionales incompatibles y candidata integrada compilable con informe138 y recorridos manuales.

## Aceptación manual por roles — preparada, no ejecutada

1. Entorno nuevo descartable,raíz1.9 con supervisor sintéticoWEB_CENTRAL y otroMANUAL_SEDE A. Generar tomas sintéticas desde distribuidor/coordinación. Cada supervisor ve solo sus fuentes y conteos autorizados.
2. Abrir aviso de toma: enlace a supervisión,datos mínimos,sin COM/documentos/expediente. Abrir la URL original o POST de gestión con esa cuenta debe fallar, aunque se manipulen controles del navegador.
3. Cuenta alias de la misma persona sin concesión nominativa no hereda la vista o aviso. Una concesión de solo lectura no habilita liberar o recuperar.
4. Revocar/suspender/expirar la concesión en configuración sintética efectivamente incorporada: misma sesión pierde lista,detalle y copia de aviso. Cambiar credencial requiere re-acreditarla; no basta conservar el rol.
5. Ejecutar consulta simultánea con retirada bajo guarda: después del COMMIT no retorna datos anteriores. Liberar la toma por su actuante retira pendiente y correo; la historia permanece.
6. Quitar distribución a su actuante conservando supervisión: el aviso sigue solo en la ruta de metadatos. Vencer la última coordinación no habilita al central a distribuir esa captura regional.
7. Migrar únicamente una base descartable con respaldo sintético: firma/documento1.7 sigue válido en raíz1.9 mientras el mandato permanece idéntico; cambio de materia exige nueva aprobación y revoca acceso previo,sin dar Caso a TI.
