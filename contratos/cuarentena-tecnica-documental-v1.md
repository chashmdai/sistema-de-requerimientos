# Cuarentena técnica documental · 1.0

DOC-07/§15.5: seguridad técnica puede aislar un archivo peligroso sin obtener lectura funcional. Asociación,restricción institucional,cuarentena y purga son operaciones distintas. Este circuito no elimina ni reescribe archivos,firmas,originales o comprobantes.

## Identidad y separación

Cuenta individual TI efectiva,credencial vigente y persona acreditada en registro firmado. Se identifica el original por UUID y SHA-256 exactos,con referencia/fundamento técnicos. No hay búsqueda de nombres,relatos o expedientes para TI. La respuesta contiene solo incidente,identificadores/huella,estado,actores/fechas y notas técnicas.

Liberación por otra persona TI vigente; una cuenta alias de quien aisló no es independiente. El servicio privado verifica integridad,antivirus e inspección de tipo antes de confirmar. Indisponibilidad,rechazo,incompatibilidad,revocación o conflicto mantienen retención. Se conserva el intento iniciado y su resultado. No se requiere que el actor que aisló conserve su cargo para que otra persona autorizada revise el incidente.

## Barrera y permisos

RETENIDO impide contenido,descarga y derivados,también cuando existía autorización institucional o SOLO_DOCUMENTO. Se comprueba al consultar vista/página y antes de cada bloque de64KiB. Se protegen copias con SHA-256 idéntico y se rechazan nuevas cargas del mismo contenido mientras esté retenido. No se expone el origen/expediente de otra copia.

El permiso institucional conserva su propia vigencia; la barrera técnica no lo revoca,reinterpreta o amplía. Los metadatos legítimos permanecen disponibles con bandera cuarentena. Una liberación no concede lecturaTI,no levanta FIR/restricciones ni restaura concesiones vencidas/revocadas. Otra retención del mismo contenido sigue comprobándose.

Reintento de carga ya confirmada puede confirmar metadatos legítimos del mismo original retenido,con verificación privada de integridad y sin transmitir bytes. No crea copia ni omite barrera de descarga. Estado institucional bloqueado o pérdida de ámbito siguen denegando los metadatos según su política propia.

Adaptadores de autorización y caché del visor no sustituyen la comprobación técnica obligatoria. El servicio de verificación puede leer internamente para inspeccionar; no devuelve rutas o bytes al operador. El cierre/transferencia/ACK no liberan ni purgan documentos.

## API y datos

`/nucleo/ti/cuarentena-documental`: lista paginada por estado y detalle opacos;POST crea aislamiento y `/{id}/verificar` solicita verificación independiente. Sesión,CSRF,Idempotency-Key y X-Siger-Actor coincidente con la cuenta actual. Cliente aborta ante cambio de cuenta y descarta respuestas tardías. Una respuesta incierta conserva cuerpo/clave para reintento.

V15.20 aditiva H2/MySQL:document_technical_hold/history/command. Retención activa única por documento y estados consistentes mediante restricciones;identidad/documento bloqueados transaccionalmente. Historia de sucesivos incidentes permanece. V1–V15.19 intactas,sinoutOfOrder o retenciones inferidas de históricos. Esquemas anteriores no tienen incidentes; en esquema20 la ausencia de tabla provoca fallo, nunca apertura de contenido.

No-store,auditoría de consultas,denegaciones,intentos y decisiones. La solicitud de bytes no acredita entrega completa o lectura humana. El perfil técnico VERIFICACION_PRIVADA_1.0 identifica el procedimiento de esta implementación; no constituye certificado de un proveedor o firma institucional.
