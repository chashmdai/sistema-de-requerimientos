# E2E interactivo Formulario WSL → SIGER Windows

## Recepción institucional comprobada

El humano presentó personalmente la Comunicación `d4b72201-26fe-4334-aabb-a910ec076fc3` desde React y autorizó por separado **una única recuperación HTTPS** en la QA aislada. Los agentes no reenviaron ni generaron el ingreso ciudadano.

- Resultado de la única invocación: **1 recuperada, 1 confirmada, 0 fallidas, sin solapamiento ni repetición**.
- Nuevo folio **COM-002498**; UUID institucional **c717e0d1-7bf0-4de8-a654-17431de01361**.
- Contrato negociado `X-Formulario-Versiones: 1.0,1.1`; documento recibido **1.0**, tipo `ADULTO_DECLARADO`. ACK **1.0**, canal `FORMULARIO_WEB_PUBLICO`. Se conservaron contenido y procedencia; no se elevó la declaración a autenticación.
- Canonicalización, huellas y firma Ed25519 de la custodia pública verificadas sobre el documento conservado; firma institucional y correspondencia del ACK verificadas con claves públicas confiables.
- Exactamente un registro COM y dos documentos `INGRESO_CIUDADANO` para la operación. Sin Requerimientos ni Casos automáticos. SQL QA: incremento de1 registro/2documentos, cero ACK pendientes y cero folios duplicados.
- Una segunda extracción no se ejecutó para probar idempotencia: se comprobó unicidad persistente y se conservaron las restricciones existentes sin consumir una autorización adicional.

## Originales

| Formato | Bytes | SHA-256 |
| --- | ---: | --- |
| PNG | 622233 | `f4c3c066ba39205d38bb3fc28a7b8923137c744abdf5187a3cacad13e400da4e` |
| PDF | 457214 | `3b486e5a266297da2ca1cfa892911ca354f960ddd33f85633f98edaf5bb18ab9` |

Coinciden los descriptores firmados públicos, los metadatos MySQL institucionales, los bytes de custodia NTFS y las descargas autenticadas. Se volvieron a calcular hashes después de previsualizar; ambos originales siguen intactos.

Se verificaron26 controles en Edge/HTTPS con `nexbyte`: login individual, acceso institucional de Distribución, búsqueda del nuevo folio, detalle correcto, formulario completo, dos originales, descarga autorizada y previsualización real de página1 para PNG y PDF. El acceso sin sesión fue denegado para ambos originales. No se pulsaron acciones de distribución ni se utilizó la Comunicación anterior.

## COMMIT y ACK

El artefacto realmente ejecutado es `siger-9897abd7cd0bd57d.jar`, SHA-256 `9897abd7cd0bd57df6f0199e28904517c10d650bd7dd2795e34b2aa7b85084bf`.

Se inspeccionó su bytecode, sin recompilarlo: `guardarCompleta` utiliza `BaseJdbc.transaction`/`TransactionTemplate.execute`; solo después de retornar se recupera el registro persistido y se vuelven a comprobar los originales en `registroCompletoConservado`; después se invoca el ACK HTTPS. La transacción confirmada y los originales conservados preceden al ACK. La evidencia incluye la secuencia del ejecutable y los eventos persistidos **4043 COMUNICACION_REGISTRADA → 4044 ACK_PUBLICO_CONFIRMADO**. No se atribuye esta comprobación a una captura de paquetes de COMMIT.

- ACK payload SHA-256: `f7f030f14d4ebd191d2af50d45192402b126f8edda15ffba7baa219da6968c8f`.
- Bytes de firma ACK SHA-256: `3caccfb3acff0e40a5dfb9044db5ef7f720f5238c78af043c7dc81ee378660d4`.
- `ack_pending=false` en SIGER. El agente público confirmó **CONFIRMADA**, exactamente **un intento**, el mismo folio/UUID y firmas/huellas ACK coincidentes. Cola pública:1total/1confirmada/0pendientes/0reclamables. Sus eventos son5RESERVADA →6REGISTRO_SIGER_CONFIRMADO. Cerró `recuperar=false` por API auditada, revisión4, manteniendo `aceptarNuevos=false`.
- Evidencia pública final conservada y SHA-256 verificado: `8f60c63977c9769d83a8a0abe815f82f3281fc438321c58426e86db36a7c0e73`, archivo local `formulario-phase-d-final-sanitized.json`. No se confunde la confirmación criptográfica/BD con una inspección de la sesión ciudadana humana, que sigue pendiente.

## Revisión humana

En Edge/Chrome de DDN-TEST-SIGER:

1. Abrir `https://localhost:8081/` e iniciar sesión como **nexbyte** con su contraseña privada existente; no se cambió ni restableció.
2. Menú **Comunicaciones** → buscar **COM-002498** → **Abrir COM-002498** → **Original recibido** → **Documentos del ingreso**.
3. Enlace directo después de iniciar sesión: `https://localhost:8081/#comunicacion/c717e0d1-7bf0-4de8-a654-17431de01361`.
4. En la pestaña original de Formulario, `https://formulario.defensorianinez.gob.cl/?vista=formulario-adulto`, sección **Recibimos tu comunicación**, pulsar **Actualizar estado**. El flujo previsto presenta **Comunicación registrada**, folio y **Descargar comprobante institucional**. Esa consulta requiere la sesión propietaria; no se atribuye a la sesión humana una comprobación realizada con otra sesión. La JVM pública había sido reanudada antes de FaseD; si la sesión antigua dejó de estar vigente, no reenviar ni alterar `owner_session` para aparentar recuperación. Documentar esa limitación con el responsable del Formulario.

El usuario puede consultar la contraseña QA en un diálogo local privado, sin consola ni restablecimiento:

```powershell
& "C:\Program Files\PowerShell\7\pwsh.exe" -NoProfile -STA -File C:\req\repo\scripts\Windows-Ver-Acceso-QA.ps1
```

## Disponibilidad y límites

Por orden humana, **las dos aplicaciones deben permanecer encendidas** al concluir. Se reanudó SIGER antes de la extracción con el mismo perfil, artefacto, esquema y custodia; el opt-in `-MantenerDisponible` del launcher QA elimina solo el vencimiento temporal de esta revisión. No instala servicio ni tarea, ni activa recuperación automática. El modo temporal anterior sigue siendo el predeterminado.

- SIGER: `C:\req\testdata\runtime-qa-73993af4cf8f4991b5fe6e5d0412c732`, JavaPID246096, launcherPID258800; HTTPS8081 únicamente127.0.0.1. Perfil privado correspondiente exclusivamente a la raíz pública20d434… y claves de esa QA.
- Custodia QA y trust combinado anteriores conservados; no se sustituyeron datos por fixtures. MySQL QA con aplicación DML, TLS1.3/VERIFY_IDENTITY; sin Flyway en runtime.
- Control SIGER cerrado inmediatamente después de la única invocación: `recuperar=false`, revisión4. Scheduler de recuperación y Graph real falsos. Ningún proveedor gubernamental ni correo real utilizado.
- Guarda4GiB respetada antes del arranque, extracción y vistas. Histórico protegido verificado mediante lectura: **504filas/59tablas idénticas y24objetos SHA-256/ACL intactos**, 16869263bytes.

No detener procesos al terminar este informe. Cuando el humano autorice cerrar el laboratorio, cerrar primero ambos controles y luego detener solo este run mediante su archivo `stop`, dejando bases, volúmenes, custodia y evidencias. No borrar el nuevo COM ni originales; cualquier reversión funcional debe ser trazable y no destructiva.

## Evidencia

Campaña privada `C:\req\evidencias\e2e-interactivo-fase-d-2026-11-01-7b6cfd75abbc44ca8e089d441267f43b`, puntero `C:\req\scratch\ultimo-e2e-fase-d.txt`. El run vigente está en `C:\req\scratch\runtime-e2e-interactivo.txt`.

Incluye autorización, marcador exclusivo previo a invocar, resultado único, controles, lecturas SQL antes/después, firmas, integridad de originales, bytecode transaccional,26 comprobaciones Edge y preservación histórica. Logs de launcher/runtime siguen abiertos: no detener para calcular sus hashes. No se almacenaron secretos, cookies, contenido ciudadano, capturas ni trazas de autorización en Git.

No se ejecutó la campaña completa de regresiones U5: no se cambió lógica de negocio ni el JAR. Los nuevos verificadores se compilaron/ejecutaron y los scripts se validaron sintácticamente. No push, migraciones, nuevas facultades ni producción.
