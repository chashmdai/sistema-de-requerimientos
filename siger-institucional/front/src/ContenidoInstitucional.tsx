import {useRef,useState} from 'react';
import * as api from './api';
import {date} from './ui';

function useCommand(changed:()=>Promise<void>){
 const keys=useRef(new Map<string,string>());const [busy,B]=useState(false),[error,E]=useState('');
 async function command(route:string,body:object){const signature=JSON.stringify([route,body]);const key=keys.current.get(signature)??crypto.randomUUID();keys.current.set(signature,key);B(true);E('');try{await api.command(route,body,key);keys.current.delete(signature);await changed();}catch(e){E((e as Error).message);}finally{B(false);}}
 return {command,busy,error};
}
export function ProteccionDocumento({caso,documento,changed,soloRestringir=false}:{caso:string;documento:string;changed:()=>void;soloRestringir?:boolean}){
 const [control,C]=useState<api.ControlDocumento|null>(null),[error,E]=useState(''),[classification,K]=useState(''),[reason,R]=useState('');
 const [mode,M]=useState('consulta');const [target,T]=useState(''),[from,F]=useState(''),[until,U]=useState(''),[content,V]=useState(false),[download,D]=useState(false),[state,S]=useState('ACTIVA');
 async function load(){const v=await api.controlDocumento(caso,documento);C(v);K(soloRestringir?'BLOQUEADO':v.clasificacion);}
 const run=useCommand(async()=>{await load();changed();});
 const base=`/nucleo/casos/${caso}/proteccion-documental/${documento}`;
 const decisions=mode==='compartir'?(control?.comparticiones??[]):(control?.publicaciones??[]);
 const previous=decisions.find(p=>p.case_id===target);
 const targets=[...new Set([...(control?.contextos.map(c=>c.id)??[]),...decisions.map(p=>p.case_id)])];
 return <section className="card"><button onClick={()=>{void load().catch(e=>E(e.message));}}>{soloRestringir?'Restringir consulta documental':'Protección y consulta por materia'}</button>{(error||run.error)&&<p role="alert">{error||run.error}</p>}{control&&<><p>Clasificación: {control.clasificacion}. FIR conserva consulta del responsable y jefatura competente; las excepciones sensibles siguen cerradas.</p>
 <form onSubmit={e=>{e.preventDefault();void run.command(base,{revision:control.revision,clasificacion:classification,fundamento:reason});}}>
 <label>Protección del contenido<select required value={classification} onChange={e=>K(e.target.value)}><option value="">Seleccionar</option>{!['ORDINARIO','FIR','BLOQUEADO'].includes(classification)&&<option value={classification} disabled>{classification}</option>}{!soloRestringir&&<><option value="ORDINARIO">Ordinario</option><option value="FIR">Ficha Interna Reservada</option></>}<option value="BLOQUEADO">Consulta bloqueada</option></select></label>
 <label>Fundamento documental<textarea required maxLength={1000} value={reason} onChange={e=>R(e.target.value)}/></label><button disabled={run.busy||!['ORDINARIO','FIR','BLOQUEADO'].includes(classification)}>Registrar protección</button></form>
 {!soloRestringir&&<form onSubmit={e=>{e.preventDefault();void run.command(base+'/'+mode+'/'+target,{revision:previous?.revision??0,desde:new Date(from).toISOString(),hasta:new Date(until).toISOString(),contenido:content,descarga:download,estado:state,fundamento:reason});}}>
 <h4>Decisión específica de consulta documental</h4><label>Destinatarios del permiso<select value={mode} onChange={e=>{M(e.target.value);T('');F('');U('');V(false);D(false);S('ACTIVA');}}><option value="consulta">Lectores por materia</option><option value="compartir">Gestores del Caso de destino</option></select></label><p>El permiso exige referencia acreditada y jefatura sobre origen y destino. Lectores por materia requieren además materia vigente. Compartir con gestores no amplía sus facultades de gestión ni habilita excepciones entre unidades. Cambiar materia, área o protección retira esta habilitación.</p>
 <label>Contexto de consulta<select required value={target} onChange={e=>{T(e.target.value);const p=decisions.find(p=>p.case_id===e.target.value);if(p){const local=(n:number)=>{const d=new Date(n);return new Date(n-d.getTimezoneOffset()*60000).toISOString().slice(0,16);};F(local(p.valid_from));U(local(p.valid_until));V(p.content_allowed);D(p.download_allowed);S(p.status);}}}><option value="">Seleccionar Caso</option>{targets.map(id=><option key={id} value={id}>{id} · {control.contextos.find(c=>c.id===id)?.matter_id??'Concesión anterior'}</option>)}</select></label>
 <label>Desde<input required type="datetime-local" value={from} onChange={e=>F(e.target.value)}/></label><label>Hasta<input required type="datetime-local" value={until} onChange={e=>U(e.target.value)}/></label>
 <label><input type="checkbox" checked={content} onChange={e=>{V(e.target.checked);if(!e.target.checked)D(false);}}/>Permitir consultar contenido</label><label><input type="checkbox" disabled={!content} checked={download} onChange={e=>D(e.target.checked)}/>Permitir descargar original</label>
 <label>Vigencia de la decisión<select value={state} onChange={e=>S(e.target.value)}><option value="ACTIVA">Activa</option><option value="SUSPENDIDA">Suspendida</option></select></label><p>Se usa el fundamento documental indicado arriba. Los metadatos no conceden contenido ni descarga.</p>
 <button disabled={run.busy||!reason.trim()||!target||(state==='ACTIVA'&&control.clasificacion!=='ORDINARIO')}>Registrar alcance de consulta</button></form>}
 <h4>Historia de decisiones</h4>{control.historial.map((h,i)=><p key={i}>{h.action_code} · {date(new Date(h.at_ms).toISOString())} · {h.actor_id}<br/>{h.reason_text}</p>)}</>}</section>;
}
export function PertinenciaReq({requerimiento}:{requerimiento:string}){
 const [ctx,C]=useState<api.ContextoPertinencia|null>(null),[child,H]=useState(''),[proposal,P]=useState<{contenido:api.Detail['contenido']}|null>(null),[targets,T]=useState<string[]>([]),[content,V]=useState(false),[reason,R]=useState(''),[error,E]=useState('');
 async function load(){C(await api.contextoPertinencia(requerimiento));}
 const run=useCommand(async()=>{await load();P(null);H('');T([]);R('');});
 return <section className="card"><h3>Pertinencia del Requerimiento</h3><button onClick={()=>{void load().catch(e=>E(e.message));}}>Consultar jefatura principal acreditada</button>{(error||run.error)&&<p role="alert">{error||run.error}</p>}{ctx&&<form onSubmit={e=>{e.preventDefault();void run.command(`/nucleo/requerimientos/${requerimiento}/pertinencia/${child}`,{revision:ctx.hijas.find(h=>h.child_id===child)?.revision??0,casos:targets,contenido:content,fundamento:reason});}}>
 <p>Principal: {ctx.unidad} · {ctx.sede}. La decisión define pertinencia y no admite antecedentes automáticamente.</p>
 <label>Comunicación propuesta<select required value={child} onChange={e=>{const id=e.target.value;H(id);P(null);if(id)void api.propuestaPertinencia(requerimiento,id).then(P).catch(e=>E(e.message));}}><option value="">Seleccionar propuesta</option>{ctx.hijas.map(h=><option key={h.child_id} value={h.child_id}>{h.child_id} · revisión {h.revision}</option>)}</select></label>
 {proposal&&<p className="original">{proposal.contenido.texto}</p>}<fieldset><legend>Casos y materias pertinentes</legend>{ctx.casos.map(c=><label key={c.id}><input type="checkbox" checked={targets.includes(c.id)} onChange={e=>T(v=>e.target.checked?[...v,c.id]:v.filter(id=>id!==c.id))}/>{c.id} · {c.matter_id??'Sin materia'} · {c.state}</label>)}</fieldset>
 <label><input type="checkbox" checked={content} onChange={e=>V(e.target.checked)}/>Autorizar contenido de esta Comunicación en los contextos seleccionados</label><p>Los adjuntos requieren su propia protección y autorización documental. Una selección vacía retira la pertinencia anterior.</p>
 <label>Fundamento de pertinencia<textarea required maxLength={1000} value={reason} onChange={e=>R(e.target.value)}/></label><button disabled={run.busy||!child||!proposal}>Registrar pertinencia</button></form>}</section>;
}
export function AntecedentesPertinentes({caso,actor}:{caso:api.Case;actor?:api.Actor}){
 const [items,I]=useState<api.AntecedentePertinente[]|null>(null),[error,E]=useState('');
 async function load(){I(await api.antecedentesPertinentes(caso.id));}
 return <section className="card"><h3>Antecedentes con pertinencia acreditada</h3><button onClick={()=>{void load().catch(e=>E(e.message));}}>Consultar antecedentes pertinentes</button>{error&&<p role="alert">{error}</p>}{items?.length===0&&<p>No hay antecedentes pertinentes visibles en este contexto.</p>}{items?.map(v=><ValoracionCom key={v.comunicacion} value={v} caso={caso} editable={!!actor&&actor.id===caso.responsable&&!caso.soloLectura} changed={load}/>)}</section>;
}
function ValoracionCom({value:v,caso,editable,changed}:{value:api.AntecedentePertinente;caso:api.Case;editable:boolean;changed:()=>Promise<void>}){
 const [decision,D]=useState(''),[reason,R]=useState('');const run=useCommand(changed);const last=v.valoraciones[0]?.seq??0;
 return <article><h4>Comunicación {v.comunicacion}</h4>{v.contenido?<p className="original">{v.contenido.texto}</p>:<p>Contenido fuera de este alcance de consulta.</p>}{v.valoraciones.map(h=><p key={h.seq}>{h.decision} · pertinencia {h.pertinence_revision} · {h.actor_id}<br/>{h.reason_text}</p>)}
 {editable&&v.contenido&&<form onSubmit={e=>{e.preventDefault();void run.command(`/nucleo/casos/${caso.id}/antecedentes-pertinentes/${v.comunicacion}/valoraciones`,{revision:v.revision,anterior:last,decision,fundamento:reason});}}><label>Valoración en este Caso<select required value={decision} onChange={e=>D(e.target.value)}><option value="">Seleccionar decisión</option><option value="ADMITIDA">Admitir</option><option value="NO_ADMITIDA">No admitir</option></select></label><label>Fundamento de valoración<textarea required maxLength={2000} value={reason} onChange={e=>R(e.target.value)}/></label><button disabled={run.busy}>Registrar valoración o revisión</button>{run.error&&<p role="alert">{run.error}</p>}</form>}</article>;
}

