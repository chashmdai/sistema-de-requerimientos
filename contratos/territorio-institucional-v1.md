# Territorio institucional y constancia de principal · versión1.0

Requisitos REG-TER-01/02,DIS-02/05/08,REG-DIS-01,ORG-12; rector§§07.3/07.4/07.6/08.3. Validación funcional confirmada06/11/2026. Configuración nominal y despliegue son actos distintos, no concedidos por este contrato.

## Propuesta y acreditación independientes

Se reutilizan org_region/core_site/org_coverage y OrganizacionInstitucional. TI puede proponer una cobertura temporal sin solapamiento de región y finalizar la abierta, con identity_guard y auditoría. La propuesta por sí sola no autoriza competencia. No se replica un segundo catálogo de territorios.

Registro firmado de autoridad **1.8**, evolución explícita de1.7, conserva todos sus campos y añade lista obligatoria `coberturas` (vacía válida). Cada entrada: UUID`cobertura` de propuesta existente,`region`,`sede`,`desde`,`hasta` finito,`propuestaSha256`,`estado` ACTIVA/SUSPENDIDA,`referencia` y`sha256` del respaldo. Máximo64, sujeto también al límite de32768bytes del payload. Intervalo dentro del registro, sede regional declarada y propuesta; sin dos acreditaciones activas solapadas para la misma región.

Huella de propuesta: SHA-256 del JSON UTF-8 de la lista ordenada `["SIGER_COBERTURA_PROPUESTA_1",id,region_id,site_id,valid_from,valid_until]`, instantes enteros UTCms, último valor null cuando la propuesta es abierta. `CatalogoTerritorial.huellaPropuesta(UUID)` es auxiliar del canal privado, no endpoint público ni concesión. No incluye cambiador TI o reloj técnico: acredita contenido territorial exacto, no sus facultades.

La incorporación comprueba firma Ed25519/confianza/revisión y correspondencia exacta de propuesta en la misma transacción de autoridad. Cada uso vuelve a verificar raíz firmada vigente, estado/fechas, huella actual y sede vigente. Cambiar propuesta, suspender/retirar certificado o expirarlo cierra nuevas asunciones. No hay fallback desde TI, rol, sede de captura o materia. Cambio en otra cobertura no invalida por sí solo facultades ajenas.

Firmas1.0–1.7 conservan interpretación; campo`coberturas` bajo1.7 se rechaza.1.8 hereda la distinción1.7 entre materia funcional y unidad propietaria del Caso. No convierte facultad documental en permiso de Caso, área o territorio. Sobre y confianza se incorporan por las rutas privadas existentes `institucional.autoridad.sobre-archivo`/`institucional.autoridad.confianza-archivo`, ACL y lectura estricta. No se habilita importación de confianza desde HTTP.

## Decisión al asumir

El domicilio es del **requiriente**. No se extrae del NNA, territorio del hecho, origen web o sede de captura. La SOL conserva su `domicile_region` clasificado explícitamente.

- Competencia central: no regionalizar por domicilio; unidad/sede principal permanecen las de la decisión competente.
- Regional y domicilio UTILIZABLE: exigir una cobertura vigente acreditada hacia la sede receptora. Una sede diferente requiere redirigir SOL antes de REQ; no permite cambiar principal silenciosamente.
- AUSENTE/CONTRADICTORIO/EXTRANJERO/INUTILIZABLE: jefatura competente decide sede con fundamento y referencias. La condiciónAUSENTE no acepta región ya declarada. No se elimina ni sustituye esa declaración por la decisión. Sin antecedentes suficientes, conservar SOL pendiente; ninguna sede se inventa.

Nuevas rutas privadas explícitas:

`POST /nucleo/institucional/solicitudes/{id}/asumir/territorio`: versión1.0,revision,responsable,fundamento,condicionDomicilio,fundamentoTerritorial,referenciasTerritoriales.

`POST /nucleo/institucional/comunicaciones/{id}/asumir-propia/territorio`: mismos campos más destino y domicilioRegion. Solo jefatura autora del ingreso y facultada en destino; no tránsito artificial por central.

Requieren sesión individual vigente, CSRF, clave de idempotencia, ámbito/área y responsable habilitados, revisión y cerrojos identidad→COM. No conceden facultades por lectura de materia, TI, alias o pertenencia a otra área. Preflight propio GET`/comunicaciones/{id}/territorio?unidad=...&sede=...&area=...&region=...` verifica autor/lectura/jefatura antes de devolver orientación y audita la consulta. No revela cuerpo de otra COM.

Rutas anteriores mantienen DTO/acción/hash y reintentos confirmados. Para nuevos comandos anteriores, domicilio nulo usa fundamento territorial combinado ya admitido; nuevo contrato separa y exige referencias. Región utilizable ya puede resolverse por cobertura acreditada. No se cambian contratos públicos1.0/1.1 o COMMIT→ACK.

## Persistencia e integridad

No DDL nuevo: authority_batch conserva payload/firma original; institution_flow_history conserva constancia tipada1.0 `REQ_TERRITORIO_ACREDITADO` en la misma transacción de asunción. Campos: modo/condición/región declarada/sede inicial, revisión de autoridad/UUID/huella/respaldo para cobertura, o fundamento/referencias para excepción. No duplica relato o crea otra tabla de expedientes.

La constancia inicial no se recalcula al cambiar catálogo, abrir/cerrar/derivar Caso, consulta UPR/UPD, colaboración o transferencia posterior. El principal vigente conserva su circuito independiente. Históricos sin constancia permanecen explícitamente desconocidos; no backfill ni inferencia del primer Caso. Fallo del historial revierte SOL/REQ/comando/eventos completos; reintento confirmado no duplica.

V1–V15.16 intactas, no V15.17 ni outOfOrder por este bloque. Verificación de referencias remotas main/U4.5 sin cambio observado al diseñar; no operación sobre sus worktrees o bases. La restricción antigua de creación LAB y carga de catálogos nominales se documenta como preparación técnica operativa pendiente; no presenta datos sintéticos como instituciones reales.

## Interfaz y pruebas

SOL muestra cobertura vigente/orientación; solo ofrece asumir con condición evaluada y requisitos completos. Excepción conserva región declarada. Formulario propio consulta preflight real y no habilita mientras falta confirmación. REQ muestra constancia inicial o ausencia acreditada, separada de principal vigente. Backend repite todas las comprobaciones al confirmar, incluso si cambió la configuración desde la consulta.

Informe LOGICA_INGRESO_TERRITORIO_2026-11-06.md distingue H2/MockMvc/DOM/MySQL, CI propia y guion manual preparado. No acredita E2E físico, envío/proveedor real o habilitación productiva.
