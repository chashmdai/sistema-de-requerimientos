import {useEffect,useState} from 'react';
import * as api from './api';
import type {Run} from './ui';
import {date} from './ui';

export function AreaCaso({c,actor,catalog,run,busy,changed}:{c:api.Case;actor:api.Actor;catalog:api.Catalog;run:Run;busy:boolean;changed:()=>void}){
 const [area,A]=useState(''),[reason,R]=useState(''),[error,E]=useState(''),[history,H]=useState<Awaited<ReturnType<typeof api.historialAreas>>>([]),[allowed,D]=useState<string[]>([]);
 useEffect(()=>{void api.historialAreas(c.id).then(H).catch(e=>E(e.message));void api.cambiosAreaDisponibles(c.id).then(D).catch(e=>E(e.message));},[c.id,c.revision]);
 const head=api.head(actor,c.unidad,c.sede,c.area);
 const targets=(catalog.areas??[]).filter(a=>a.unidad===c.unidad&&a.sede===c.sede&&a.id!==c.area&&allowed.includes(a.id)&&api.head(actor,a.unidad,a.sede,a.id));
 return <section className="card"><h2>Área acreditada del Caso</h2><p>{c.area?(catalog.areas?.find(a=>a.id===c.area)?.nombre??c.area):'Sin área acreditada · acceso histórico, sin pertenencia inferida'}</p><p>Unidad, sede, área y materia son dimensiones distintas. Un rol de unidad no concede acceso a las áreas.</p>
 {head&&targets.length>0&&<form onSubmit={e=>{e.preventDefault();void run(`/nucleo/casos/${c.id}/area`,{revision:c.revision,area,fundamento:reason,origen:c.area??null}).then(changed).catch(e=>E(e.message));}}><label>Área de destino acreditada<select required value={area} onChange={e=>A(e.target.value)}><option value="">Seleccionar área</option>{targets.map(a=><option key={a.id} value={a.id}>{a.nombre}</option>)}</select></label><label>Fundamento del cambio de área<input required maxLength={1000} value={reason} onChange={e=>R(e.target.value)}/></label><p>Se exige Dirección o delegación expresa acreditada, jefatura vigente en origen y destino y un responsable habilitado en destino. El cambio conserva asignación, original e historial.</p><button disabled={busy}>Acreditar cambio de área</button></form>}
 {error&&<p role="alert" className="error">{error}</p>}<h3>Historial de área</h3>{history.map((h,i)=><p key={i}>{h.previous_area??'Sin área'} → {h.area_id} · {h.actor_id} · {date(new Date(h.at_ms).toISOString())}<br/>{h.reason_text}</p>)}</section>;
}
