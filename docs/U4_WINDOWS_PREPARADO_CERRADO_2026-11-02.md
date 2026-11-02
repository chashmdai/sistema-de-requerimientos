# WINDOWS_PREPARADO_CERRADO · campaña U4.4

**Estado confirmado el 02/11/2026 a las 11:36, America/Santiago.** Campaña `fc1d267d-9cc0-436c-a926-6e5c1cc564ff`. Windows está preparado para participar en la ventana coordinada de 15 minutos. No hay ventana iniciada, consumidor U4 permanente, túnel U4 activo ni Comunicación nueva. No se ejecutaron ABRIR, CICLO ni cambios de control.

## Verificaciones actuales

| Condición | Evidencia |
| --- | --- |
| Versiones 1.0/1.1 | El bytecode de `FuenteFormulario.post` contiene la cabecera `X-Formulario-Versiones: 1.0,1.1`. Se verificó correspondencia exacta de las **242 clases** compiladas con el JAR inmutable, sin diferencias |
| Artefacto | `C:\req\bin\siger-9a9e14f9c664b121.jar`, SHA-256 `9a9e14f9c664b1217d325a1228942bc236c594726d884fcad37f4d46b5e1194a`, sin cambios |
| Perfil/origen | `C:\req\config\formulario-ddn-ubuntu-u4\perfil.json`, alcance `ENSAYO_SINTETICO_U4_DDN_UBUNTU`, origen exclusivo `https://formulario.defensorianinez.gob.cl:25443`, base `siger_u4_ddn_ubuntu` |
| Custodia | `C:\req\testdata\u4-ddn-ubuntu-5fac1631ea014fad992551964db9c91f\archivo`; custodia NTFS privada preparada y libre de otro escritor |
| Identidades | PSCredential DPAPI app/migrador existentes, identidades exactas, sesiones MySQL TLS1.3; app solo DML y migrador DDL limitado a U4. Ambas cuentas tienen denegado acceso a QA/histórico |
| Exclusión | Locks U4 de esquema y origen adquiridos/verificados/liberados; segunda instancia rechazada y readquisición posterior correcta. No se reclamaron entregas |
| Vínculo durable | DDN-UBUNTU/CA/trust/firmante/custodia coinciden con `consumer_origin_binding`. No se adoptaron datos ni se cambió el vínculo |
| Controles/datos U4 | Revisión **0**, aceptar=false, recuperar=false, `SOLO_CONSULTA`, **inbox=0, ACK pendientes=0** |
| Confianza | CA/trust recibos/trust ACK coinciden con las huellas acreditadas. Key-id ACK `institucional-nna-lab`. ACL, ausencia de reparse points y DPAPI comprobados sin mostrar valores |
| QA independiente | PID Java **163036**, padre **188044**, mismo JAR `siger-ae70ab769605c1e9.jar`, base `siger_institucional_windows_qa`, origen WSL por 443 y `wslrelay.exe` PID **22100**. Control revisión13 NORMAL, automático activo, 17095 ciclos en la fotografía, cinco recuperadas/cinco confirmadas, cero fallos/ACK pendientes/COM duplicados |
| SONDA previa | Se acepta el GO Windows de **11:04:07–11:04:08**, 401/401/404 con TLS1.3/hostname/CA correctos. Perfiles, archivos DPAPI, trust, JAR y hosts conservan hash/ACL: **no se repitió SONDA**, ni se abrió SSH |

Se leyeron los dos adjuntos. Conservan instrucciones de la fase U4.2, incluida una mención a repetir cola vacía. Se aplica la instrucción vigente del usuario: Ubuntu `LISTO_CERRADO`, SONDA ya aceptada y **un solo CICLO**. La ausencia de duplicados se comprobará por evidencia durable y SQL; no se añade un segundo CICLO. El estado Ubuntu `LISTO_CERRADO` se recibe del coordinador y se reconfirmará al inicio, sin una nueva intervención remota ahora.

La acción de verificación local fue `Windows-Verificar-MySql-Ubuntu-U4.ps1 -Control ESTADO`; no cambia controles. La SONDA HEAD interna que el propio ejecutor realiza al entrar en CICLO sigue siendo una barrera del JAR existente; no se elimina ni se ejecutó ahora.

Evidencia privada de preparación: `C:\req\evidencias\u4-windows-preparado-2026-11-02-e26c6f5e5116482196b5d211051724af`, puntero `C:\req\scratch\u4-windows-preparado.txt`. Incluye bytecode/hash de artefacto, estados y permisos MySQL, exclusión/custodia, perfil/DPAPI/confianza, continuidad con SONDA previa, QA y copias de los adjuntos.

## Condiciones para iniciar después

1. Recibir autorización explícita del usuario para esta campaña; ambos responsables confirman presencia y acuerdan **T0 y fin T0+15 minutos**. Hasta entonces ambos lados continúan cerrados.
2. Recorrido único NNA sintético sin RUN/CU/PISEE/adultos, un PNG sintético acordado y flujo HTTP real con aviso/sesión/CSRF. No se crea ni prepara una Comunicación desde Windows.
3. Ubuntu abre recepción sintética con PULL cerrado, realiza un envío, cierra nuevos ingresos y abre temporalmente PULL por CAS. Debe devolver `UBUNTU_ENTREGA_LISTA`, campaña, **UUID público**, revisión de controles y hashes, confirmando que esa es la **única entrega recuperable**, sin entregas ajenas.
4. El ejecutor solicita `limite=1` y recibe el siguiente elemento de la cola; **no tiene un selector CLI por UUID**. Por ello, el cierre de nuevos ingresos y la confirmación de cola única con ese UUID son condiciones previas a ABRIR/CICLO. Ante discrepancia, no se ejecuta.
5. Revalidar localmente U4 sin pendientes, QA saludable, mismos hashes/identidad y exclusividad. Si cambió una condición relevante, detenerse y verificarla antes de continuar. No iniciar un ciclo sin margen suficiente para terminar y cerrar dentro de la ventana.

## Comandos previstos, no ejecutados

Desde `C:\req\repo`, bajo la identidad DPAPI existente. Se preparó el directorio privado de campaña y subdirectorios `apertura`/`cierre`, sin controles abiertos. Los resultados de CERRAR no sobrescribirán la evidencia de ABRIR.

En una consola SSH separada, únicamente durante la ventana autorizada:

```powershell
ssh -N -L 127.0.0.1:25443:127.0.0.1:24443 -o ExitOnForwardFailure=yes -o StrictHostKeyChecking=yes -o ConnectTimeout=5 admin-ti@192.168.100.14
```

Credencial por entrada privada; no contraseñas en argumentos. No hosts/firewall ni servicios persistentes.

Solo tras autorización y `UBUNTU_ENTREGA_LISTA` con UUID/cola única:

```powershell
$ErrorActionPreference='Stop'
$jar='C:\req\bin\siger-9a9e14f9c664b121.jar'
$ev='C:\req\evidencias\u4-ventana-fc1d267d-9cc0-436c-a926-6e5c1cc564ff'
if(Test-Path -LiteralPath "$ev\ciclo-invocado.txt"){throw 'La campaña ya invocó su ciclo; no repetir'}

& .\scripts\Windows-Verificar-MySql-Ubuntu-U4.ps1 -Evidencia "$ev\apertura" -Control ABRIR -EnsayoSinteticoCoordinado

# Marca local de una sola invocación. Conservarla incluso ante error o respuesta ambigua.
$null=New-Item -ItemType File -Path "$ev\ciclo-invocado.txt" -ErrorAction Stop
& .\scripts\Windows-Ensayo-Formulario-Ubuntu.ps1 -Jar $jar -Accion CICLO -EnsayoSinteticoCoordinado |
    Tee-Object -FilePath "$ev\ciclo-unico.json"
```

No envolver el CICLO en bucles/reintentos ni ejecutar otro por cola vacía. Verificar salida `recuperadas=1`, `confirmadas=1`, `fallidas=0`, `solapada=false`, `conciliadas=0`, `ackPending=0`. Además, comprobar bajo app U4 el UUID esperado, COM/folio único, persistencia anterior al ACK, hashes/firmas/documento y original, y cruzar con Ubuntu la confirmación durable `CONFIRMADA`. La salida de un contador por sí sola no cierra el gate. No importar U4 a la QA ni imprimir contenido NNA.

Después del ciclo drenado y la comprobación cruzada, cerrar Windows:

```powershell
& .\scripts\Windows-Verificar-MySql-Ubuntu-U4.ps1 -Evidencia "$ev\cierre" -Control CERRAR
```

Ubuntu cierra recepción/PULL por CAS y restaura únicamente gates temporales NNA/aviso, conservando confianza e integración técnica U4.2. Cerrar normalmente la única consola SSH. Comprobar ambos lados cerrados, cero ACK pendientes y QA preservada. Las revisiones CAS avanzan; no se fuerzan a 0 por SQL.

## Condiciones de parada y reversión

- Sin autorización, UUID público, cola única, exclusividad, confianza o tiempo suficiente: permanecer cerrado, no invocar el ciclo.
- Fallo de TLS/firma/hash, entrega inesperada, pérdida de lock o fallo técnico: detener la campaña, cerrar nuevas admisiones, conservar evidencia y no repetir automáticamente el CICLO ni el envío ciudadano.
- Antes de COMMIT: cerrar U4 tras drenar el proceso y coordinar la reserva con Ubuntu; no fabricar ACK ni purgar leases.
- Después de COMMIT o con ACK ambiguo/pendiente: conservar exactamente el mismo COM/folio/recibo, base, originales, origen y confianza. Comunicar el estado y acordar conciliación explícita; no borrar ni regenerar datos y no efectuar un segundo CICLO bajo esta autorización de ciclo único. No retirar integración/confianza antes de resolverlo.
- Al vencer la ventana: no iniciar nuevos trabajos; drenar el proceso en curso y comunicar cualquier cierre incompleto. No matar la QA o servicios ajenos para forzar un resultado.
- El cierre conserva todo el material sintético y lleva los controles a cerrado por CAS, restaura solo configuración temporal Ubuntu y cierra solo el túnel U4. No revierte U4.2, no toca QA/WSL, MySQL global, Graph, U5, CU, PISEE o la futura VM.

**Mensaje para coordinación:** `WINDOWS_PREPARADO_CERRADO fc1d267d-9cc0-436c-a926-6e5c1cc564ff`. Esperando autorización explícita de inicio, T0/fin y devolución Ubuntu con UUID público único. La preparación y los comandos anteriores no constituyen apertura ni inicio de la ventana.
