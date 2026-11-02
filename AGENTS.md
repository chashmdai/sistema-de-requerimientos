# Continuidad SIGER · GATE H2

Leer primero `docs/TRASPASO_CODEX_SIGER_H2.md` y `docs/ESTADO_GATE_H2_SIGER_2026-11-01.md`. Son la entrada vigente; los checkpoints anteriores conservan evidencia histórica, no instrucciones actuales de activación.

Continuidad remota U4: leer primero `docs/INFORME_FINAL_U4_4_WINDOWS_UBUNTU_2026-11-02.md`. U4.4 terminó con un E2E sintético físico y ambos lados cerrados; los informes anteriores conservan sus cortes históricos. La base `siger_u4_ddn_ubuntu`, COM-000001, original y ACK son evidencia preservada, no destino de fixtures. U4.5 (interfaz privada aislada) está pendiente y no se inicia automáticamente. No sustituir el perfil WSL de la QA por la URL Ubuntu, importar U4 a QA ni ejecutar V14 sobre las bases preservadas por rutina.

- El usuario autoriza trabajo únicamente dentro del alcance concreto de su solicitud. Publicar código no autoriza producción, cutover, servicios permanentes ni nuevas llamadas a proveedores.
- `DDN-TEST-SIGER` es desarrollo Windows Server + WSL. SIGER corre nativo Windows; Formulario en WSL es otra aplicación y otro responsable. DDN-UBUNTU es el destino del Formulario; futura VM Windows alojará SIGER, con datos de infraestructura aún pendientes. No inventar nombres/IP/puertos.
- Conservar runtime QA, procesos, registros, originales, claves, COM/ACK, evidencias y todas las ramas. El JAR activo vive fuera de Git en `C:\req\bin`; jamás sobrescribirlo al compilar. No reset/clean destructivos.
- La QA `siger_institucional_windows_qa` contiene E2E conservados: **no es descartable**. No ejecutar fixtures MySQL sobre ella ni importar/restaurar datos por rutina. La copia histórica `siger_institucional_windows_lab` y 24 originales tampoco se usan para tests.
- Build normal aislado: `siger-institucional/scripts/Build-Siger.ps1`, Java 25 explícito, Node 24, Maven, PowerShell 7.4+. No cambia Java global. Los 3 métodos históricos de estrés tienen tag `stress` y están excluidos por defecto; no añadir exclusiones para ocultar fallos.
- No detener procesos ajenos para conseguir RAM. Antivirus real exige 4 GiB físicos disponibles y límites existentes. Distinguir pruebas con dobles, workers reales, E2E y omisiones; no reportar como Windows una prueba Linux.
- Identidad individual y permisos persistentes: no elevar TI a lector universal ni SRCeI/RUN declarado a autenticación. No añadir facultades institucionales por inferencia.
- U5 es funcionalmente parcial: responder/canalizar/escalar Solicitud siguen cerrados por API 409 y UI. No habilitarlos sin definición institucional explícita.
- Secretos solo por canal privado aprobado, DPAPI/entrada privada y ACL. No leer configuraciones reservadas completas a consola; inspeccionar nombres/presencia/hashes sin mostrar valores. No secretos en Git, argumentos, logs, capturas o imágenes. No copiar DPAPI a otra máquina suponiendo que podrá descifrarse.
- Formularios y SIGER tienen contratos 1.0/1.1 y V3 histórico separados. ACK solo después de COMMIT y de originales íntegros/durables. No reescribir documentos o firmas anteriores.
- No editar V1–V13. Cambios de esquema exclusivamente mediante nuevas migraciones compatibles.
- Mantener Graph simulado y no realizar nuevas consultas CU/PISEE ni extracciones sobre otros orígenes sin autorización específica. No tocar Kaspersky, MySQL80 global, firewall/GPO, `C:\agente` u otros servicios.
- Cualquier actualización del Formulario la ejecuta su responsable; coordinar pausa/drenaje/reanudación del consumidor, sin modificar directamente su aplicación. No crear Comunicaciones en nombre del usuario.

Si una dependencia de administración o una garantía real no puede verificarse, registrar el bloqueo y continuar lo independiente. No eliminar controles para alcanzar un resultado verde.
