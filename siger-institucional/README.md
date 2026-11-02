# SIGER institucional

Aplicación Java 25 / Spring Boot 4.1.1 con React y TypeScript empaquetados en su propio JAR. MySQL/Flyway, custodia documental nativa Windows, identidad individual y recepción HTTPS PULL/ACK independientes del Formulario Público.

**Entrada vigente:** [traspaso GATE H2](../docs/TRASPASO_CODEX_SIGER_H2.md) y [estado comprobado](../docs/ESTADO_GATE_H2_SIGER_2026-11-01.md).

- [Compilar y preparar Windows](../docs/INSTALACION_PREPARACION_WINDOWS_SIGER.md).
- [Arquitectura y topología](../docs/ARQUITECTURA_MODULOS_SIGER.md).
- [Identidad, roles, datos y Comunicaciones](../docs/DATOS_ROLES_OPERACION_SIGER.md).
- [Operación automática](../docs/OPERACION_PULL_ACK_SIGER.md).
- [Configuración privada](../docs/CONFIGURACION_PRIVADA_SIGER.md).
- [U5 funcionalmente parcial](../docs/U5_PENDIENTES_INSTITUCIONALES.md).
- [Gates institucionales](../docs/GATES_DESPLIEGUE_SIGER.md).

El build desde la raíz usa `siger-institucional/scripts/Build-Siger.ps1`, con Java 25 explícito cuando el global sea otro. Ejecuta frontend y H2 aislados; no requiere MySQL, claves ni el otro repositorio. Los workers reales se califican por separado.

No ejecutar `target` como runtime permanente ni lanzar la demo histórica sobre la QA preservada. Las guías anteriores de puerto 8443, contraseña compartida y núcleo inicial son historia. La QA actual usa HTTPS 8081, cuentas individuales y datos E2E que no se limpian como fixtures.

El núcleo permanece cerrado a producción mediante guardas explícitos. Publicar código no instala la futura VM ni habilita datos reales. No responder, canalizar o escalar Solicitudes U5 hasta definición institucional.
