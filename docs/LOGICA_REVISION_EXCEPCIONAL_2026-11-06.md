# Continuidad excepcional por Caso y doble canal · 06/11/2026

Rama `codex/siger-continuidad-excepcional`, base **adb7eebf8dabe59c5467d746be158a8326dba79c**, PR #8. Cadena #2→#3→#4→#5→#6→#7→#8→continuación actual. Levantamiento validado; los parámetros nominales efectivos permanecen configurables y separados de las identidades sintéticas.

## Capacidades desarrolladas

SUB-CONT-01 para Caso propio de subrogante: solicitud profesional previa, necesidad fundada, revisor nominal con lectura vigente, autoridad independiente acreditada, intervalo finito, retiro/rechazo/revocación/expiración y confirmación expresa del Caso actual. No concede jefatura, equipo, gestión ni lectura adicional. Conserva REQ/Casos hermanos, responsable, cierres previos, tareas y autores.

Registro firmado explícito1.5, con facultades de autorización exactas por unidad/sede/área/materia; 1.0–1.4 conservadas. V15.13 H2/MySQL aditivas idénticas con FK/restricciones. Servicio/Controller/API y React integrados; auditoría de consultas/denegaciones/decisiones, aviso durable y revisión de documentos con huella. La modalidad EXCEPCIONAL distingue la facultad utilizada; una modalidad DERIVACION previa aplicable se conserva.

La revisión actual incluye actos y documentos por las políticas existentes. Se muestran bloqueos de obligatorias, espera y continuidad. Retirar acceso del equipo elimina también bandejas, paquetes y avisos. Un alias explícitamente incluido en equipo puede conservar su lectura propia, pero no hereda el permiso de confirmación de otra cuenta.

Precisión §16: la tabla EV habla de acuse, no de exención de correo. Se completa el doble canal en la proyección de eventos funcionales ya emitidos y se conserva NO_APLICA histórico sin retroenvío. Transporte real apagado; pruebas mediante dobles. Ver [contrato](../contratos/notificaciones-funcionales-doble-canal-v1.md).

## Arquitectura y componentes

`AutoridadesInstitucionales.Registro15/FacultadCierre`, `RevisionExcepcional/Controller`, ruta específica de cierre en `NucleoInstitucional`, elegibilidad de subrogancia acreditada, documentos por contenido/recurso, notificaciones/outbox y `RevisionExcepcional.tsx`. Reutiliza core_command e historias; no crea otro servicio o repositorio de expedientes. [Contrato por Caso](../contratos/revision-excepcional-caso-v1.md).

Caso y solicitud de cierre quedan vinculados explícitamente. Campos inmutables de la petición sellados; la autoridad aprobadora y su facultad se verifican otra vez al confirmar. La autorización se consume junto con cierre/historia en la transacción identity_guard→COM. Fuente y autoría no se sustituyen al cambiar función.

## Evidencia actual, sin anticipar CI

- Padre PR #8 publicado en adb7eeb: integral local **506 métodos, 497 aprobados, 9 omitidos, cero fallos/errores**; frontend **129/129**, lint/build/package, Maven27:33. MySQL final13/13 más86 previos según cortes del informe. CI42233659071: comprobar ambos jobs del mismo HEAD y registrar acta; no atribuir al hijo.
- Nueva selección H2: primer corte34 métodos tuvo33 aprobados y un fixture que intentaba incorporar documentos mientras el Caso estaba en revisión; corregido usando devolución fundada, gestión documental autorizada y solicitud nueva, conservando el bloqueo. 15 migraciones, HTTP1 y subrogancias13 aprobaron en ese corte.
- Corte posterior6/6 (cinco dominio y uno HTTP) aprobado. Selección ampliada **8/8** (siete dominio y uno HTTP) aprobada: alias, ámbito, actuales revisiones, obligatorias, reserva documental, expiración/revocación, suspensión de raíz, rollback, concurrencia y acceso indirecto. Se añadieron después aserciones de modalidad y avisos reales de proyección; acreditar sus focales/integral.
- **MySQL nativo Windows8.0.46 descartable23/23**, cero fallos/errores/omisiones: siete de excepción y dieciséis de migración/checksum, incluidas V14/V15.1…V15.12→V15.13. Directorio privado ecb626c0a4f74253bdbab26c472977b9, proceso propio ausente. Focal posterior **1/1** de modalidad EXCEPCIONAL en datadir64e06f4ad3da4d4a9c210bc25fd72f3c, proceso ausente. No sumar solapamientos como métodos únicos.
- Frontend **133/133**, TypeScript y lint aprobados en los cortes identificados. Primer intento tuvo importación con ruta incorrecta, corregida; no se ocultan sus cinco archivos sin ejecutar. Repetir validación integral al cerrar el bloque.
- Doble canal: selección focal en curso, `revision-excepcional-doble-canal.log`; no declarar esas modificaciones acreditadas hasta comprobar resultado. No se retiran pruebas ni exclusiones, timeouts o controles transaccionales.

Los tests de política documental usan metadatos/archivos sintéticos o dobles según fixture; no son un nuevo E2E físico ni validación de trabajadores reales. MySQL acredita el motor aislado y sus transacciones, sin acceso a bases preservadas. Graph/TransporteCorreo simulados no acreditan correo institucional entregado. Guion preparado, todavía no recorrido físico.

## Guion manual adicional por roles

En entorno nuevo descartable y con raíz de confianza sintética independiente:

1. Configurar función regional, candidato, titular y facultad1.5 de una autoridad superior/delegada. Concesiones base y credenciales individuales por U1.5, áreas vigentes y datos sintéticos.
2. Abrir REQ y dos Casos; candidato es responsable del primero. Incorporar persona revisora con lectura legítima solo en ese Caso. Activar la subrogancia mediante solicitud/aprobación/TI. Solicitar término profesional del Caso propio; intentar cierre propio y comprobar denegación.
3. Solicitar revisión excepcional fundada, cuenta revisora y período finito. Autoridad ve el paquete mínimo y aprueba; TI no recibe el expediente. Comprobar ausencia de autoaprobación y alias como confirmador.
4. Persona revisora abre el paquete, consulta actos/documentos, resuelve previamente obligatorias mediante sus ejecutores reales y confirma manualmente revisión con fundamento. El cierre debe registrar EXCEPCIONAL y mantener REQ activo/Caso hermano abierto. No se generan actuaciones por la aprobación o el cumplimiento de tareas.
5. Repetir con tarea obligatoria pendiente, nueva referencia documental o cambio de clasificación. Debe bloquear o exigir actualización. FIR necesaria sin permiso específico bloquea; esta habilitación no levanta esa reserva.
6. Revocar permiso o retirar membresía antes de confirmar. Comprobar que desaparecen listados/paquete/avisos del alcance retirado, incluso con sesión anterior. Repetir expiración y suspensión de facultad nominal.
7. En transporte simulado, revisar dos filas de canal/correlación. Leer SIGER no confirma correo. Probar aceptación técnica, ambigüedad, espera/reintento y revocación antes de enviar; no usar proveedores reales.

## Brechas y preservación

Acceso sensible excepcional ACC-05/10 requiere su propio respaldo de Gestión Institucional y jefatura responsable y no se deriva de SUB-CONT. FIR como ficha/actuación y rectificaciones especializadas, territorio/conciliación, plazos/reportes y eventos EV no emitidos continúan como implementación técnica. No se solicita otra validación funcional ni se inventan catálogos, titulares o credenciales.

V1–V15.12, main y padres preservados. No merge/despliegue, migración operacional o envío real. QA/U4.4/U4.5/SMR/COM-000001/bases/JAR/servicios/Formulario/proveedores/originales/firmas/hashes y COMMIT→ACK intactos. Referencias Git y CI definitivas se registran al publicar el incremento, sin atribuir éxitos de otro SHA.

Focal doble canal final:52/52 aprobadas, cero fallos/errores/omisiones. TransporteCorreo simulado, privacidad/deduplicación/revocación, NO_APLICA histórico conservado y límites previos intactos. CI padre: Ubuntu506/482/24 aprobado; Windows agotó30min con446/0fallos/0errores/9omitidos. Se conserva el intento42233659071 y se corrige la organización de CI, sin ampliar presupuesto ni excluir métodos. El hijo aún requiere integral/CI propias.
