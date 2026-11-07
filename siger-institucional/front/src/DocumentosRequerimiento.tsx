import {AsociacionDocumento} from './AsociacionDocumento';
import {VersionDocumento} from './VersionDocumento';
import {useEffect,useRef,useState} from 'react';
import * as api from './api';
import type {EntradaGestion,VistaBitacora} from './BitacoraGestion';
import {VisorDocumento} from './VisorDocumento';
import {date} from './ui';

export function DocumentosRequerimiento({requerimiento,version}:{requerimiento:string;version:number}){
 const [data,D]=useState<api.ListaDocumentosReq|null>(null),[error,E]=useState(''),[refresh,F]=useState(0);
 const [busy,B]=useState(false),[pending,P]=useState(false),[file,V]=useState<File|null>(null),[seq,Q]=useState(''),[preview,R]=useState<api.DocumentoReq|null>(null);
 const [entries,G]=useState<EntradaGestion[]>([]),[page,N]=useState(0),[total,T]=useState(0);
 const generation=useRef(0),input=useRef<HTMLInputElement>(null);
 const attempt=useRef<{req:string;file:File;seq:number|null;key:string}|null>(null);
 useEffect(()=>{
  const abort=new AbortController();generation.current++;D(null);R(null);E('');G([]);N(0);T(0);V(null);Q('');B(false);P(false);attempt.current=null;if(input.current)input.current.value='';
  void api.documentosReq(requerimiento,abort.signal).then(v=>{if(!abort.signal.aborted)D(v);}).catch(e=>{if(!abort.signal.aborted)E(e.message);});
  return()=>{abort.abort();generation.current++;};
 },[requerimiento,version,refresh]);
 useEffect(()=>{
  if(!data||!data.editable&&!data.documentos.some(d=>d.restringible))return;let live=true;G([]);
  void api.gestionGet<VistaBitacora>(`/nucleo/institucional/requerimientos/${encodeURIComponent(requerimiento)}/bitacora?pagina=${page}&tamanio=25`).then(v=>{if(live){G(v.entradas);T(v.total);}}).catch(e=>{if(live){D(null);R(null);E(e.message);}});
  return()=>{live=false;};
 },[requerimiento,data,page]);
 async function upload(){
  if(!data?.editable||busy||(!file&&!attempt.current))return;
  const current=generation.current;B(true);E('');
  if(!attempt.current&&file)attempt.current={req:requerimiento,file,seq:seq?Number(seq):null,key:crypto.randomUUID()};
  try{const a=attempt.current!;await api.cargarDocumentoReq(a.req,a.file,a.seq,a.key);if(current===generation.current){attempt.current=null;P(false);F(v=>v+1);}}
  catch(e){if(current===generation.current){if(e instanceof api.ApiError&&e.status>=400&&e.status<500){attempt.current=null;P(false);if(e.status===403||e.status===404){D(null);R(null);}}else P(true);E((e as Error).message);}}
  finally{if(current===generation.current)B(false);}
 }
 const changed=()=>F(v=>v+1);
 return <section className="card" aria-label="Documentos del Requerimiento"><h2>Documentos del Requerimiento</h2>
  <p>Originales de la atención sin Caso. Una referencia al mismo REQ no concede acceso a un Caso ni a sus documentos.</p>
  {error&&<p role="alert">{error}</p>}
  {!data&&!error&&<p role="status">Consultando documentos autorizados…</p>}
  {data?.editable&&<form onSubmit={e=>{e.preventDefault();void upload();}}>
   <fieldset disabled={busy||pending}><legend>Incorporar antecedente de gestión</legend>
    <label>Archivo del Requerimiento (máximo 25 MiB)<input ref={input} type="file" accept=".pdf,.png,.jpg,.jpeg,.webp,.txt,.docx,.xlsx,.pptx,.odt,.mp3,.m4a,.wav,.ogg,.mp4,.mov,.webm" onChange={e=>V(e.target.files?.[0]??null)}/></label>
    <label>Gestión asociada del REQ<select value={seq} onChange={e=>Q(e.target.value)}><option value="">Antecedente general del REQ</option>{entries.map(e=><option key={e.id} value={e.id.slice(4)}>{e.id} · {e.descripcion.slice(0,100)}</option>)}</select></label>
    <button type="button" disabled={page===0} onClick={()=>N(v=>v-1)}>Gestiones anteriores del REQ</button><button type="button" disabled={(page+1)*25>=total} onClick={()=>N(v=>v+1)}>Gestiones siguientes del REQ</button>
   </fieldset>
   {pending&&<p role="status">Resultado aún no confirmado. El reintento conserva el mismo archivo, referencia y clave.</p>}
   <button disabled={busy||(!file&&!pending)}>{busy?'Validando original…':pending?'Reintentar el mismo documento':'Conservar documento del REQ'}</button>
   <p>El archivo pasa por antivirus e inspección. No se registra una actuación ni se abre un Caso por cargarlo.</p>
  </form>}
  {data&&!data.editable&&<p>La consulta conserva los antecedentes anteriores. Las nuevas cargas requieren atención activa sin Casos.</p>}
  {data&&!data.documentos.length&&<p>Sin documentos autorizados en este Requerimiento.</p>}
  {data&&!data.editable&&data.documentos.some(d=>d.restringible)&&<p><button disabled={page===0} onClick={()=>N(v=>v-1)}>Gestiones anteriores del REQ</button><button disabled={(page+1)*25>=total} onClick={()=>N(v=>v+1)}>Gestiones siguientes del REQ</button></p>}
  {data?.documentos.map(d=><article className="card" key={`${d.id}:${d.revision}`}><h3>{d.nombre}</h3>{d.asociacionRetirada&&<p role="status">Asociación retirada. El original se conserva y no está disponible para consulta.</p>}{d.cuarentena&&<p role="status">Archivo aislado técnicamente. Se conserva el original; consulta y descarga temporalmente cerradas.</p>}<p>{d.autor} · {date(d.fecha)} · {d.tamanio.toLocaleString('es-CL')} bytes · {d.proteccion}</p>
   <ul>{d.contextos.map(c=><li key={c.gestionSeq??'REQ'}>{c.gestionSeq?`Gestión REQ:${c.gestionSeq}`:'Antecedente general del REQ'}</li>)}</ul>
   <details><summary>Integridad del original</summary><code>{d.sha256}</code></details>
   {!d.asociacionRetirada&&!d.cuarentena&&d.consultable===true&&d.representacion==='PAGINAS'&&<button onClick={()=>R(d)}>Previsualizar {d.nombre}</button>}
   {!d.asociacionRetirada&&!d.cuarentena&&d.consultable===true&&d.descargable===true&&d.representacion==='AUDIO'&&<audio controls preload="none" src={`${api.baseDocumentosReq(requerimiento)}/${d.id}/representacion`}/>}
   {!d.asociacionRetirada&&!d.cuarentena&&d.consultable===true&&d.descargable===true&&d.representacion==='VIDEO'&&<video controls playsInline preload="none" src={`${api.baseDocumentosReq(requerimiento)}/${d.id}/representacion`}/>}
   {!d.asociacionRetirada&&!d.cuarentena&&d.descargable===true&&<a href={`${api.baseDocumentosReq(requerimiento)}/${d.id}/original`}>Descargar {d.nombre}</a>}
   {!d.asociacionRetirada&&!d.cuarentena&&d.consultable===true&&<VersionDocumento base={`${api.baseDocumentosReq(requerimiento)}/${d.id}`} documento={d} documentos={data.documentos} version={`${version}:${refresh}:${d.revision}`} denegado={()=>{D(null);R(null);}}/>}
   {!d.asociacionRetirada&&!d.cuarentena&&d.consultable===true&&<Historia req={requerimiento} doc={d.id} denegado={()=>{D(null);R(null);}}/>}
   {d.restringible&&<AsociacionDocumento base={`${api.baseDocumentosReq(requerimiento)}/${d.id}`} nombre={d.nombre} raiz="REQ" opciones={entries.map(e=>({id:e.id.slice(4),nombre:`Gestión · ${e.descripcion.slice(0,100)}`}))} version={`${version}:${refresh}:${d.revision}`} changed={()=>{R(null);changed();}} denegado={()=>{D(null);R(null);}}/>}{(d.administrable||d.restringible)&&<Proteger req={requerimiento} doc={d} changed={changed} denegado={()=>{D(null);R(null);}}/>}

  </article>)}
  {preview&&<VisorDocumento key={preview.id} nombre={preview.nombre} descargable={preview.descargable} base={`${api.baseDocumentosReq(requerimiento)}/${preview.id}`} cerrar={()=>R(null)}/>}
  <button disabled={busy||pending} onClick={changed}>Actualizar documentos autorizados del REQ</button>
 </section>;
}
function Historia({req,doc,denegado}:{req:string;doc:string;denegado:()=>void}){
 const [open,O]=useState(false),[items,I]=useState<Awaited<ReturnType<typeof api.historialDocumentoReq>>>([]),[error,E]=useState('');
 const deny=useRef(denegado);deny.current=denegado;
 useEffect(()=>{if(!open)return;const abort=new AbortController();I([]);void api.historialDocumentoReq(req,doc,abort.signal).then(v=>{if(!abort.signal.aborted)I(v);}).catch(e=>{if(!abort.signal.aborted){I([]);E(e.message);if(e instanceof api.ApiError&&(e.status===403||e.status===404))deny.current();}});return()=>abort.abort();},[req,doc,open]);
 return <details onToggle={e=>O(e.currentTarget.open)}><summary>Historia del documento REQ</summary><p>Últimos 100 movimientos. La solicitud de bytes no acredita lectura o recepción humana.</p>{error&&<p role="alert">{error}</p>}<ol>{items.map(i=><li key={i.secuencia}>{i.tipo} · {i.autor} · {date(i.fecha)} · {i.fundamento}</li>)}</ol></details>;
}
function Proteger({req,doc,changed,denegado}:{req:string;doc:api.DocumentoReq;changed:()=>void;denegado:()=>void}){
 const [value,V]=useState(doc.proteccion),[reason,R]=useState(''),[busy,B]=useState(false),[error,E]=useState('');const attempt=useRef<{body:object;key:string}|null>(null);
 async function send(){if(busy||!reason.trim())return;B(true);try{attempt.current??={body:{revision:doc.revision,clasificacion:doc.administrable?value:'BLOQUEADO',fundamento:reason},key:crypto.randomUUID()};await api.command(`${api.baseDocumentosReq(req)}/${doc.id}/proteccion`,attempt.current.body,attempt.current.key);changed();}catch(e){E((e as Error).message);if(e instanceof api.ApiError&&e.status<500){attempt.current=null;if(e.status===403||e.status===404)denegado();}}finally{B(false);}}
 return <form onSubmit={e=>{e.preventDefault();void send();}}><fieldset disabled={busy||!!attempt.current}><legend>Protección de {doc.nombre}</legend><label>Clasificación documental<select value={doc.administrable?value:'BLOQUEADO'} onChange={e=>V(e.target.value)}>{doc.administrable&&<><option value="SIN_CLASIFICAR" disabled>Sin clasificar</option><option value="ORDINARIO">Ordinario</option><option value="FIR">Ficha Interna Reservada</option></>}<option value="BLOQUEADO">Bloqueado</option></select></label><label>Fundamento documental<textarea value={reason} onChange={e=>R(e.target.value)} required maxLength={2000}/></label></fieldset>{error&&<p role="alert">{error}</p>}<button disabled={busy||!reason.trim()||doc.administrable&&value==='SIN_CLASIFICAR'}>Confirmar protección documental</button></form>;
}
