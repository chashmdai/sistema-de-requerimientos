# U4.2 · Sonda cruzada Windows GO y propuesta U4.4

**Resultado independiente: GO desde Windows. U4.4 no iniciado.** El 02/11/2026, entre **11:04:07 y 11:04:08, America/Santiago (UTC−03:00)**, se ejecutó únicamente SONDA. No se ejecutó ABRIR, CICLO, migración o aprovisionamiento. El túnel se cerró normalmente a las **11:07:38** y se comprobó que desapareció su listener.

## Resultado de SONDA

Desde `C:\req\repo`, bajo la identidad DPAPI existente:

```powershell
& .\scripts\Windows-Ensayo-Formulario-Ubuntu.ps1 -Jar C:\req\bin\siger-9a9e14f9c664b121.jar -Accion SONDA
```

```json
{
  "connection": {
    "sinCredencial": 401,
    "credencialInvalida": 401,
    "autenticada": 404
  },
  "technicalAuthenticationReady": true,
  "institutionalSigningTrustedOffline": true,
  "claims": 0,
  "acksSent": 0
}
```

Se utilizó HEAD sobre operación/adjunto/reserva UUID nulos inexistentes, conforme al contrato. El 404 acredita autenticación técnica y acceso al recurso inexistente; la aceptación de un ACK real queda pendiente de U4.4. La firma SIGER comprobada fue una prueba en memoria, sin COM ni envío al Formulario.

- JAR Windows SHA-256: `9a9e14f9c664b1217d325a1228942bc236c594726d884fcad37f4d46b5e1194a`.
- Origen Windows: `https://formulario.defensorianinez.gob.cl:25443`.
- Trayecto: SSH `admin-ti@192.168.100.14:22`, reenvío `127.0.0.1:25443` → Ubuntu `127.0.0.1:24443`.
- `StrictHostKeyChecking=yes`; clave SSH previamente acreditada, sin reemplazos.
- HTTPS `/api/session`: HTTP 200, **TLSv1.3 / TLS_AES_128_GCM_SHA256**, SAN `formulario.defensorianinez.gob.cl`.
- Certificado servidor SHA-256 DER: `f4c0e5f6f1e4167f658b9a5e0eb08ec0a4ca4b2420bf8f579f2722313e7917e8`.
- CA auténtica SHA-256 de archivo: `9ce8ccfc0045660c4f46b88ccee91deca5e88890af7f6dec26c2ef7b7d31557e`.
- Sin CA confiada: TLS rechazado. Con hostname `127.0.0.1` y CA correcta: TLS rechazado. No hubo excepciones de cadena, SNI o hostname.
- Mapa ACK activo Ubuntu: `7888a45c690750b07be974394c327849749d68a98ecbb8e46e12cf43ab836767`, key-id `institucional-nna-lab`; mapa recibos Ubuntu: `5620f27dafcbbc5fc0473d4a158f1823733820381b1322f7061060e5b88c0f2f`, key-id `publico-candidato`.

## Preservación y cierre

El perfil U4 y los archivos privados existentes conservaron bytes y ACL. Se verificaron presencia, ausencia de enlaces, ACL y descifrado DPAPI sin mostrar valores. No se regeneraron credenciales ni privadas; tampoco se editó hosts, DNS, confianza global, firewall, GPO o servicios.

La comprobación SQL U4 utilizó exclusivamente la cuenta app U4 y SELECT/SHOW; no se invocó el helper que vincula/prepara custodia. Resultado: `siger_u4_ddn_ubuntu`, TLS1.3, cero inbox/COM/documentos/ACK pendientes, controles **revisión 0, false/false, SOLO_CONSULTA**.

Ubuntu se comprobó por GET operacional autenticado y SELECT: integración técnica habilitada; controles **revisión 0, false/false, SOLO_CONSULTA**; `citizen_submission=0`, `public_delivery=0`, `control_audit=0`. No se llamó a reclamar, confirmación o fallo.

SIGER QA sigue exclusivamente con el perfil WSL original por 443. El listener `127.0.0.1:443` pertenece al mismo `wslrelay.exe` PID **22100**, y SIGER al mismo Java PID **163036**, padre **188044**, listener `127.0.0.1:8081`. Su JAR sigue siendo `siger-ae70ab769605c1e9.jar`, SHA `ae70ab769605c1e93eab0de052ceee46f724a7b9dfe684a45227717d6a07c2f0`; base `siger_institucional_windows_qa`.

Fotografías antes/después de SONDA: ciclos **16710 → 16740**, cinco recuperadas/cinco confirmadas, cero conciliadas y cero fallos. Control revisión **13**, aceptar/recuperar activos, NORMAL, sin cambios. Los recuentos de las veinte tablas auditadas no cambiaron; cero ACK pendientes y cero COM duplicados. Hash/ACL de perfiles, DPAPI, run.json, JAR y hosts sin diferencias. No se pausó ni reinició la QA.

Evidencia privada de este turno: `C:\req\evidencias\u4-sonda-cruzada-2026-11-02-46a13171c3df49e38ce772f4d2b3f031`, puntero `C:\req\scratch\u4-sonda-cruzada.txt`. Incluye sonda/contexto horario, pruebas TLS, SQL SELECT, controles y comprobación remota, inventario/comparación de archivos, procesos y cierre del túnel. Los dos informes recibidos de Ubuntu se conservaron allí. No contienen una ejecución E2E nueva.

Las anotaciones antiguas `technicalAuthenticationReady=false` / `remoteAckTrustInstalled=false` del descriptor privado se conservaron sin cambios: reflejan la preparación anterior, no el estado operacional actual. La evidencia fresca anterior acredita U4.2; las barreras operacionales autoritativas de ambos lados continúan cerradas.

## Propuesta operativa U4.4 — todavía no autorizada para ejecución

**Campaña:** `fc1d267d-9cc0-436c-a926-6e5c1cc564ff`. Responsables: Codex Windows y Codex Ubuntu, con el usuario como coordinador entre sesiones. Ventana propuesta: máximo **15 minutos desde T0**, que solo se fija tras la confirmación explícita de inicio; no hay hora reservada ni activación automática.

**Recorrido único:** NNA sintético sin RUN, sin CU, sin PISEE. Asunto identificable `PRUEBA SINTETICA U4.4 fc1d267d-9cc0-436c-a926-6e5c1cc564ff`, sin datos personales reales. Un único envío por el flujo HTTP real, aviso vigente y sesión/CSRF reales; adultos deshabilitados. No insertar ni firmar entregas mediante SQL.

**Adjunto propuesto:** fixture PNG sintético ya utilizado en la calificación Ubuntu, `/home/admin-ti/operacion-formulario/2026-11-02/sinteticos/imagen.png`, **200 bytes**, SHA-256 **`2e39e298cd95f262ea68b437a0da65f34dc9087cabd0d3d4b1be9841e156f76e`**. Tamaño/hash se verificaron por lectura en este turno. El responsable Ubuntu debe confirmar su admisibilidad por el recorrido NNA vigente; si lo rechaza, detenerse y acordar el adjunto, sin debilitar validaciones.

**Windows:** mismo JAR U4 por SHA, perfil privado existente, base `siger_u4_ddn_ubuntu`, app `siger_u4_ubuntu_app`, custodia independiente `C:\req\testdata\u4-ddn-ubuntu-5fac1631ea014fad992551964db9c91f\archivo`. Sin scheduler permanente, sin consumidores adicionales y sin modificar la QA WSL.

### Secuencia de coordinación

1. **Ahora:** Ubuntu devuelve `LISTO_CERRADO fc1d267d-9cc0-436c-a926-6e5c1cc564ff`, confirmando recursos, fixture y plan, sin abrir recepción/PULL ni crear el envío. Windows permanece cerrado y con túnel apagado.
2. **Inicio futuro:** ambos responsables confirman presencia/disponibilidad y el usuario indica explícitamente `INICIAR_U4.4 fc1d267d-9cc0-436c-a926-6e5c1cc564ff`. Registrar T0 y límite T0+15min. Esta propuesta por sí sola no ejecuta esa etapa.
3. Windows abre únicamente su túnel privado, comprueba estado/aislamiento y repite SONDA si el estado o la ventana han cambiado; devuelve `WINDOWS_LISTO_CERRADO` con campaña. Antes de abrir: inbox/ACK U4 cero, locks/custodia disponibles, QA saludable.
4. Ubuntu conserva backups exactos y modifica solo los gates necesarios del recorrido NNA/aviso. Conserva integración técnica y claves U4.2, CU/PISEE/adultos/mantenimiento/retirada deshabilitados y proxy solo loopback. Control autenticado CAS/If-Match: aceptar nuevos **true**, recuperar **false**, NORMAL, usando la revisión vigente.
5. Ubuntu realiza **un único** ingreso NNA sintético con el PNG; registra UUID público, versión/canal, documento y huellas, adjunto/tamaño/hash. Inmediatamente cierra nuevos ingresos por CAS y comprueba que existe exactamente esa entrega.
6. Ubuntu habilita PULL con `aceptarNuevos=false, recuperar=true, NORMAL` por CAS, si el contrato admite esa combinación. Si no la admite, detenerse y coordinar una alternativa cerrada antes de continuar. Devuelve `UBUNTU_ENTREGA_LISTA`, campaña, UUID público, revisiones y hashes, confirmando ausencia de entregas ajenas.
7. Solo tras esa devolución y la ventana autorizada: Windows abre exclusivamente el control U4 y ejecuta **un único CICLO**, que concilia primero y reclama como máximo una entrega. No tocar el control QA ni ejecutar un segundo ciclo para probar cola vacía.

Comandos previstos para esa etapa futura, **no ejecutados en esta comprobación**:

```powershell
$jar='C:\req\bin\siger-9a9e14f9c664b121.jar'
$ev='DIRECTORIO_PRIVADO_NUEVO_DE_LA_CAMPANA'
& .\scripts\Windows-Verificar-MySql-Ubuntu-U4.ps1 -Evidencia $ev -Control ABRIR -EnsayoSinteticoCoordinado
& .\scripts\Windows-Ensayo-Formulario-Ubuntu.ps1 -Jar $jar -Accion CICLO -EnsayoSinteticoCoordinado
```

El directorio nuevo debe crearse debajo de `C:\req\evidencias`, con ACL privadas y campañas anteriores preservadas. El helper usa las clases compiladas existentes; confirmar que siguen correspondiendo al candidato antes de reutilizarlo, sin compilar sobre el runtime activo.

8. **Validación cruzada:** Windows devuelve un COM/folio único en U4, registro durable, UUID público, comprobante Ed25519 y validaciones de documento/procedencia/originales. Verificar en SQL bajo app U4 una sola fila inbox/registro por UUID, metadatos de un adjunto, `ack_pending=false`, cero duplicados y eventos COM→ACK; validar el comprobante contra el trust auténtico y el PNG contra el SHA anterior. No imprimir cuerpo NNA ni secretos. Ubuntu devuelve el mismo UUID y ACK verificado, confirmación durable y estado funcional CONFIRMADA, sin nuevas entregas. Los datos U4 no se importan al buzón/folios de la QA.
9. **Cierre normal:** después de drenar, Ubuntu cierra PULL/recepción por CAS y restaura únicamente los flags temporales NNA/aviso a su respaldo U4.2. Mantiene la confianza e integración técnica de U4.2. Windows ejecuta `Windows-Verificar-MySql-Ubuntu-U4.ps1 -Evidencia $ev -Control CERRAR`, comprueba cero pendientes, y cierra la única sesión SSH. Conservar COM/ACK/original/evidencia sintéticos y verificar QA WSL sin cambios. Las revisiones CAS avanzan; no restaurarlas a 0 por SQL.

### Fallos y reversión de la ventana futura

Si no hay entrega, falla una firma/hash, aparece una entrega ajena o el ciclo no confirma: cerrar nuevos ingresos, registrar el estado y detener la prueba. No ejecutar automáticamente un segundo CICLO ni reenviar el formulario. Tras COMMIT/ACK ambiguo, conservar mismo origen/base/custodia/recibo y confianza necesaria para conciliación; acordar su resolución antes de retirar integración. Nunca purgar leases, regenerar claves, borrar datos o importar sobre QA/histórico.

La reversión lleva ambos controles a cerrado mediante sus helpers CAS, restaura los archivos temporales exactos del recorrido y cierra solo el túnel U4; mantiene el estado GO técnico U4.2. No utilizar la reversión completa U4.2 para cerrar una mera ventana de admisión sintética. Las interrupciones controladas se planificarán como casos separados; no forman parte de este único ciclo.

## Mensaje transferible a Codex Ubuntu

> Windows acredita U4.2 GO independiente: SONDA del 02/11/2026 11:04:07–11:04:08 America/Santiago devolvió 401/401/404 con TLS1.3, CA/SAN correctos y firma SIGER offline válida. Cero claims/ACK; U4 Windows y Ubuntu siguen revisión0 false/false SOLO_CONSULTA, cero Comunicaciones. QA WSL preservada; túnel cerrado.
>
> Proponemos campaña `fc1d267d-9cc0-436c-a926-6e5c1cc564ff`, un solo NNA sintético sin RUN/CU/PISEE y un PNG de200 bytes, SHA `2e39e298cd95f262ea68b437a0da65f34dc9087cabd0d3d4b1be9841e156f76e`, desde tu fixture `sinteticos/imagen.png`. Windows usará exclusivamente `siger_u4_ddn_ubuntu`, custodia U4 y el mismo JAR acreditado.
>
> Devuelve ahora LISTO_CERRADO con el UUID de campaña y confirma fixture/recorrido/secuencia, sin abrir controles ni crear envíos. Tras confirmación explícita del usuario y ambos responsables, se fija T0 (15min): abres solo NNA con PULL cerrado, realizas un ingreso real HTTP/CSRF, cierras nuevos ingresos y habilitas PULL por CAS. Devuelve UBUNTU_ENTREGA_LISTA con UUID público/revisiones/hashes; Windows realizará una sola apertura U4 y un CICLO. Después cruzamos COM/folio/originales/firmas/ACK/CONFIRMADA y cerramos ambos lados, conservando U4.2 y toda evidencia. Ninguna activación U4.4 está autorizada por este mensaje de propuesta.
