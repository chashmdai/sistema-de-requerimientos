# SMR RC1 · checkpoint Windows 04/11/2026

Implementación sobre main/origin main verificados en `95cf2ae8173b4c6cab3eb8a1531659b8459dd898`, worktree solicitado `C:\req\worktrees\smr`, rama exclusiva `release/smr-recepcion-2026-11`. No se fusionó la rama de lógica ni se cambiaron migraciones V1–V14.

El operador confirmó que había extraído el expediente en `entrada-smr\handoff-smr-windows-20261104`. Se verificaron **88/88 hashes**, 89 archivos contando manifiesto y se conservó una copia nueva `verificado-smr-20261104`. El ZIP interior original no estaba disponible: no se afirma haber comprobado su hash de archivo. El ZIP público sí se verificó contra `1456fdf2c1c6be2e74cb71733c92ad33bf9376b4aabd7c962a100c604d0977ab`, junto con PEM/mapa/CA.

## Hecho y probado

- Entrada SMR institucional separada, sin perfiles/identidades sintéticas QA; backend restringido a recepción/consulta/operación y autenticación local individual. UI React integrada, filtros/paginación, detalle íntegro, comprobantes, originales y estado real.
- Reutilizados FuenteFormulario/EntregaCompleta/RecuperarFormulario, custodia NTFS, registro JDBC, folios y comprobantes existentes. Reserva ACK persiste en el mismo COMMIT del registro; I/O documental permanece fuera de la transacción SQL. Se conservan idempotencia, conflicto, ACK repetido/perdido y nueva reserva sin otro folio.
- Exclusión por base/origen, escritor por almacén, vínculo durable y guardia de custodia/ventana pública. No se atribuye coordinación entre dos máquinas a un lock local.
- Build normal Windows Java25/Node24/Maven/PowerShell: **54/54 pruebas frontend**, lint/build correctos; **287 métodos backend, 278 correctos, 9 omitidos, cero fallos/errores**. Las nueve omisiones son workers reales sin configuración; tres pruebas históricas stress siguen excluidas por su tag original. No se añadieron exclusiones. Hubo iteraciones fallidas de compilación/tests antes de corregirlas; solo el build final completo se acredita.
- Ambos vectores Ubuntu verificados con Java, aislados y reloj controlado; ambos NNA y ACK1.1. Nuevos tests de guardas SMR, permisos, CSRF, revocación, cierre de negocio, atomicidad de reserva y bootstrap recuperable. Batería existente conserva adulto declarado/CU controlada, extranjero, Unicode, nulls, discrepancias, firma/hash alterado, identidad desconocida, expiración/conflicto/caídas y recuperación.
- HTTPS local con productor exclusivo de pruebas: cinco Comunicaciones recibidas por consumidor automático; 16 originales SHA-256 íntegros; documentos1.0/1.1, NNA, ADULTO_DECLARADO y ADULTO_CU simulado, extranjero, Unicode, discrepancias y lote ampliado de12 adjuntos. Respuesta ACK perdida real del productor de pruebas, conciliada; cinco registros durables, cero ACK pendientes. No hubo consultas CU/SRCeI.
- Navegador: login, bandeja, detalle,12 adjuntos visibles, comprobante y consumidor pausado. Capturas sintéticas privadas `evidence\ui-*.jpg`; descarga de los16 originales verificada mediante API autenticada.
- Sonda HTTPS Java real pública HEAD:401 sin Bearer,401 con falso; CA del borde/hostname verificados. Bearer válido/404 pendiente. No POST reclamar, ACK o envío ciudadano público.
- TLS con CA incorrecta y Bearer inválido rechazados en HTTPS local. Backup cifrado/integridad y restore MySQL/NTFS sobre recursos nuevos: firmas, hashes, Flyway y exclusión comprobados. Un intento inicial de restore sin privilegio LOCK TABLES se conservó aparte y se corrigió el dump para no requerirlo; no se borró SQL para aparentar éxito.
- JAR portable y distribución con Java25 propio; PowerShell portable y wrapper existente para preparar servicio. Scripts externos de arranque, drenaje, actualización/reversión, provisión privada, respaldo/restore y recuperación cifrada.

## Instalación y límites

UI sintética `https://localhost:18443/`; receptor público preparado `https://localhost:18444/`. MySQL propio `127.0.0.1:13316`, esquemas/usuarios/almacenes separados. Credenciales aleatorias locales en DPAPI, visor privado; ninguna contraseña en este documento. Identidad ACK real `smr-windows-20261104-v1`, independiente de firmantes QA/fixtures. Paquete público y nota en `C:\req\smr\public`.

El consumidor sintético queda pausado tras la prueba; el público, desactivado por configuración. No hay servicio SMR registrado: la cuenta de desarrollo carece de administración Windows. Launchers locales independientes de la terminal y scripts de instalación SCM quedan identificados; arranque de servicio/DPAPI tras reboot requiere calificación TI. No se declara calificada la VM Java8: Java25 es portable y explícito, con chequeos de preservación global preparados/ejecutados en desarrollo.

No se habilitan representaciones sin workers reales ni se presenta el productor local como Ubuntu. No se acredita E2E público, respaldo fuera del anfitrión, RPO/RTO institucional o ausencia de impacto. La ejecución actual del SIGER y los archivos de C:\req\bin se preservan. Los consumos/puertos reales, reversiones y hashes finales se documentan en el informe de instalación externo a Git junto al release.

## Punto exacto de continuación

Windows completó el trabajo independiente de acceso Ubuntu. El operador entrega a su responsable `public\PARA_CODEX_UBUNTU.md` y el ZIP ACK, verificando huellas por canal independiente. Ubuntu instala confianza preservando el mapa histórico y comunica estado/apertura. El Bearer existente se importa únicamente con `Import-SmrBearer.ps1`; luego sonda401/401/404.

Antes de activar: confirmar custodia en este PC, alcance de toda la cola elegible, receptor único, respaldo y ventana PULL. Dejar referencia/ventana/huella en autorización privada, configurar transporte y activar desde UI. El operador realiza el envío autorizado. Verificar original, COMMIT, folio, ACK, confirmación Ubuntu y mismo registro visible. Traslado posterior conserva SQL/originales/folios/pendientes/confianza e ID de instalación, con entrega explícita de control; no recompila lógica ni toca Java8.

Manual completo: [SMR_OPERACION_WINDOWS.md](SMR_OPERACION_WINDOWS.md). El manifiesto del release identifica commit/JAR/runtime/dependencias. La CI remota se informa separadamente después del push, sin confundirla con la calificación Windows local.
