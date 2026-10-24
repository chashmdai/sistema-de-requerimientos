# SIGER Institucional

Código vigente de laboratorio de SIGER 2026, desarrollado por Nexbyte para validación de la Defensoría de la Niñez. No es habilitación productiva.

- **[Manual Windows: instalación, configuración, compilación y arranque](docs/MANUAL_REPOSITORIOS.md)**.
- [Guía de esta aplicación](siger-institucional/README.md).
- [Integración completa 1.0/1.1: custodia, documentos y verificación](docs/INTEGRACION_FORMULARIO_COMPLETO_2026-10-24.md).
- [Checkpoint de integración institucional](docs/CHECKPOINT_INSTITUCIONAL_2026-10-24.md).
- [Contratos de integración](contratos/README.md).
- [Arquitectura de las dos aplicaciones](docs/ARQUITECTURA_APLICACIONES.md).
- [Reglas y operación del núcleo V8](docs/ANTECEDENTES_Y_REAPERTURAS_LAB.md).

## Compilar esta aplicación

Java 25, Maven 3.9+, Node 24 y PowerShell 7.4+.

```powershell
pwsh -NoProfile -File .\siger-institucional\scripts\Build-Siger.ps1
```

El build verifica frontend y backend y empaqueta React en su propio JAR. No requiere clonar el otro aplicativo. Para el laboratorio integrado MySQL/HTTPS y las cuentas sintéticas, seguir el manual; no basta ejecutar un JAR sin configuración.

El otro repositorio es [formulario-web](https://github.com/chashmdai/formulario-web). Las aplicaciones tienen bases, sesiones y despliegues independientes. SIGER inicia las conexiones HTTPS de recuperación; el navegador ciudadano no llama a SIGER.

Se excluyen secretos, certificados privados, bases, logs y evidencias de autenticación. Los fixtures son sintéticos y sus claves públicas permiten verificar firmas históricas. La documentación de entregas anteriores conserva contexto y resultados anteriores; sus capturas y archivos privados de evidencia no se publican.