# Primera gestión resuelta · contrato privado1.0

Fuente: rector §§06.4,08.2/08.5,15/17; ING/SOL/BIT/PLA. Requisitos funcionales validados el06/11/2026. Este contrato no es el transporte Formulario1.0/1.1 ni el registro firmado de autoridad.

POST `/nucleo/institucional/primera-atencion/gestion`, sesión individual, CSRF e Idempotency-Key. Cuerpo tipado: `version="1.0"`, `comunicacion` (Manual vigente), `ambito`, `respuesta`, `tipoResultado` y `realizadaEn`. Resultado debe ser ORIENTACION, RESPUESTA_DIRECTA o REMISION_CONCLUIDA sin seguimiento. Un acuse no es un resultado concluido. No se infiere el tipo del texto.

La recepción efectiva proviene de la COM manual inmutable; realización de respuesta es otra fecha explícita; registro técnico lo asigna servidor. UTC y precisión ms coherente con las fuentes existentes. Respuesta no puede preceder a la recepción ni ser futura; ambas fechas deben ser representables. Registrar tarde conserva las fechas reales, sin reemplazarlas para ocultar demora ni impedir documentar una gestión ya realizada. No se calcula un incumplimiento de48h o un plazo de resolución por suposición.

Reutiliza primera atención U2 y sus permisos de ámbito; TI u otra unidad no adquieren facultad por registrar. Una sola transacción crea COM íntegra y REQ finalizado sin Caso, conserva respuesta y añade metadatos de gestión a su misma constancia de historia (V15.16). El tipo se guarda en un snapshot tipado de auditoría. No duplica texto, crea SOL artificial, abre CAS o modifica transporte público. Reintento confirmado conserva COM/folio/REQ/respuesta/fechas; otra atención recibida usa otra identidad de operación.

La sede de captura es la acreditada en la cuenta, incluso desconocida; principal se elige explícitamente dentro del ámbito autorizado. No se deduce captura de principal, domicilio, NNA o hechos. El REQ proyecta `primeraGestion` y su bitácora muestra fecha efectiva/persistida y ejecutor propio. Los anteriores sin fecha siguen desconocidos.

La ruta anterior `/primera-atencion` mantiene únicamente reintentos de comandos confirmados verificando actor/acción/hash; nuevo registro requiere formulario vigente con fecha. No se cambian su DTO/hash/semántica histórica ni se ejecuta una segunda respuesta durante el reintento.

Interfaz real exige ambas fechas, tipo y respuesta efectivamente entregada. No usa el instante de abrir pantalla como recepción/gestión. Un registro que continúa usa ingreso pendiente; no finaliza por recibir o distribuir. No añade aprobación de Caso inexistente ni una autorización nominal ficticia.
