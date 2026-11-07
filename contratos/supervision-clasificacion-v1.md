# Supervisión de clasificación · 1.0

Rector §§05,07,16,18; ACC-07/08,DIS-03,NOT-01/03/07,AUD-03/04. Validación funcional confirmada06/11/2026. La concesión de supervisión permite consultar metadatos de tomas; no el contenido o gestión de COM,SOL,REQ,CAS,actuaciones o documentos.

## Concesión específica y versiones

RegistroAutoridad **1.9** extiende explícitamente1.8 mediante supervisoresClasificacion. Cada entrada contiene id,actor,persona,bandeja,sede,desde,hasta,referencia,sha256,estado. actor es la cuenta exacta; persona debe coincidir con su vínculo firmado vigente y el período concedido debe caber en ese vínculo/raíz. La credencial actual debe estar acreditada. Compartir persona entre cuentas no transfiere la concesión.

WEB_CENTRAL acredita la procedencia inmutable FORMULARIO_PUBLICO_DDN; no el textoWEB normalizado de la interfaz. No incluye capturas manuales por ser centrales. MANUAL_SEDE requiere una sede concreta de captura acreditada y origen REGISTRO_FUNCIONARIO. No se determina el alcance por unidad,materia,área,domicilio,territorio de hechos,cargoTI o un destino posterior. La sede debe existir y estar vigente para usar su concesión.

Estados ACTIVA,SUSPENDIDA,REVOCADA; fechas y firma se verifican en cada consulta. Retirar la entrada de una raíz posterior también cierra su acceso. No hay permisos por defecto. Datos nominales reales no vienen precargados; los ejemplos y pruebas son sintéticos.

La firma Ed25519 sobre el documento versionado acredita la decisión institucional independiente; ejecutar la configuración técnica no permite a TI fabricarla. Se mantiene el canal privado/configuración de autoridad existente. No se agrega un endpoint para importar raíces arbitrarias,configurar confianza o firmar decisiones. El despliegue/carga nominal y actualización del archivo privado requieren su procedimiento operacional autorizado,fuera de esta campaña. La API consulta el registro efectivamente persistido, no el archivo que alguien declara haber preparado.

Registros1.0–1.8 no contienen esta concesión y se interpretan como antes. No se reescriben firmas,propuestas o respaldos anteriores.1.9 conserva unidad funcional distinta de materia introducida en1.7 y coberturas de1.8. RegistroAutoridad1.9 es una versión de contrato, no una migraciónFlyway.

## API y consulta

GET /nucleo/institucional/clasificacion/supervision?despues=0&limite=20 devuelve tomas activas pendientes dentro de concesiones actuales. Filtro por fuente y autorización se aplica enSQL antes de conteo/paginación. Devuelve folio,cuenta actuante,fecha,versión opaca y sede de captura; no relatos,documentos,estadoREQ/CAS o revisiones de gestión. Sin concesión efectiva403;identificador de detalle fuera de alcance404. Límite técnico1–50 y cursor opaco,no parámetro institucional.

GET .../supervision/{com} conserva los últimos30cambios de clasificación. No devuelve una COM sin toma registrada. revisionCom=0 y todas las facultades de toma/liberación/recuperaciónfalse,incluso si el usuario tiene otra facultad independiente. La vista específica es de consulta; gestionar requiere su circuito y permisos propios. Auditoría de consultas/listados,Cache-Control:no-store y descarte de datos/respuestas tardías tras denegación.

EV-02 usa la intención AVISO_CLASIFICACION1.0 existente: actuante autorizado y supervisores nominales vigentes,una notificación por destinatario/evento y outbox separado. Un supervisor recibe enlace a metadatos; un actuante que perdió su función y conserva solo supervisión recibe también ese enlace. Ninguno obtiene acceso al original por la notificación. Liberar/finalizar/revocar retira detalle,listas,conteos y correo pendiente. Fuentes ya proyectadas no se reenvían retrospectivamente al cargar una concesión; su bandeja sí muestra los pendientes autorizados actuales.

La ausencia del último coordinador no transforma una captura previamente regional en central. Cuando falta catálogo formal, la designación regional histórica conservada solo restringe el encaminamiento; no concede roles ni inventa organización. El catálogo firmado vigente sigue teniendo prioridad. La conciliación de datos nominales reales no se ejecuta por inferencia.

## Interfaz y persistencia

Ruta #supervision-clasificacion y detalle porCOM,tabla paginada,cuenta actuante,origen e historial sin acciones de gestión. La API y el backend mantienen la restricción aunque se invoquen operaciones manualmente. Lectura de metadatos no marca avisos leídos ni acusa asignaciones.

Sin tablas/migraciones nuevas. Se reutilizan raíz firmada,historiales,guardas y outbox. V1–V15.17 intactas,sin outOfOrder/backfill. Originales,hashes,firmas Formulario1.0/1.1 y COMMIT→ACK se preservan.
