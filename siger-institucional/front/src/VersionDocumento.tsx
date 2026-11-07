import {useEffect,useRef,useState} from 'react';
import * as api from './api';import {date} from './ui';
type Referencia={id:string;nombre:string;consultable?:boolean};
export function VersionDocumento({base,documento,documentos,version,denegado}:{base:string;documento:Referencia;documentos:Referencia[];version:string;denegado:()=>void}){
 const [open,O]=useState(false),[data,D]=useState<api.VersionDocumental|null>(null),[error,E]=useState(''),[success,S]=useState(''),[refresh,F]=useState(0);
 const [title,T]=useState(''),[type,Y]=useState(''),[reason,R]=useState(''),[external,X]=useState(''),[previous,P]=useState(''),[candidate,C]=useState<api.VersionDocumental|null>(null),[busy,B]=useState(false),[pending,Q]=useState(false);
 const generation=useRef(0),selection=useRef(0),deny=useRef(denegado);deny.current=denegado;
 const attempt=useRef<{op:string;body:object;key:string}|null>(null);
 useEffect(()=>{if(!open)return;const abort=new AbortController();generation.current++;selection.current++;D(null);C(null);P('');E('');S('');T('');Y('');R('');X('');B(false);Q(false);attempt.current=null;
  void api.versionDocumental(base,abort.signal).then(v=>{if(!abort.signal.aborted){D(v);T(v.titulo??documento.nombre);Y(v.tipoDeclarado??'');X(v.referenciaExterna??'');}}).catch(e=>{if(!abort.signal.aborted){E(e.message);if(e instanceof api.ApiError&&(e.status===403||e.status===404))deny.current();}});return()=>{abort.abort();generation.current++;selection.current++;};
 },[base,documento.nombre,version,open,refresh]);
 async function elegir(id:string){const current=++selection.current;P(id);C(null);if(!id)return;try{const v=await api.versionDocumental(`${base.slice(0,base.lastIndexOf('/'))}/${encodeURIComponent(id)}`);if(current===selection.current)C(v);}catch(e){if(current===selection.current){E((e as Error).message);if(e instanceof api.ApiError&&(e.status===403||e.status===404))deny.current();}}}
 async function enviar(op:string){if(!data?.editable||busy||!reason.trim())return;
  if(!attempt.current){let body:object;if(op==='metadatos'){if(!title.trim()||!type.trim())return;body={revision:data.revision,titulo:title,tipoDeclarado:type,fundamento:reason};}else if(op==='finalizar'){if(!data.finalizable||data.fase!=='BORRADOR')return;body={revision:data.revision,referenciaExterna:external.trim()||null,fundamento:reason};}else{if(!data.permiteSucesion||!candidate?.acreditada||!previous)return;body={revision:data.revision,revisionAnterior:candidate.revision,anterior:previous,fundamento:reason};}attempt.current={op,body,key:crypto.randomUUID()};}
  const a=attempt.current,current=generation.current;B(true);E('');S('');
  try{const v=await api.versionDocumentalMutacion(base,a.op,a.body,a.key);if(current===generation.current){attempt.current=null;Q(false);D(v);R('');P('');C(null);S('Declaración conservada con autor, fundamento e historial. El original permanece íntegro.');}}
  catch(e){if(current===generation.current){E((e as Error).message);if(e instanceof api.ApiError&&e.status<500){attempt.current=null;Q(false);if(e.status===403||e.status===404){D(null);deny.current();}}else Q(true);}}finally{if(current===generation.current)B(false);}
 }
 const nombre=(id:string)=>documentos.find(d=>d.id===id)?.nombre??id;
 return <details onToggle={e=>O(e.currentTarget.open)}><summary>Metadatos y versiones de {documento.nombre}</summary>{open&&<>
  <p>La declaración de versión final en SIGER no acredita firma digital, visación ni despacho externo. Los originales se conservan separados.</p>
  {error&&<p role="alert">{error}</p>}{success&&<p role="status">{success}</p>}{!data&&!error&&<p role="status">Consultando versiones autorizadas…</p>}
  {data&&<><p>{data.acreditada?`Versión ${data.numero} · ${data.fase==='FINAL'?'Final declarada':'Borrador documental'}`:'Sin fase o versión acreditada. No se deduce del nombre o de la carga.'}</p>{data.titulo&&<p>Título declarado: {data.titulo} · Tipo declarado: {data.tipoDeclarado}</p>}
   {data.finalDeclaradoEn&&<p>Final declarada por {data.declaradoPor} · {date(data.finalDeclaradoEn)}. Referencia externa: {data.referenciaExterna??'Sin referencia declarada'}</p>}
   {data.anterior&&<p>Antecedente autorizado: {nombre(data.anterior)}</p>}{data.siguiente&&<p>Sucesora autorizada: {nombre(data.siguiente)}</p>}
   {data.editable&&<><fieldset disabled={busy||pending}><legend>Declaración o corrección de metadatos</legend><label>Título del documento<input maxLength={180} value={title} onChange={e=>T(e.target.value)}/></label><label>Tipo documental declarado<input maxLength={120} value={type} onChange={e=>Y(e.target.value)}/></label><label>Fundamento del cambio documental<textarea maxLength={2000} value={reason} onChange={e=>R(e.target.value)}/></label></fieldset>
    {!pending&&<button disabled={busy||!title.trim()||!type.trim()||!reason.trim()} onClick={()=>void enviar('metadatos')}>{data.acreditada?'Rectificar metadatos conservando el anterior':'Declarar metadatos y borrador'}</button>}
    {data.finalizable&&data.fase==='BORRADOR'&&!pending&&<><label>Referencia externa, si existe<input disabled={busy} maxLength={500} value={external} onChange={e=>X(e.target.value)}/></label><button disabled={busy||!reason.trim()} onClick={()=>void enviar('finalizar')}>Declarar esta versión final</button></>}
    {data.permiteSucesion&&!pending&&<><label>Original anterior autorizado<select disabled={busy} value={previous} onChange={e=>void elegir(e.target.value)}><option value="">Seleccionar antecedente</option>{documentos.filter(d=>d.id!==documento.id&&d.consultable===true).map(d=><option key={d.id} value={d.id}>{d.nombre}</option>)}</select></label>{candidate&&!candidate.acreditada&&<p>El antecedente requiere declarar sus metadatos antes de enlazar versiones.</p>}<button disabled={busy||!reason.trim()||!candidate?.acreditada} onClick={()=>void enviar('sucesion')}>Enlazar como sucesora conservando originales</button></>}
    {pending&&<><p role="status">Confirmación pendiente. Se reintenta exactamente la misma declaración y clave.</p><button disabled={busy} onClick={()=>void enviar(attempt.current!.op)}>Reintentar declaración documental</button></>}
   </>}
   {!!data.historial.length&&<details><summary>Historia de declaraciones documentales</summary><p>Últimos 100 cambios. No acredita lectura humana ni firma externa.</p><ol>{data.historial.map(h=><li key={h.secuencia}>{h.tipo} · {h.autor} · {date(h.fecha)} · {h.fundamento}{typeof h.datos.title_text==='string'&&<> · {h.datos.title_text}</>}</li>)}</ol></details>}
  </>}
  <button disabled={busy||pending} onClick={()=>F(v=>v+1)}>Actualizar versión documental</button>
 </>}</details>;
}
