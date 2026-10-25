# Cierre de identidad y operación territorial — tercera unidad
Cierre funcional de laboratorio iniciado el 24 y verificado el 25/10/2026. Producción no habilitada.

Repositorio: sistema-de-requerimientos. Rama: identidad-territorial-laboratorio-2026. Base documental conservada: 2e54edd y 3e5af85. Implementación: e0b002c. El commit posterior incorpora este informe, resultados, contrato, reversión y checkpoint.

## Resultado
Cuentas individuales persistentes, administración TI, roles múltiples con ámbito y vigencia, registro manual por todo funcionario activo, distribución principal/regional y autorización por recurso funcionan con HTTPS, MySQL y la custodia existente. La recepción pública 1.0/1.1, sus contratos, COM, firmas y ACK permanecen intactos.

Antes de editar se comprobaron Git limpio, historial, documentos de cierre/reversión, V1–V10 y las autorizaciones existentes. Se creó una rama local; no se reconstruyó ninguna unidad ni se modificó formulario-web.

## Identidad y autorización
V11 agrega cuentas vinculadas a core_actor mediante identidad estable; perfiles y su historia; asignaciones múltiples; eventos de cambio; regiones, macrozonas, vigencias de unidades/sedes y coberturas; origen histórico y eventos de distribución. Las FK documentales y los autores históricos conservan sus identificadores.

Cargo, ubicación del perfil, rol y ámbito son datos independientes. core_actor.role_code se conserva como campo histórico de compatibilidad y no otorga facultades a las cuentas individuales. Los actores sintéticos anteriores conservan sus referencias, pero no son cuentas habilitadas mediante la contraseña compartida.

Las contraseñas individuales se transmiten por HTTPS y se almacenan con BCrypt, coste 12. Mínimo 16 caracteres y máximo 72 bytes UTF-8. No se devuelven hashes ni contraseñas. La sesión conserva identidad estable y versión de seguridad; cada petición comprueba cuenta/versión y cada recurso resuelve asignaciones vigentes desde MySQL. Desactivar, cambiar perfil/clave, otorgar o revocar incrementa la versión e invalida las sesiones previas. La expiración se aplica en las consultas de autorización sin esperar un nuevo login.

Se mantienen CSRF, cookies Secure/HttpOnly, fijación de sesión, HTTPS, no-store y límites de login: ocho intentos por cuenta y treinta por IP/minuto, con mapa acotado. Se impide retirar el último TI actualmente activo. Estas decisiones son protección operativa del laboratorio, no una matriz institucional definitiva.

| Asignación | Ámbito autorizado | Facultad |
|---|---|---|
| TI | Institución | Administrar cuentas/asignaciones/organización; sin acceso automático a contenido |
| DISTRIBUCION | Institución | Distribuir y conservar las facultades históricas del distribuidor principal |
| DIRECTOR / COORDINADOR | Unidad y sede; múltiples asignaciones posibles | Misma función de jefatura, únicamente en cada ámbito |
| COORDINADOR_REGIONAL | Sede | Jefatura de la sede y distribución de Comunicaciones cuyo origen real sea esa sede |
| PROFESIONAL | Unidad y sede | Gestionar los Casos donde es responsable autorizado |
| ADMINISTRATIVO | Unidad y sede | Registro manual; no distribución |
| Funcionario activo sin roles | Ubicación acreditada del perfil | Registro manual y consulta del propio registro; sin facultad sobre Casos/documentos ajenos |

La facultad común de registro también alcanza a TI activo; eso no concede lectura de ingresos o Casos de terceros. Compartir sede o macrozona, otra función, el cargo o conocer un UUID no suple una autorización. Las consultas SQL y las políticas de detalle/descarga evitan combinar una jefatura de A con una asignación profesional de B para convertirse en jefatura de B.

La frontera de autenticación es Spring Security/UserDetailsService con principal normalizado e identidad local estable; las facultades son del servidor. Un futuro SSO requerirá validar proveedor y mapear su sujeto a esa identidad, manteniendo versión de seguridad y política por recurso. No existe un proveedor Entra activo ni se aceptan roles de un navegador o de claims no integrados.

## Operación territorial y documentos
El registro manual fija la sede a partir del perfil de sesión y rechaza una sede manipulada. Una cuenta sin sede acreditada registra origen nulo. La región del asunto es opcional e independiente; no determina destino. Los nuevos manuales con región incorporan esa declaración en una huella MANUAL_2, preservando el cálculo histórico MANUAL_1 y las claves de reintento anteriores.

El Buzón y el detalle presentan origen, canal técnico, canal declarado y sede mediante texto. Un ingreso web mantiene sede nula; recibirlo o consultar su destino no autoriza distribución regional. El coordinador regional puede distribuir un ingreso originado en su sede a un destino institucional habilitado. La jefatura destinataria evalúa después. Distribuir nunca abre Casos/Requerimientos.

Se validan actor, cuenta, vigencia, ámbito, estado, revisión y clave de idempotencia. Las nuevas operaciones individuales rechazan destinos repetidos para la misma unidad/sede. La auditoría guarda actor, fecha, sede de origen y destino. Los comandos anteriores y su semántica histórica permanecen.

Una sede admite varias regiones. Las coberturas usan intervalos [desde, hasta); cambiar una cobertura no cambia el origen de una Comunicación. La API TI permite agregar una sede de laboratorio y sus unidades sin modificar código/migraciones. Los catálogos REG_LAB y DEMO_SEDE son sintéticos; no constituyen organigrama ni cobertura definitiva DDN. La macrozona no otorga permisos.

AutorizacionNucleo y AutorizacionDocumental reutilizan la política vigente. Las mutaciones documentales respetan el orden de bloqueo identidad → Comunicación. No se duplica ni sustituye un original, ni se flexibiliza su lectura por ser creador, TI o pertenecer a otra función.

## Verificación
| Evidencia | Resultado |
|---|---|
| Backend H2 | 160 correctas; cero fallos/errores/omitidas en selección final |
| Backend MySQL 8.4 | 160 correctas; cero fallos/errores/omitidas en selección final |
| Frontend | 33 correctas; lint/build correctos |
| Nuevas pruebas backend | 10 identidad/territorio, 5 HTTP/sesión y 3 autorización documental |
| Workers documentales | Cinco pruebas reales habilitadas expresamente en H2/MySQL |
| Navegador real Edge | TI crea cuenta y rol desde React; login individual; registro manual; distribución regional/central; operación profesional |
| Seguridad real | Cuenta compartida retirada 401; CSRF TI 403; sede ajena 403; otra unidad/TI en Caso/documento 404 |
| Revocación/desactivación | Sesión existente 401; reautenticada sin rol, recurso 404; cuenta rehabilitada mediante TI |
| Roles múltiples | Coordinador UPD y profesional UPR no obtiene jefatura ni documentos ajenos de UPR |
| Persistencia/reinicio | Repetición sobre artefacto final sin duplicar COM, destinos ni Caso |
| Interfaz | Escritorio 1440×1000 y móvil 390×844; visor histórico PDF página 2 y zoom 200% |

Los casos de servicio usan datos/dobles sintéticos; la demostración usa cuentas sintéticas con backend real, MySQL, sesión HTTPS y originales reales de laboratorio. No usa dobles para declarar completado el recorrido institucional. No hubo llamadas gubernamentales ni cambios al Formulario Público.

La comparación de custodia confirma siete Comunicaciones previas con cuerpo, COM, comprobante, firma y ACK sin cambios; dieciséis originales ciudadanos, dos de gestión, tres vínculos y seis páginas derivadas íntegros por tamaño/SHA. Cero temporales y cero ACK pendientes. V1–V10 permanecen byte a byte iguales.

El laboratorio suma dos Comunicaciones manuales, dos distribuciones, un Caso y un Requerimiento creados expresamente por comandos autorizados. Total: nueve COM únicos, tres Casos y dos Requerimientos. Los dos Casos documentales previos se conservaron. Hay ocho cuentas individuales activas y tres coberturas históricas; los reintentos no aumentaron esos registros.

## Incidencias corregidas
- Un selector de TI no tenía nombre accesible exacto en navegador. Se añadieron etiquetas explícitas y se verificó el formulario real.
- La reanudación de QA confundía destinos no visibles de otra sede con ausencia de distribución. Se corrigió para conservar/reutilizar el comando confirmado; el estado privado se reconcilió por lectura, sin modificar registros institucionales.
- Un archivo de credenciales sintéticas escrito mediante Windows heredó permisos más amplios de archivo dentro del directorio Linux 0700. Se ajustó a 0600; el navegador ahora solo lo lee y su preparación se realiza en Linux. No se imprimieron valores ni se incorporaron a Git.
- Un filtro preliminar mal escrito incluyó por error el test H2 de 72 duplicados/24 hilos. Se detuvo la batería al detectarlo. Fue una prueba sintética aislada; no accedió a MySQL integrado ni a proveedores. Las baterías finales excluyen correctamente los tres métodos de carga históricos. El nuevo lanzador valida sus nombres antes de ejecutar Maven.

## Evidencias y reproducción
Evidencias privadas: .local/evidencias/identidad/estado.json, resultado-navegador.json, custodia-verificada.json y capturas. El estado permite reanudar con claves previas. No borrar ese estado para repetir sobre la misma custodia.

Logs ignorados: .test-identidad-h2.log, .test-identidad-mysql.log, .test-identidad-front.log, .lint-identidad.log, .build-identidad-front.log y .package-identidad.log. Los avisos no fatales de Flyway/H2 y Mockito, y un aviso act de React ya presente, no se contabilizan como fallos.

Preparación inicial y operación, siempre con el directorio privado existente:
1. Preparar-Identidad-Linux.py conserva configuración anterior y genera el material inicial privado; no imprime secretos.
2. Institucional-Completo-Linux.py copia únicamente un JAR inactivo con la instancia detenida; --migrar valida/aplica V11. El JAR activo permanece fuera de target.
3. Preparar-Credenciales-QA-Identidad-Linux.py crea/conserva credenciales sintéticas distintas con modo 0600.
4. Regresion-Identidad-Linux.py <privado> --fixtures <fixtures-documental-propias> ejecuta H2; agregar --mysql utiliza exclusivamente robust_inst_test y credenciales separadas de migración.
5. En front: npm test, npm run lint, npm run build.
6. Probar-Identidad-Institucional.mjs utiliza QA_PLAYWRIGHT_PACKAGE, INSTITUCIONAL_LAB_PRIVATE y NODE_EXTRA_CA_CERTS. Reutiliza Playwright instalado, no ignora TLS y limita destinos a loopback 20443.
7. Verificar-Custodia-Identidad-Linux.py <privado> --evidencias .local/evidencias/identidad utiliza el acceso Docker autorizado para SELECT y comprueba originales por streaming.

HTTPS 20443, robust_inst_formulario_lab, robust_inst_test, almacenamiento y workers existentes fueron reutilizados. No se crearon servicios, bases ni volúmenes. El JAR final coincide con el activo; huella y resultados están en RESULTADOS_IDENTIDAD_2026-10-25.json.

## Límites y siguiente unidad
No es producción lista. Falta la validación del catálogo institucional definitivo y la integración SSO, su recuperación/MFA y política definitiva de credenciales. El directorio contiene únicamente identidades sintéticas; habilitar personas reales requiere el procedimiento institucional autorizado.

La administración lista hasta 500 cuentas; es suficiente para este laboratorio y requiere paginación/búsqueda para escalar. No se implementaron equipos, subrogancias, tareas, notificaciones externas ni auditoría transversal detallada. Las asignaciones con vigencia, autores y política por recurso son sus puntos de extensión; no se inventaron aprobadores, plazos o facultades.

La campaña de capacidad, respaldos externos/recuperación, retención y hardening productivo sigue siendo otra unidad. No se hizo push, no se habilitó producción ni se reemplazaron claves/credenciales institucionales existentes.

[Contrato](../contratos/identidad-institucional-laboratorio-v1.md) · [Reversión](REVERSION_IDENTIDAD.md) · [Checkpoint](CHECKPOINT_IDENTIDAD_2026-10-25.md).
