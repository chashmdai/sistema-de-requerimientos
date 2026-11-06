import {useState} from 'react';
import type {Run} from './ui';
import {date} from './ui';

export type PropuestaTermino={id:string;case_id:string;state:string;revision:number;reason_text:string;decision_text:string|null;requested_by:string;created_ms:number;decided_by:string|null;decided_ms:number|null;puedeRetirar:boolean;puedeDecidir:boolean;estadoCaso:string;historial:{revision:number;actor_id:string;at_ms:number;action_code:string;reason_text:string}[]};
type Props={registros?:PropuestaTermino[];run:Run;busy:boolean};

export function TerminosColaboracion(p:Props){
 const rows=p.registros??[];
 return <section className="card"><h2>Término de intervenciones colaboradoras</h2><p>La principal del REQ propone. La jefatura del Caso decide. Aceptar la propuesta conserva el Caso abierto: su profesional solicita término y su jefatura confirma por el circuito de cierre.</p>{!rows.length&&<p>No hay propuestas de término en este alcance.</p>}{rows.map(r=><PeticionTermino key={r.id} row={r} {...p}/>)}</section>;
}
function PeticionTermino({row:r,...p}:Props&{row:PropuestaTermino}){
 const [reason,setReason]=useState(''),[error,setError]=useState('');
 const send=(decision:string)=>void p.run(`/nucleo/institucional/colaboraciones/terminos/${r.id}/decidir`,{revision:r.revision,decision,fundamento:reason}).catch(e=>setError(e.message));
 return <article className="card"><h3>Propuesta de término · {r.state}</h3><p>{r.reason_text}</p><p>Solicitante {r.requested_by} · {date(new Date(r.created_ms).toISOString())}. Caso {r.case_id.slice(0,8)}: {r.estadoCaso}.</p>{r.decision_text&&<p>Decisión: {r.decision_text} · {r.decided_by} · {date(r.decided_ms===null?null:new Date(r.decided_ms).toISOString())}</p>}{r.state==='PENDIENTE'&&(r.puedeRetirar||r.puedeDecidir)&&<><label>Fundamento sobre la continuidad de la intervención<textarea maxLength={2000} value={reason} onChange={e=>setReason(e.target.value)}/></label>{r.puedeDecidir&&<><button disabled={p.busy||!reason.trim()} onClick={()=>send('ACEPTADA')}>Aceptar propuesta de término</button><button disabled={p.busy||!reason.trim()} onClick={()=>send('RECHAZADA')}>Rechazar propuesta y continuar intervención</button></>}{r.puedeRetirar&&<button disabled={p.busy||!reason.trim()} onClick={()=>send('RETIRADA')}>Retirar propuesta de término</button>}</>}{r.state==='ACEPTADA'&&r.estadoCaso!=='CERRADO'&&<p className="notice">Propuesta aceptada. El Caso sigue abierto hasta la solicitud y confirmación explícitas de su cierre. Equipo y agenda conservados.</p>}<details><summary>Historia de esta propuesta</summary>{r.historial.map(h=><p key={h.revision}>{h.action_code} · {h.actor_id} · {date(new Date(h.at_ms).toISOString())}<br/>{h.reason_text}</p>)}</details>{error&&<p role="alert">{error}</p>}</article>;
}
