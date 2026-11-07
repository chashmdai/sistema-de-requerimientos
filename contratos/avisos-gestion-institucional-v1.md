# Avisos de asignación y término · 1.0

Fuente vigente: rector28/10 §16, EV-06/11/18, validación funcional06/11/2026. Extensión privada aditiva del [circuito de avisosSOL/REQ](avisos-nucleo-institucional-v1.md). No cambia el payload1.0 de ese contrato ni mandatos firmadosU1.5.

## Hechos y destinatarios

- CASO_ASIGNADO: apertura institucional y reasignación efectiva. Entrante y jefatura actuales; saliente exacto de la asignación anterior recibe referencia mínima del cambio. La lectura no acusa asignación; el acuse manual sigue vinculado a institution_assignment_current/receipt.
- CASO_TERMINO_DEVUELTO y CASO_CERRADO: resultado de revisión para responsable, equipo actualmente autorizado y jefatura competente. No cierran el REQ ni otros Casos.
- REQ_TERMINADO: cierre explícito válido, incluida primera atención ya resuelta sinCaso. REQ_REACTIVADO: acto efectivo coordinado con reapertura, nunca su autorización todavía pendiente. Destinatarios: jefatura/encargado con consultaREQ, responsables de susCasos con permiso actual y registrador original con consulta ordinaria autorizada. No se transmite el expediente mixto a estos destinatarios.

El saliente no recupera Caso, documentos, tareas, REQ o pertenencia por recibir el aviso. Su referencia queda nominativamente vinculada a la cuenta que consta en core_assignment anterior; no basta coincidir en persona canónica/rol. Se exige cuenta habilitada, vínculo institucional vigente, raíz auténtica vigente y huella de credencial acreditada. Puede conservar la constancia mínima de su salida aunque ya no gestione el Caso; una revocación de acceso al expediente no se convierte en acceso mediante el aviso. Deshabilitar cuenta, expirar acreditación o sustituir credencial sin nueva acreditación retira también esta referencia. TI no recibe un permiso general.

Los demás destinatarios se resuelven por autorizaciones actuales; la pertenencia anterior no habilita contenido. Ser lector de materia no equivale a ser gestor/miembro/responsable. El enlace al Caso/REQ se entrega solo si la consulta actual de ese recurso está autorizada, también para un saliente que conserve otra facultad independiente acreditada.

## Historia y entrega

AvisosGestion.Datos1.0 conserva tipo, contexto, REQ/Caso, asignaciones exactas anterior/actual, responsables correspondientes, revisión, estado y registrador si existe. Se guarda en institution_flow_history con AVISO_GESTION_PROGRAMADO y workflow_event resource_type=AVISO_GESTION. Reutiliza el escritor transaccional del contrato anterior y la referencia AVISO_U2:<seq_historia>; decodifica cada payload contra su propio action_code/tipo/version, sin reinterpretaciones silenciosas.

Fuente, recurso, revisión, contexto y asignaciones se verifican. Errores de datos fallan cerrados; fallos JDBC se propagan para recuperación, no se presentan como recurso inexistente. Eventos de apertura/asignación históricos conservan su evidencia; cuando hay intención tipada del Caso no se vuelve a proyectar la señal provisional CASO_ASIGNADO como otro aviso. No se reescribe lo ya proyectado ni se crea backfill masivo.

Son constancias de decisiones realizadas. Un cierre anterior no desaparece al reabrir; su aviso no obliga a repetirlo. pendienteActual solo informa del acuse de la asignación exacta todavía actual y sin acuse manual a quien conserva consulta autorizada delCaso. El saliente sin esa consulta no conoce siquiera el estado del acuse del entrante. La bandeja, contador, consulta individual y correo revalidan antes de revelar o transportar. Dos proyecciones concurrentes generan un único aviso/outbox por evento/cuenta. Fallar la persistencia de intención revierte la decisión de negocio y su comando, no deja un cierre sin constancia.

## API e interfaz

Reutiliza GET /nucleo/notificaciones/{id}/contexto-institucional y #aviso-institucional/{id}, mediante notificación propia visible. Devuelve únicamente la vista mínima del contrato anterior, con título del nuevo tipo. No contiene objetivo, relato, observaciones de cierre, documentos o tareas. Marcar leído continúa como POST con CSRF y no ejecuta otra gestión.

Continuidad del Caso y fichaREQ muestran preparación/enrutamiento separado del acuse y transporte. Cambiar Caso/versión descarta datos anteriores antes de resolver la autorización del siguiente contexto. Correo reusa plantilla mínima/enlaceHTTPS y outbox del padre, con los mismos controles de laboratorio; ningún envío real se habilita por este contrato.

## Compatibilidad

Sin nueva migración; V1–V15.16 intactas y mismos historiales/asignaciones/cierres/períodos. No se modifican firmas ni transporte público1.0/1.1/originales/COMMIT→ACK. La nueva comprobación de credencial se usa para referencias nominativas mínimas; no amplía ni reinterpreta concesiones anteriores. Otros eventos de§16 y comunicación externa se califican aparte.
