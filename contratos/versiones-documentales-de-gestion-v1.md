# Metadatos y versiones de gestión · 1.0

Aplica DOC-01/03/04 y §15.4 del levantamiento validado06/11/2026. No incorpora editor ofimático ni reemplaza firma, visación, despacho o numeración externa de Docflow/Partes.

## Declaración y original

Un documento de gestión de Caso o REQ conserva el nombre,bytes,hash,autor y fecha originales. Los metadatos descriptivos se declaran por separado con título,tipo declarado,fundamento y autor efectivo. Tipo declarado no significa catálogo institucional acreditado. Ninguna carga o migración deduce una fase o un número de versión.

Primera declaración utiliza revisión-1 y crea un BORRADOR documental identificado explícitamente. Se puede declarar FINAL por su responsable/jefatura competente; se conserva fecha del servidor y cuenta actuante. Referencia externa opcional es un dato declarado, no una firma ni confirmación de despacho. FINAL no vuelve a BORRADOR. Una rectificación descriptiva posterior conserva todas las declaraciones anteriores, el estado/autor/fecha de finalización y los bytes. No equivale a una actuación, nueva COM o borrador compartido de COM.

## Sucesión explícita

La nueva versión es otro original previamente conservado por el circuito documental seguro. Ambos documentos requieren autorización efectiva de su propietario en el mismo contexto Caso/REQ y declaración de metadatos. El nuevo debe ser BORRADOR independiente,sin relaciones previas; el anterior debe carecer de sucesora. Ambos tienen revisiones verificadas y protección expresamente equivalente. Una reserva o un permiso no se transfiere por enlazar.

Se conserva una cadena lineal, sin ciclos o ramas. El antecedente final permanece final y consultable bajo su propia autorización. Version_number de la sucesora se deriva únicamente de la relación declarada, no del nombre del archivo. No se sustituye el original anterior ni se fusionan documentos de distintos Casos por compartir REQ/materia.

## Autorización y consulta

Escritura por responsable/jefatura propietaria, no por ser cargador,colaborador,lector de materia,TI o cuenta alias. Documentos históricos sin propiedad/protección acreditada no se regularizan por esta ruta. Reutiliza autorización individual,área,materia,equipo y reserva de los servicios documentales actuales. Nueva finalización/sucesión requiere gestión abierta; corrección descriptiva no crea gestión poscierre. Reintento confirmado no duplica ni reabre.

Un lector legítimo del documento puede consultar su declaración actual. Historia de metadatos se entrega al propietario autorizado; referencias anterior/siguiente se filtran nuevamente por documentos consultables en ese contexto. Una versión FIR oculta no se revela mediante historia,conteos o enlaces del documento ordinario. Revocación vigente se comprueba dentro de la transacción.

## API y persistencia

`/nucleo/{casos|requerimientos}/{recurso}/documentos-gestion/{documento}/versiones`: GET devuelve declaración actual,facultades y referencias autorizadas; POST `/metadatos`, `/finalizar`, `/sucesion` exige sesión,CSRF,Idempotency-Key y revisión. Solicitudes no acreditan conocimiento humano. No-store y auditoría de consulta/denegación; mutaciones con historia transaccional inmutable.

V15.19 aditiva H2/MySQL:metadata,metadata_history,successor,metadata_command. Sin modificación de originales/tablas históricas/V1–V15.18,backfill,outOfOrder o nueva firma. Contratos Caso/REQ anteriores siguen vigentes y los documentos sin declaración se muestran SIN_ACREDITAR.
