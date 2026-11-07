import {useEffect,useState} from 'react';
import * as api from './api';
import type {Catalog} from './api';
const fecha=(v:string|null)=>v?new Date(v).toLocaleString('es-CL',{timeZone:'America/Santiago'}):'Sin fecha acreditada';
export function SupervisionClasificacion({id,version,catalog}:{id?:string;version:number;catalog:Catalog}){
 const[bandeja,B]=useState<api.BandejaSupervision|null>(null),[detalle,D]=useState<api.TomaClasificacion|null>(null),[error,E]=useState(''),[cursors,C]=useState([0]),[refresh,F]=useState(0);const cursor=cursors[cursors.length-1];
 useEffect(()=>{let live=true;B(null);D(null);E('');
  const load=id?api.detalleSupervisionClasificacion(id).then(v=>{if(live)D(v);}):api.supervisionClasificacion(cursor,20).then(v=>{if(live)B(v);});
  void load.catch(e=>{if(live){B(null);D(null);E(e.message);}});return()=>{live=false;};
 },[id,version,cursor,refresh]);
 return <section className="card" aria-label="Supervisión de clasificación"><h1>Supervisión de clasificación</h1>
  <p>Consulta de metadatos conforme a tu concesión vigente. Esta vista no concede acceso al contenido de Comunicaciones, documentos o expedientes, ni permite gestionar tomas.</p>
  {error&&<p role="alert">{error}</p>}{!error&&!bandeja&&!detalle&&<p role="status">Comprobando el alcance autorizado…</p>}
  <button onClick={()=>{C([0]);F(n=>n+1);}}>Actualizar supervisión</button>
  {id&&<p><a href="#supervision-clasificacion">Volver a la bandeja de supervisión</a></p>}
  {bandeja&&<><p>Tomadas actualmente en mis ámbitos: {bandeja.total}</p>{!bandeja.items.length?<p>No hay tomas visibles en esta página.</p>:<table><caption>Comunicaciones tomadas dentro de tus ámbitos</caption><thead><tr><th>Referencia</th><th>Cuenta actuante</th><th>Captura</th><th>Toma</th><th>Consulta</th></tr></thead><tbody>{bandeja.items.map(v=><tr key={v.comunicacion}><td>{v.folio}</td><td><span title={v.titular}>{catalog.operadores.find(a=>a.id===v.titular)?.nombre??'Cuenta individual conservada'}</span></td><td>{v.sede?(catalog.sedes.find(s=>s.id===v.sede)?.nombre??v.sede):'Web central'}</td><td>{fecha(v.tomadaEn)}</td><td><a href={'#supervision-clasificacion/'+v.comunicacion}>Consultar historial de clasificación</a></td></tr>)}</tbody></table>}
   <div className="buttons"><button disabled={cursors.length===1} onClick={()=>C(v=>v.slice(0,-1))}>Página anterior</button><button disabled={bandeja.items.length<20||bandeja.cursor<=cursor} onClick={()=>C(v=>[...v,bandeja.cursor])}>Página siguiente</button></div>
  </>}
  {detalle&&<><h2>Historial de clasificación</h2><p>Estado: {detalle.estado.replaceAll('_',' ')}</p><p>Último cambio: {fecha(detalle.tomadaEn)}</p>{detalle.titular&&<p>Cuenta del último cambio: <span title={detalle.titular}>{catalog.operadores.find(a=>a.id===detalle.titular)?.nombre??'Cuenta individual conservada'}</span></p>}<p>Solo metadatos. Consultar no marca avisos leídos ni acusa asignaciones.</p><ol>{detalle.historial.map(h=><li key={h.revision}>{h.accion.replaceAll('_',' ')} · {fecha(h.fecha)}</li>)}</ol></>}
 </section>;
}
