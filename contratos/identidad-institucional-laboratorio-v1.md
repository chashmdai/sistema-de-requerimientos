# Identidad institucional de laboratorio v1
Extensión interna de SIGER. No cambia HTTP V3 público, firmas, COM o ACK. Solo cuenta activa por sesión HTTPS y CSRF en mutaciones; ningún actor/autor/procedencia se toma del navegador.

## Sesión
POST /nucleo/login recibe usuario/contraseña individual por formulario HTTPS. GET /nucleo/csrf entrega el token de sesión. GET /nucleo/session devuelve identidad estable, perfil y accesos vigentes; nunca hash/clave. POST /nucleo/logout invalida sesión. La versión de seguridad cambia en alteraciones de cuenta/permisos y una sesión anterior recibe 401 en la próxima petición.

La autenticación individual local no usa ClaveÚnica ciudadana. La futura federación deberá validar su proveedor y mapear el sujeto a actor_id; no convertir roles externos en permisos sin política institucional.

## Administración TI
Solo acceso TI vigente institucional:
- GET/POST /nucleo/ti/funcionarios: listado (hasta 500) y creación.
- PUT /nucleo/ti/funcionarios/{id}: perfil, ubicación, activo y revision esperada.
- POST /nucleo/ti/funcionarios/{id}/clave: clave individual nueva y revision; no devuelve secreto.
- POST /nucleo/ti/funcionarios/{id}/roles: rol, alcance, unidad/sede y desde/hasta.
- DELETE /nucleo/ti/funcionarios/{id}/roles/{asignacion}: revoca conservando autor/fecha.
- GET /nucleo/ti/organizacion.
- POST /nucleo/ti/organizacion/sedes: sede sintética LAB_, nombre, unidades y vigencia.
- POST /nucleo/ti/organizacion/coberturas y POST .../{id}/finalizar: cobertura/vigencia sin reescribir históricos.

No se implementan borrados físicos de funcionarios ni reemplazo de identidades históricas. Todos los cambios guardan autor del servidor y eventos; los perfiles tienen historia. Se exige conservar un TI actualmente activo. El bootstrap solo existe mediante archivo privado fuera de Git, validación de permisos y ausencia de cuentas previas.

## Roles y alcance
TI y DISTRIBUCION requieren INSTITUCION, sin unidad/sede en la asignación. COORDINADOR_REGIONAL requiere SEDE y sede habilitada. DIRECTOR, COORDINADOR, PROFESIONAL y ADMINISTRATIVO requieren UNIDAD_SEDE vigente. Una persona admite múltiples asignaciones; los intervalos son [desde,hasta), con hasta opcional. Revocación y expiración excluyen la asignación. No se autoriza por cargo, macrozona ni por la unión indebida de ámbitos de roles distintos.

## Recursos y operación
Todos los funcionarios activos pueden registrar manualmente. La sede real coincide con el perfil acreditado; null conserva ausencia de sede. Región del asunto opcional, distinta del origen y del destino. GET /nucleo/catalogo incorpora regiones sintéticas y operadores habilitados para seleccionar destinos/responsables; no concede acceso a su contenido.

Manual/Resumen agregan campos internos compatibles de región/origen/canal declarado. Los manuales nuevos con región usan MANUAL_2; los anteriores preservan MANUAL_1 y la serialización de comandos sin región. El documento ciudadano, la identidad CU/declarada/NNA y procedencia registral no se recalculan.

La distribución exige clave Idempotency-Key y revision. El principal opera institucionalmente; el regional solo sobre origen real de su sede. La región declarada y un destino ya asignado no habilitan distribución de un ingreso web sin sede. El servidor guarda evento de actor, origen y destino y rechaza duplicados en operaciones individuales. Se conserva el comportamiento de comandos históricos.

Caso y actuación requieren jefatura en el ámbito exacto o profesional responsable con asignación vigente. Los originales ciudadanos/gestión conservan las políticas documentales por recurso/contexto. TI no otorga lectura. Los vínculos no copian archivos ni amplían facultades. La recepción/distribución no abre agrupadores: abrir Caso es un comando separado de la jefatura destinataria.

## Estados HTTP
401: credencial/sesión/cuenta o versión de acceso no vigente. 403: operación no permitida, sede origen ajena o CSRF. 404: recurso/contexto fuera del alcance. 409: revisión, estado, destino repetido, comando incompatible o retirada del último TI. No exponer cuerpos ciudadanos, contraseñas, hashes o detalles SQL en errores.

Algunos fallos de unicidad/persistencia se presentan como error técnico conservador; refrescar el listado TI antes de repetir una creación incierta. No existe envío automático de credenciales ni recuperación externa de contraseña en esta unidad.
