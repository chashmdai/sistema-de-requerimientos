# Gates desde H2 hacia una VM Windows institucional

H2 consolida código/documentación verificables. No instala la VM, habilita producción, registra servicios permanentes ni cambia infraestructura. Cada gate posterior necesita entradas concretas, evidencia y autorización de su alcance; no basta editar un booleano de laboratorio.

| Gate | Entradas/resultado exigido | Condición de salida |
| --- | --- | --- |
| H2 · código y traspaso | Main auditado, ramas preservadas, build H2/frontend y candidato identificados, manuales y U5 separado | Nueva sesión puede reconocer/clonar/compilar/preparar sin chat; ejecución QA intacta |
| Infraestructura | Nombre/FQDN/IP, Windows, identidad operativa, discos/ACL/capacidad, MySQL, TLS/red y herramientas aprobadas | Inventario firmado por TI; ninguna colisión con servicios existentes |
| Arranque institucional | Modo distinto de los guardas QA, configuración portátil privada, esquema/usuario aprobados, identidad real y matriz de accesos | Pruebas de rechazo de configuración insegura, revocación y alcances; sin fallback compartido de laboratorio |
| Custodia y workers | NTFS privado, binarios/versiones/manifiestos confiables, AV/inspector/renderizador y límites | Ocho controles negativos reales y casos documentales positivos/rechazos; memoria de 4 GiB; sin excepciones globales |
| Datos | Migrador separado, aplicación DML, V1–V13 intactas y migraciones futuras aditivas | Compatibilidad/cifrado/TLS/FK/firmas/folios y pruebas en esquema descartable diferente |
| Integración remota | Origen aprobado en DDN-UBUNTU, DNS/TLS/CA/token/keys, vínculo de pendientes/origen, exclusividad persistente | E2E sintético nuevo PULL→originales→COMMIT→ACK→CONFIRMADA, sin duplicados |
| Recuperación/operación | Backup independiente y restauración, supervisión/drenaje, telemetría/alertas, respuesta a caídas y rotación | Ensayo de recuperación y procedimiento entendible para TI; responsabilidades/RPO/RTO aprobados |
| Habilitación | Validación funcional/seguridad/operación e institucional, datos reales y privacidad | Autorización explícita de puesta en marcha; no derivada de un push |

## Condiciones que siguen pendientes

- La futura VM Windows no tiene nombre/IP/DNS confirmados en este gate. No asignarle el nombre de desarrollo ni inventar un tercer host.
- `HabilitacionNucleo` exige perfiles `nucleo-lab,jdbc-reception`, entorno LABORATORIO, loopback y MySQL permitido; rechaza producción. `ArranqueWindows` acota QA Windows, puertos/rutas y origen loopback. Diseñar un modo institucional autorizado con pruebas antes de sustituir estos guardas; no eliminarlos.
- La identidad local de laboratorio tiene cuentas individuales/roles reales en la base, pero no SSO institucional. No habilitar Entra ID o una cuenta TI universal para completar un instalador.
- El lock del automático actual está acotado al esquema QA. Un destino remoto/institucional requiere ámbito/origen/exclusión explícitos, conservar confianza histórica y no conciliar pendientes contra otro productor.
- Servicio Windows/supervisión/usuario DPAPI requieren diseño y aprobación; el launcher CIM actual es continuo de desarrollo y no se reinicia automáticamente tras caídas/reinicio del anfitrión.
- Las 3 operaciones U5 pendientes no bloquean recibir/consultar Comunicaciones, pero no se habilitan durante el despliegue.
- Cuatro envíos Graph históricos no autorizan otra campaña. Mantener SIMULADO hasta una autorización distinta; no usar estados 202 como entrega confirmada.
- La retención/eliminación pública y el agente independiente no se configuran desde SIGER. No activar 72 h ni copiar lógica del productor a la custodia institucional.

## Antes de una prueba conjunta nueva

Identificar proceso/JAR/config efectivos de ambos lados, cola exclusivamente sintética y sin entregas ajenas, certificados/keys correctos, MySQL/NTFS/AV sanos, memoria y una sola instancia. No instalar automáticamente la última rama por haber sido publicada. Registrar UUID, COM, huellas, originales y ACK; distinguir simulaciones de evidencia real.

## Cierre de cada ventana

Registrar controles antes/después, datos/artefacto conservados, sondeos y estado final de ambas interfaces. No dejar pausado el consumidor por olvido. Si falla la sustitución, restaurar el artefacto previo/config bajo la misma ventana; no purgar colas ni reenviar en nombre del ciudadano.

Cuando una dependencia TI esté pendiente, preparar lo independiente y dejar un checkpoint con punto exacto de continuación. Nunca declarar habilitación productiva si falta un control o una prueba.
