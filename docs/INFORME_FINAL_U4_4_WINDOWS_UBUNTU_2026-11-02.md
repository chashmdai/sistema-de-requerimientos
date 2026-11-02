# U4_4_CERRADO_TOTAL_GO · Windows ↔ DDN-UBUNTU

**Campaña:** `fc1d267d-9cc0-436c-a926-6e5c1cc564ff`. Ensayo E2E sintético completado y ambos ambientes cerrados el **02/11/2026**, America/Santiago. El túnel U4 fue retirado; se conservan todos los datos y evidencias. La QA WSL permanece operativa e independiente. No se habilitó producción ni se inició U4.5.

## Resultado cruzado

| Elemento | Resultado |
| --- | --- |
| UUID público | `68aa6319-43e8-4787-bc75-13f74787249e` |
| COM institucional del ensayo | `a9106fce-5819-437b-8b34-4c6899d3423b` |
| Folio | **COM-000001**, número 1, exclusivamente en la base U4 |
| Contrato / ACK / canal | 1.1 / 1.1 / FORMULARIO_WEB_NNA |
| Recorrido | Un NNA sintético sin RUN, CU ni PISEE; sesión/CSRF y envío HTTP reales ejecutados por el responsable Ubuntu |
| Original | `imagen.png`, image/png, **200 bytes** |
| Adjunto | `5cd42be2-71d3-4975-86f4-e9f5c7750635` |
| Consumidor Windows | **Un solo CICLO**, una recuperada, una confirmada, cero fallidas, cero conciliadas, sin solapamiento ni reintentos |
| Persistencia Windows | Un inbox, un registro COM y un documento, cero COM duplicados y **cero ACK pendientes** |
| Persistencia Ubuntu | **CONFIRMADA**, una Comunicación/recorrido/adjunto, attempts=1, ACK auténtico persistido y cero pendientes/reservas, según el informe final del responsable Ubuntu |
| Firmas | Recibo Formulario Ed25519 `publico-candidato` y ACK SIGER Ed25519 `institucional-nna-lab`, verificados independientemente en ambos lados |

| Integridad | SHA-256 |
| --- | --- |
| Original | `2e39e298cd95f262ea68b437a0da65f34dc9087cabd0d3d4b1be9841e156f76e` |
| Contenido canónico | `6a4ad094da2dc0e31f78cdf518a4905f74787335d2fb4c7edc3b3046028d5a9f` |
| Procedencia | `4dddde4ae1af445fe91660844de140b963ff521bf46336cf6b7f64fb723dc165` |
| JSON ACK durable, coincidente Windows/Ubuntu | `7905429941b5eb672b06672d4719e8cf5dea05e926aa9e45ec353aacb36ddf5c` |

El sobre Ubuntu de 2451 bytes tiene su huella de archivo independiente `2f9fc70a5f99d65f2f41d618067dcd530604d1e83eabc937dfe1cc2a9b17d477`. No se confunde esa huella con la canónica de contenido.

## Ventana, código y transporte

- T0 autorizado: **`2026-11-02T12:52:14.9368060-03:00`**.
- Límite autorizado: **`2026-11-02T13:52:14.9368060-03:00`**, máximo 60 minutos. Los intentos horarios previos quedaron anulados y se conservan como historia.
- SIGER H2 de partida: `35d9e6853d756706f8c34973408ccc60323afc5e`. Preparación U4 en rama `gate/u4-ddn-ubuntu-2026-11-02`; cambios operacionales anteriores identificados en los [informes de preparación](GATE_U4_WINDOWS_UBUNTU_2026-11-02.md).
- JAR U4 ejecutado: **`C:\req\bin\siger-9a9e14f9c664b121.jar`**, SHA-256 **`9a9e14f9c664b1217d325a1228942bc236c594726d884fcad37f4d46b5e1194a`**.
- Sus 242 clases coincidían con las usadas por los verificadores; las 239 clases preexistentes del runtime QA se conservaron idénticas en el candidato. No se sustituyó el JAR activo QA.
- Formulario efectivamente desplegado: **`099c3b16e71d57b335496c3a112daccfa38831db`**; JAR `e7decf6e37ded80dd30e9e60a53cc028994346b82a493aa68b6277c0dd1f427c`. Los commits documentales posteriores no se presentan como otro despliegue.
- Ruta ensayada: Windows `https://formulario.defensorianinez.gob.cl:25443` → SSH privado → Ubuntu `127.0.0.1:24443` → Caddy/Spring. Sin cambios hosts/DNS/firewall, sin Internet y sin publicar MySQL/Spring.
- SONDA independiente previa 401/401/404, TLS1.3/CA/SAN correctos y controles cerrados. Durante la ventana se volvió a comprobar disponibilidad HTTPS; no se omitió validación TLS ni hostname.
- SSH se sostuvo en una consola PowerShell abierta manualmente fuera de Codex, cadena Explorer→PowerShell→SSH, bajo la identidad autorizada. No se instalaron servicios o tareas permanentes.

## COMMIT antes de ACK y conservación

El ejecutor reutilizó el protocolo institucional existente, con cabecera `X-Formulario-Versiones: 1.0,1.1`, lote máximo 1, descarga por reserva, verificación SHA-256/Ed25519, custodia NTFS y exclusión MySQL de base/origen. No selecciona por UUID: antes de invocarlo se comprobó por SELECT en Ubuntu que existía exactamente la entrega comunicada, PENDIENTE, sin intentos/reserva, con ingresos cerrados y PULL habilitado.

`RecuperarFormulario` emite ACK únicamente después de que `guardarCompleta` vuelve de la transacción y recupera el registro conservado verificando otra vez los originales. La transacción usa el COMMIT de Spring JDBC; se comprobó `innodb_flush_log_at_trx_commit=1` en la base real. El artefacto ejecutado mantiene esa cadena; no se dedujo un COMMIT solo de fechas firmadas o de contadores.

Evidencia durable Windows:

- Evento **seq 2**, `COMUNICACION_REGISTRADA`, epoch ms `1793635646372`.
- Evento **seq 3**, `ACK_PUBLICO_CONFIRMADO`, epoch ms `1793635646439`, registrado después del HTTP 204 contractual.
- Registro firmado: `2026-11-02T16:07:26.355Z`.
- ACK pendiente false; comprobante firmado y original nuevamente verificados desde la base/custodia después de terminar el proceso y después de cerrar controles.

Ubuntu verificó por separado firmas, vinculación, recepción del ACK y persistencia tras recrear app. Su primer ACK persistido es epoch ms `1793635647373`. Los relojes de ambos hosts no se usan como prueba independiente de orden entre máquinas. La comprobación de COMMIT→ACK corresponde a la cadena y evidencia Windows anteriores.

## Estado final y cierre autorizado

| Ambiente | Revisión final | aceptarNuevos | recuperar/PULL | Estado |
| --- | ---: | --- | --- | --- |
| **Windows U4 aislado** | **2** | false | false | **SOLO_CONSULTA** |
| **Formulario Ubuntu** | **4** | false | false | **SOLO_CONSULTA** |
| QA WSL preservada | 13 | true | true | NORMAL; automático vigente |

Windows pasó de revisión 0 cerrada a 1 para el ciclo y a 2 para el cierre; no se forzó una revisión anterior por SQL. El único cambio de cierre se realizó con el helper oficial:

```powershell
$ev='C:\req\evidencias\u4-ventana-fc1d267d-9cc0-436c-a926-6e5c1cc564ff'
& .\scripts\Windows-Verificar-MySql-Ubuntu-U4.ps1 -Evidencia "$ev\cierre" -Control CERRAR
```

- Cierre de control Windows acreditado: **`2026-11-02T13:28:30.3105336-03:00`**, dentro de la ventana.
- El responsable Ubuntu acreditó su cierre a **`2026-11-02T13:13:59.972583-03:00`**: flags temporales NNA/aviso restaurados, cinco servicios saludables, misma imagen, integración técnica y confianza U4.2 conservadas, temporizador de expiración desarmado. Esta parte procede de `INFORME_FINAL_UBUNTU_U4_4.md`, recibido del responsable y cotejado con UUID, folio y huellas Windows.
- Después de cerrar U4, se identificó exclusivamente el SSH de la campaña, PID 153164, padre PowerShell 180644 e identidad coincidente. Windows rechazó la terminación desde Codex. El usuario detuvo ese SSH con **Ctrl+C** en su consola independiente; no se terminaron otros SSH ni la consola del usuario.
- Se comprobó la desaparición de **`127.0.0.1:25443`** y del proceso SSH identificado. No quedan ejecutores U4, guardianes o tareas activas de la campaña. La única JVM observada continúa siendo la QA, PID 163036.
- No se lanzó otro CICLO, no se reclamaron nuevas entregas y no se repitió el envío o el ACK. Se conserva la marca atómica de invocación única y el registro de proceso drenado.

La negativa de Windows a terminar el SSH fue una incidencia de cierre resuelta mediante intervención manual confirmada y comprobación posterior. No produjo pérdida de datos, duplicados ni ACK pendientes.

## QA WSL preservada

- SIGER conserva JVM **163036**, launcher padre **188044**, Java25 explícito y listener `127.0.0.1:8081`.
- JAR QA **`C:\req\bin\siger-ae70ab769605c1e9.jar`**, SHA-256 **`ae70ab769605c1e93eab0de052ceee46f724a7b9dfe684a45227717d6a07c2f0`**.
- Base **`siger_institucional_windows_qa`**, mismo perfil WSL y origen `https://formulario.defensorianinez.gob.cl` por 443, `wslrelay.exe` PID **22100**.
- Fotografía final de cierre: **18502 ciclos**, cinco recuperadas/cinco confirmadas, cero fallos, cero ACK pendientes y cero COM duplicados. Los ciclos siguen avanzando; no son una meta de reinicio.
- Los recuentos de las veinte tablas auditadas no cambiaron durante el cierre. Los **33 objetos QA** conservan tamaños, SHA-256 y ACL; el original U4 también. Cero cambios de hashes/ACL en perfiles, DPAPI, trust/certificados, run.json, JAR activo o hosts.
- No hubo pausa, reinicio, reemplazo, fixture, migración, importación o escritura de registros del ensayo en QA/histórico. Graph, CU, PISEE, U5 y futura infraestructura productiva no se modificaron.

## Evidencia conservada y alcance del GO

- Evidencia del ciclo: `C:\req\evidencias\u4-ventana-fc1d267d-9cc0-436c-a926-6e5c1cc564ff\evidencia-ciclo-unico`.
- Cierre oficial: subdirectorio `cierre` y `cierre-resumen.json` en la raíz de campaña.
- Auditoría definitiva: **`C:\req\evidencias\u4-ventana-fc1d267d-9cc0-436c-a926-6e5c1cc564ff\cierre-definitivo-22a36fb56e024d5bbfcc04feea01f5a6`**; puntero `C:\req\scratch\u4-cierre-definitivo.txt`.
- Allí se conservan el informe final Ubuntu recibido, verificación institucional antes/después, controles cerrados, inventarios SHA/ACL, preservación QA y comprobación del retiro del túnel. Los archivos previos del ciclo conservaron íntegros sus hashes; los nuevos controles/auditorías de cierre se guardaron aparte.
- El ACK durable conserva SHA `7905429941b5eb672b06672d4719e8cf5dea05e926aa9e45ec353aacb36ddf5c` antes y después. La comparación de metadatos ignora únicamente el orden de propiedades JSON; valores, eventos, firmas y hashes no cambiaron.
- Permanecen la base **`siger_u4_ddn_ubuntu`**, app/migrador DPAPI, custodia NTFS, certificados/CA/trust, claves y registros/originales sintéticos. **La base U4 ya es evidencia preservada y no es destino descartable de fixtures.**

Este GO acredita el único E2E físico sintético 1.1 acordado y su cierre completo. No se ejecutaron interrupciones controladas adicionales, un segundo ciclo, una campaña de carga o un nuevo E2E 1.0. Las pruebas previas del protocolo y del candidato se mantienen como evidencia separada, sin presentarlas como otra ejecución física. El cierre no habilita ciudadanía ni producción.

## Siguiente etapa: U4.5 pendiente

U4.5 será una demostración manual privada en navegador con **interfaz SIGER aislada sobre la base U4**. Este cierre conserva los datos para ello, pero no inicia interfaces, cuentas nuevas, listeners, servicios, tareas ni recuperación automática. La QA actual seguirá independiente. Cualquier adaptación de arranque/autorización de esa interfaz se hará en su etapa explícita, preservando gates y datos; no se importa este COM a la QA ni se amplían facultades U5 por inferencia.
