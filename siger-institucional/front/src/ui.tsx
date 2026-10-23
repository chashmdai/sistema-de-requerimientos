import { useEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import type { Page } from './api';
export type Run = (route: string, payload?: object) => Promise<void>;
export const date = (v: string | null) => v ? new Date(v).toLocaleString('es-CL') : '—';
export const short = (v: string) => v.slice(0, 8).toUpperCase();
export const label = (v: string) => ({ REAPERTURA_PENDIENTE: 'Reapertura pendiente de jefatura', EN_GESTION: 'En gestión', REVISION_TERMINO: 'Término en revisión', CERRADO: 'Cerrado', ABIERTA: 'Por atender', PENDIENTE: 'Pendiente de evaluación', PENDIENTE_ASIGNACION: 'Pendiente de asignar evaluación', SIN_EVALUACION_HISTORICA: 'Vínculo histórico sin evaluación', ADMITIDA: 'Admitida', NO_ADMITIDA: 'No incorporada en este contexto', APROBADA: 'Reapertura aprobada', RECHAZADA: 'Reapertura rechazada', SUPERADA: 'Sustituida por actualización', EVALUANDO: 'Evaluación abierta', RESUELTO: 'Evaluación resuelta' }[v] ?? v.replaceAll('_', ' ').toLowerCase());
export function Status({ value }: { value: string }) { return <span className={`status state-${value}`}>{label(value)}</span>; }
export function TechnicalId({ value, kind }: { value: string; kind: string }) { const [copied, setCopied] = useState(false); return <span className="technical"><span title={value}>{kind} · {short(value)}</span><button className="icon-button" aria-label={`Copiar identificador de ${kind}`} title={value} onClick={() => { void navigator.clipboard.writeText(value).then(() => setCopied(true)).catch(() => setCopied(false)); }}>{copied ? 'Copiado' : 'Copiar ID'}</button></span>; }
export function Pager({ page, change, busy }: { page: Page<unknown>; change: (n: number) => void; busy: boolean }) { return <div className="pager"><span>{page.total} resultados autorizados · Página {page.pagina + 1}</span><div className="buttons"><button disabled={busy || page.pagina === 0} onClick={() => change(page.pagina - 1)}>Anterior</button><button disabled={busy || (page.pagina + 1) * page.limite >= page.total} onClick={() => change(page.pagina + 1)}>Siguiente</button></div></div>; }
export function Dialog({ title, children, close }: { title: string; children: ReactNode; close: () => void }) {
 const ref = useRef<HTMLDialogElement>(null);
 useEffect(() => { const previous = document.activeElement as HTMLElement | null; const el = ref.current; el?.showModal(); return () => { el?.close(); previous?.focus(); }; }, []);
 return <dialog ref={ref} aria-labelledby="dialog-title" onCancel={e => { e.preventDefault(); close(); }}><div className="dialog-heading"><h2 id="dialog-title">{title}</h2><button aria-label="Cerrar panel" onClick={close}>Cerrar</button></div>{children}</dialog>;
}
export function Empty({ children }: { children: ReactNode }) { return <div className="empty"><span className="empty-mark" aria-hidden="true">—</span><p>{children}</p></div>; }
