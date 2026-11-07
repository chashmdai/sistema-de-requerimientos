# Clasificación con toma individual y relevo · 07/11/2026

**Cierre acreditado:** PR18/d1fea2a2e0047f8530b9589feb1d6e215056dcd2,CI42374808455,ochojobs aprobados,93raíces/677métodos porOS,Windows668aprobados/9omitidos,Ubuntu653/24,cero fallos/errores. Frontend196/196 porOS una vez,lint/TypeScript/Vite y JAR3archivosdist idénticosSHA256. Cobertura completa/disjunta del mismoHEAD/run/OS;actaPR ylogsverificados. Noatribuir alhijo supervisión1.9.

## Capacidad y decisión técnica

Circuito vertical para reservar una COM,liberar con fundamento y recuperar tras revocación/vencimiento/relevo efectivo. Reutiliza los cerrojos de identidad/COM,idempotencia e historiales inmutables. El estado se deriva de eventos exclusivos de este contrato; los registros antiguos no se convierten en tomas por inferencia. No se añaden tablas o migraciones.

Bandeja de origen conservada: web/central solo distribuidor efectivo; manual regional solo coordinación de captura. TI,jefatura,otro ámbito u otra cuenta de la misma persona no adquieren titularidad. Recuperar requiere que el titular haya perdido su habilitación; no por antigüedad de la toma o un permiso técnico. Una toma no crea SOL/REQ/CAS ni acuse. Distribuir crea la SOL después; la jefatura conserva su asunción posterior.

Backend controla distribuciónU2,compatibilidad histórica,vinculación como antecedente y asunción propia para que no pasen sobre otra toma. Encaminamiento finaliza la toma sin borrar historia. Reintentos confirmados conservan sus resultados. Consulta de metadata no expone revisiones de gestión de expedientes ya encaminados; visibilidad documental/área/materia posterior no cambia.

API real en NucleoU2Controller; ClasificacionComunicaciones separa ciclo y autorización; Notificaciones incorpora intención AVISO_CLASIFICACION1.0 por revisión exacta y canal interno/correo pendiente. Listas/conteos/detalles/envío revalidan cuenta y toma. Retomar no resucita aviso anterior. Auditoría de consulta no genera avisos o acuses.

Frontend TomaClasificacion integrado en EncaminarU2: controles explícitos, fundamento,estado,historial,versión real para distribuir,reintento idéntico y descarte de contexto/denegación. Distribución deshabilitada y submit protegido mientras no se acredite toma propia. Vista de aviso admite ausencia legítima de destino: tomar no asigna SOL.

Contrato [toma-clasificacion-com-v1](../contratos/toma-clasificacion-com-v1.md). Reglas avanzadas ACC-08,DIS-02/03,REG-DIS-01,NOT-01/03/04/07,AUD-03/04. EV-02 parcial: actuante implementado; supervisión nominal separada todavía pendiente técnico, sin privilegio implícitoTI/jefatura.

## Validación por impacto

- Primer H2:12métodos,8aprobadas/4errores de fixture por perfil regional unidad/sede incoherente. Corregido creando perfil válido y otorgando alcanceSEDE por circuito real, sin modificar autorización.
- H2 posterior:14métodos,13aprobadas/1error de fixturealias. Preparación inicial intentó habilitación reservada a TI y luego reasignar una identidad ya acreditada; los controles rechazaron ambas. Corregido con cambio personal de contraseña y vínculo compartido desde el PRIMER registro firmado de alias, seguido de solicitud/aprobación independiente/ejecuciónTI reales. Ningún bypass de credenciales,identidad o concesiones.
- HTTPnuevo1/1 aprobado: sesión/CSRF/no-store,toma explícita/reintento,consulta sin agregados.
- Calificación posterior **48/48 H2**,01:56:alias1+NucleoU2Test17+NucleoInstitucionalTest+ContenidoInstitucionalTest. Historia,permisos y recorridos previos conservados. Logs clasificacion-alias-regresion.log; cortes fallidos anteriores preservados.
- **MySQL nativo Windows8.0.46 descartable14/14**,cero fallos/errores/omisiones,selecciónClasificacion. Instancia **58490fc8e40040d19323eaeec42bcad4** cerrada/proceso propio0;31migraciones existentes. Concurrencia,alias,revocación,vigencia,rollback,intenciónatómica y compatibilidad real del motor; sin ejecutar bases preservadas.
- Reintentos confirmados después de FINALIZADA: nuevo focal **1/1 H2 y1/1 MySQL**,instanciaeb3e6a6a9b1549d5bc7e5954e1f38735 cerrada/proceso0; se recuperan los mismos resultados sin reabrir toma o duplicarSOL. Corte separado,no una selección integral15/15 ni repetición de los14 ya acreditados. Fuente de producción sin cambios.
- Frontend focal25/25,después2nuevas seleccionadas/18no seleccionadas explícitas. Integral final **196/196**,lint/TypeScript/Vite correctos. No E2E físico ni aceptación manual ejecutada.
- CI propia Windows/Ubuntu todavía pendiente de publicación, requiere ochojobs completos/disjuntos del HEAD final. No se atribuye CI de correos o workflow al hijo. No cambios de timeouts,exclusiones o aserciones.

Logs privados campaña siger-build-4c652bfd9b7a41d5a8e0d659e180202d/clasificacion-*. AuxiliarMySQL agrega schemaGUID _clasificacion dentro de allowlist loopback existente;sin firewall,servicios o MySQLglobal. Ningún correo/Graph/Formulario real.

## Publicación y límites

Rama codex/siger-toma-clasificacion hija limpia de PR17/bc27f2de825f2ecfe0aa0258459a40aa4d422377,basePR16/efdceed. Todos en borrador,sin merge/despliegue. PR16CI42372030854 aprobada8jobs/661métodos porOS/187frontend;fallidaanterior42356855634 conservada en informeCORREO. PR17CI42372396506 aprobada8jobs,92raíces661métodos/OS,Win652/9omitidos,Ub637/24,frontend187 una vezporOS/lint/build/JAR3archivosidénticos. Equivalencia acreditada del workflow,padres intactos; no atribuirla al hijo actual.

V1–V15.17 y firmas1.0–1.8 preservadas,sin outOfOrder/migración nueva/backfill. Datos reales,QA,U4.4/U4.5,COM-000001,SMR,JAR activos,servicios y proveedores intactos.

Supervisión nominal/recuperación forzada sobre titular habilitado no se habilitan por rol ordinario. Otros pendientes técnicos:invalidaciones/duplicados,documentosREQsinCaso,48h/calendarios,históricos,búsqueda/reportes/exportaciones y catálogos oficiales por cargar. No se declaraU2–U9 ni138 completadas,candidata final o nueva validación institucional pendiente.

## Guion por roles — preparado, no ejecutado

1. En entorno nuevo descartable, registrar entrada web sintética. Distribuidor abre clasificación: consulta no creaSOL/REQ/CAS. Tomar habilita el formulario; aviso individual no marca lectura ni acuse.
2. Dos pestañas intentan tomar simultáneamente: una sola toma/intención. Otra cuenta regional habilitada en la misma sede no puede liberar,recuperar o distribuir mientras titular siga habilitado. Alias de la misma persona tampoco hereda titularidad/aviso.
3. En captura manual regional, distribuidor central y coordinación de otra sede no consultan la toma; coordinación de captura solo distribuye dentro de esa sede.
4. Liberar exige fundamento. Historial conserva toma/liberación y nueva toma. Aviso anterior no vuelve al retomar.
5. Revocar o esperar el vencimiento de la concesión de la cuenta titular en datos sintéticos. Habilitar relevo nominal mediante circuito firmado; recuperar con fundamento conservaCOM/folio/original y deja el mismo pendiente. No recuperar solo por tiempo de inactividad.
6. Distribuir la toma propia: unaSOL,sinREQ/CAS; toma finalizada. La jefatura puede asumirSOL y continuar recorrido institucional previo. Intento de vinculación o asunción propia por otra cuenta mientras está tomada falla en backend.
7. Interrumpir respuesta de red de toma/liberación y reintentar: mismo cuerpo/clave,un efecto. CambiarCOM/revocar mientras consulta está pendiente: sin respuesta tardía o distribución autorizada con datos anteriores.
