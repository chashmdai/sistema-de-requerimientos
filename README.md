# SIGER institucional · Traspaso GATE H2

Desarrollo Nexbyte para la Defensoría de la Niñez. **Laboratorio funcional; producción no habilitada. U5 sigue funcionalmente parcial.**

**Una sesión nueva comienza en [TRASPASO_CODEX_SIGER_H2.md](docs/TRASPASO_CODEX_SIGER_H2.md) y [estado/comprobaciones H2](docs/ESTADO_GATE_H2_SIGER_2026-11-01.md).** No necesita el historial de conversaciones. Leer también [AGENTS.md](AGENTS.md) antes de operar sobre este checkout.

## Manuales vigentes

- [Arquitectura, topología y módulos](docs/ARQUITECTURA_MODULOS_SIGER.md).
- [Base de datos, identidad, roles y buzón Comunicaciones](docs/DATOS_ROLES_OPERACION_SIGER.md).
- [Preparación, build y ejecución Windows Server](docs/INSTALACION_PREPARACION_WINDOWS_SIGER.md).
- [PULL automático, ACK, idempotencia y conciliación](docs/OPERACION_PULL_ACK_SIGER.md).
- [Certificados, claves, DPAPI y configuración privada](docs/CONFIGURACION_PRIVADA_SIGER.md).
- [Respaldo, restauración y reversión no destructiva](docs/RESPALDO_RESTAURACION_SIGER.md).
- [Gates de despliegue institucional](docs/GATES_DESPLIEGUE_SIGER.md).
- [U5: implementado frente a decisiones pendientes](docs/U5_PENDIENTES_INSTITUCIONALES.md).
- [Contratos](contratos/README.md) y [cadena documental Windows](docs/OPERACION_DOCUMENTAL_WINDOWS.md).

## Compilar sin secretos ni servicios de negocio

Java 25, Maven 3.9+, Node 24/npm y PowerShell 7.4+. Si Java 25 ya es el seleccionado del proceso:

```powershell
pwsh -NoProfile -File .\siger-institucional\scripts\Build-Siger.ps1
```

Si el Java global es otro, seleccionar el JDK25 instalado mediante `-JavaHome`; [manual Windows](docs/INSTALACION_PREPARACION_WINDOWS_SIGER.md). El script ejecuta frontend lint/tests/build y backend H2/verify, con temporales privados. No requiere credenciales, MySQL ni Formulario. Excluye únicamente las 3 pruebas históricas de estrés por tag; los workers reales se califican por separado y sus omisiones se informan.

El JAR empaqueta React. No ejecutar `target` como runtime permanente ni sobrescribir el artefacto activo al compilar. La QA integrada y sus originales son datos preservados, **no fixtures descartables**.

## Topología y alcance

Desarrollo actual: Windows Server + WSL en **DDN-TEST-SIGER**. SIGER es nativo Windows y el Formulario corre en WSL del mismo equipo. Destino: **DDN-UBUNTU para el Formulario** y **una futura VM Windows para SIGER**, con infraestructura definitiva pendiente de TI.

SIGER conserva Comunicaciones, originales, procedencia y gestión institucional; inicia HTTPS PULL/ACK hacia el [Formulario independiente](https://github.com/chashmdai/formulario-web). Tiene cuentas individuales/roles, distribución central/regional, Casos/Requerimientos, documentos, tareas y avisos. Recibir no abre Casos automáticamente. El consumidor automático de QA está acreditado; no se confunde con un servicio productivo habilitado.

Preparaciones U5 y tareas personales independientes están incluidas; **responder/canalizar/escalar Solicitud permanecen deshabilitados en servidor e interfaz**. No hay permiso institucional implícito por publicarlos enmain.

Los checkpoints U1–U5 y manuales anteriores se conservan como historia. Las referencias de arriba prevalecen para continuidad; no copiar puertos, credenciales o permisos de una demo antigua. No se versionan secretos, originales, certificados privados, binarios documentales ni evidencias reservadas.
