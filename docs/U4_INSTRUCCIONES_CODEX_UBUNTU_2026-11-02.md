# U4 · Instrucciones para el Codex responsable de DDN-UBUNTU

Estado Windows al 02/11/2026: preparación terminada; **ningún PULL de entregas ni ACK enviado a Ubuntu**. Este documento coordina una ventana sintética, sin apertura ciudadana. Leer primero los AGENTS y gates propios del Formulario. No tocar el Formulario WSL ni la QA SIGER preservada.

## 1. Referencias y material ya intercambiado

- Instalación nueva: `/srv/ddn-formulario-institucional`, proyecto `ddn-formulario-institucional`.
- Código efectivamente instalado: `099c3b16e71d57b335496c3a112daccfa38831db`. El checkout `7189a95cd449d1a5c6117116f1a360b4fb6b9f63` agrega documentación; no sustituir imágenes por esa diferencia.
- JAR del Formulario: SHA-256 `e7decf6e37ded80dd30e9e60a53cc028994346b82a493aa68b6277c0dd1f427c`.
- CA de origen: SHA-256 de archivo `9ce8ccfc0045660c4f46b88ccee91deca5e88890af7f6dec26c2ef7b7d31557e`.
- Mapa de firma del Formulario: SHA-256 `5620f27dafcbbc5fc0473d4a158f1823733820381b1322f7061060e5b88c0f2f`, key-id `publico-candidato`.
- **Clave pública ACK auténtica de SIGER ya entregada** en `/home/admin-ti/operacion-formulario/2026-11-02/siger-ack-trust-windows-u4.json`.
- SHA-256 de ese mapa ACK: `7888a45c690750b07be974394c327849749d68a98ecbb8e46e12cf43ab836767`, key-id exacto `institucional-nna-lab`. Es el firmante real del SIGER de desarrollo existente; su nombre no acredita una clave productiva nueva.
- Windows comprobó fuera de línea que la privada SIGER corresponde a esa pública, firmando/verificando un comprobante sintético en memoria. No creó COM para esa comprobación.
- Token PULL propio de Ubuntu aprovisionado en Windows bajo DPAPI, distinto del WSL. No volver a enviar tokens, contraseñas o privadas por chat. No se copió ninguna privada de firma entre servidores.

El 02/11 se comprobó en Ubuntu: `citizen_submission=0`, `public_delivery=0`, controles revisión 0 (`false,false,SOLO_CONSULTA`), `publico.integracion.habilitada=false` y ausencia de `keys/institutional-trust.json`. Windows recibe 503 con/sin credencial mientras ese gate permanezca cerrado.

## 2. Instalar únicamente confianza técnica, manteniendo recepción cerrada

1. Registrar controles, cantidades, imágenes/digests, PKI y hashes de configuración actuales en evidencia privada. Confirmar ausencia de otras entregas/consumidores. Respaldar los archivos exactos que se modificarán, con permisos privados.
2. Comprobar SHA/key-id/Ed25519 del mapa ACK anterior. Si ya existe un destino de confianza, comparar y coordinar; no sobrescribir una confianza distinta.
3. Instalar el mapa público auténtico en la ubicación prevista por el despliegue:

```bash
sudo install -o 1000 -g 1000 -m 0400 \
  /home/admin-ti/operacion-formulario/2026-11-02/siger-ack-trust-windows-u4.json \
  /srv/ddn-formulario-institucional/keys/institutional-trust.json
```

4. Seguir `docs/ARRANQUE_CERRADO_UBUNTU_2026-11-02.md` del código desplegado. En la configuración privada vigente, cambiar únicamente `publico.integracion.habilitada=true`. Conservar la referencia `/run/keys/institutional-trust.json`, la PKI, los tokens y las privadas existentes. Conservar recepción/adultos deshabilitados y controles persistidos cerrados. No ejecutar nuevamente un preparador que regenere claves o bases.
5. Recrear exclusivamente `app` con la imagen ya instalada y el Compose vigente. Verificar previamente que estas rutas/proyecto siguen siendo los efectivos:

```bash
sudo docker compose --project-name ddn-formulario-institucional \
  --env-file /srv/ddn-formulario-institucional/compose.env \
  -f /srv/ddn-formulario-institucional/release/infra/ubuntu/compose.yml \
  up -d --no-deps --no-build --force-recreate app
```

No imprimir `compose config`, `.env`, configuración privada completa ni headers con Bearer. No recrear MySQL, migrar datos WSL, abrir puertos o reiniciar servicios ajenos. Si el despliegue efectivo difiere, usar su comando verificado y documentar la diferencia.

6. Comprobar salud y HTTPS completo; comprobar CU y PISEE aún deshabilitados y contadores sin cambios. Avisar a Windows para repetir la sonda **sin reclamar ni confirmar**. Resultado esperado del contrato: HEAD sin token 401, token falso 401, token auténtico 404 sobre UUID nulos inexistentes. Si difiere, diagnosticar antes de abrir la ventana.

## 3. Ventana E2E sintética coordinada

Windows tiene una base **nueva** `siger_u4_ddn_ubuntu`, cuentas app/migrador exclusivas y custodia NTFS nueva. Ambas cuentas tienen denegado el acceso a QA/histórico. Los controles U4 Windows permanecen revisión 0, `SOLO_CONSULTA`. La QA SIGER sigue consumiendo WSL; no se pausa ni se cambia su perfil.

1. Acordar UUID de campaña, responsable y comienzo/fin. Verificar que el único consumidor de este origen será el ejecutor U4 Windows. Mantener el proxy únicamente en `127.0.0.1:24443`; acceso privado por SSH, sin túnel público.
2. Habilitar temporalmente solo el recorrido sintético necesario. Preferir NNA sin CU, adulto deshabilitado. El código desplegado usa `formulario.recepcion.habilitada` y el gate `publico.recepcion.aviso-validado`; revisar sus requisitos reales. No saltar validaciones, inventar una identidad CU/PISEE ni habilitar recorridos generales. Conservar hashes/valores previos para revertir. Recrear solamente app si hace falta.
3. Mediante control operacional autenticado y CAS/If-Match, abrir recepción sintética con PULL todavía cerrado. Crear **una** Comunicación sintética mediante el flujo HTTP real con sesión/CSRF y adjuntos autorizados, sin insertar directamente una entrega firmada por SQL. Registrar UUID público, tipo/versión, hashes/tamaños y cantidades iniciales. No usar RUN/personas reales ni importar datos WSL.
4. Cerrar nuevos ingresos tras ese envío; habilitar solamente PULL mediante el control soportado (`aceptarNuevos=false, recuperar=true, NORMAL` si lo admite el contrato). Si el código no admite esa combinación, coordinar la ventana mínima con ingreso inaccesible fuera del loopback y detenerse ante cualquier entrega ajena.
5. Comunicar UUID y estado listo a Windows. Windows abre exclusivamente sus controles U4 y ejecuta un ciclo con los comandos de la sección siguiente. No ejecutar un scheduler permanente ni cambiar el consumidor QA.
6. Verificar COM/folio/registro firmados y originales en Windows; comparar todos los hashes con Ubuntu. Comprobar el COMMIT durable antes de ACK; no aceptar solo la salida de un contador. En Ubuntu comprobar confirmación durable y estado funcional `CONFIRMADA` conforme al modelo real, conservación de UUID/ACK y ausencia de otra Comunicación. En Windows deben quedar `ack_pending=0` y un solo registro por operación.
7. Repetir un ciclo con la cola vacía: cero nuevas Comunicaciones/folios/archivos. Los folios de la base U4 pertenecen al ensayo aislado, no se importan ni mezclan con los folios de la QA preservada.

## 4. Comandos Windows del ensayo

Ejecutar desde `C:\req\repo` bajo la identidad DPAPI operativa, con el túnel iniciado. No contienen secretos:

```powershell
$ev=(Get-Content C:\req\scratch\gate-u4.txt -Raw).Trim()
$jar='C:\req\bin\siger-9a9e14f9c664b121.jar'
& .\scripts\Windows-Ensayo-Formulario-Ubuntu.ps1 -Jar $jar -Accion SONDA
```

Solo cuando la ventana sintética esté coordinada y la sonda sea 401/401/404:

```powershell
& .\scripts\Windows-Verificar-MySql-Ubuntu-U4.ps1 -Evidencia $ev -Control ABRIR -EnsayoSinteticoCoordinado
& .\scripts\Windows-Ensayo-Formulario-Ubuntu.ps1 -Jar $jar -Accion CICLO -EnsayoSinteticoCoordinado
```

El ejecutor concilia primero y reclama como máximo una entrega. Reutiliza `RecuperarFormulario`, `RecepcionInstitucionalJdbc`, `ArchivoInstitucional`, hashes/firmas y el COMMIT→ACK originales. Conservar stdout sanitizado en archivos nuevos de campaña, sin sobrescribir evidencia inicial. Validar por SELECT bajo app U4 (no root): `communication_registry`, `institutional_inbox`, `institution_document`, `operational_event`, `control_audit` y `consumer_origin_binding`; verificar recibos con las claves públicas y originales con SHA-256. No consultar contenido ciudadano en consola.

Cerrar Windows U4 al terminar, después de drenar el proceso:

```powershell
& .\scripts\Windows-Verificar-MySql-Ubuntu-U4.ps1 -Evidencia $ev -Control CERRAR
```

No ejecutar `MIGRAR`, fixtures, imports o restauraciones sobre QA/histórico. La base U4 ya está migrada V1–V14. No volver a aprovisionar usuarios ni rotar contraseñas por rutina.

## 5. Recuperación tras interrupciones

Las pruebas automáticas con productor controlado ya comprueban pérdida de red, reserva vencida, respuesta ACK perdida, reinicio, duplicados e interrupción de exclusión antes/después de COMMIT. Son evidencia aislada; falta el ensayo entre máquinas.

Para probar COMMIT antes de ACK real sin matar la QA:

1. Crear una segunda Comunicación exclusivamente sintética, con UUID registrado, sin otras entregas.
2. El responsable Ubuntu puede preparar una regla **temporal** del proxy privado que devuelva 503 solo a `POST /integracion/v3/formulario-completo/entregas/UUID_SINTETICO/confirmacion`, dejando reclamo/descarga disponibles. Validar Caddy, registrar/restaurar archivo exacto y recargar solo ese proxy. No bloquear otras rutas/UUID ni alterar firmas, base o reloj. No introducir la regla si no puede delimitarse y revertirse con garantías.
3. Windows ejecuta un ciclo. Exigir un COM durable y originales íntegros, ACK pendiente y cero confirmaciones públicas. No repetir el envío ciudadano.
4. Retirar la regla exacta, verificar TLS y esperar el backoff persistido. Ejecutar otro ciclo U4: la conciliación debe confirmar **el mismo** registro/folio/firma, sin otra descarga/COM; comprobar `ack_pending=0` y confirmación Ubuntu.
5. Para interrupción de descarga, usar otra entrega sintética y cortar únicamente el túnel U4 durante la transferencia; restaurarlo, esperar vencimiento/reclamación según protocolo y repetir. Comprobar ausencia de COM/ACK prematuros, temporales descartados y un único COM final. No terminar procesos QA ni alterar MySQL/almacenes.

Si algún fallo no se puede provocar/controlar exactamente, registrar el caso como no ejecutado. No presentar la batería con dobles como evidencia física nueva.

## 6. Cierre y respuesta que debe devolver Codex Ubuntu

Cerrar nuevos ingresos y drenar reservas/ACK. Si queda un ACK pendiente, mantener solo la confianza/ruta necesarias para conciliar y registrar el bloqueo; no borrar leases ni comprobantes. Después cerrar PULL/recepción por CAS, restaurar flags temporales y el proxy exacto, y comprobar persistencia tras recrear solo app cuando corresponda. La confianza pública auténtica puede conservarse en disco; dejar integración cerrada si no hay ventana autorizada.

Devolver un informe sin secretos con: SHA/imágenes efectivos, hashes públicos/key-id instalados, controles antes/después, UUID de campaña/entregas, COM/folios, hashes de documento/originales, resultado de validación ACK, confirmación durable, ausencia de duplicados, interrupciones realmente realizadas y estado final. Indicar expresamente CU/PISEE sin cambios y ninguna apertura ciudadana.

Actualmente **no se realizó ninguna de estas modificaciones activas en Ubuntu**. Solo se leyó estado, se intercambió material público y se aprovisionó el token en Windows mediante cifrado de transporte efímero. No declarar U4 E2E cerrado hasta reunir esa evidencia.
