export type EstadoTerritorial={estado:string;sedeAcreditada:string|null};
export type DecisionTerritorial={condicion:string;fundamento:string;referencias:string};
export const decisionVacia:DecisionTerritorial={condicion:'',fundamento:'',referencias:''};
export function decisionCompleta(v:DecisionTerritorial,regional:boolean,orientacion?:EstadoTerritorial|null){
 if(!v.condicion)return false;
 if(!regional)return true;
 if(v.condicion==='UTILIZABLE')return orientacion?.estado==='COBERTURA_ACREDITADA';
 return !!v.fundamento.trim()&&!!v.referencias.trim();
}
export function FormularioTerritorial({region,regional,orientacion,value,changed}:{region:string|null;regional:boolean;orientacion?:EstadoTerritorial|null;value:DecisionTerritorial;changed:(v:DecisionTerritorial)=>void}){
 const labels:Record<string,string>={COBERTURA_ACREDITADA:'Cobertura institucional vigente acreditada',COBERTURA_NO_ACREDITADA:'No hay cobertura vigente acreditada para este domicilio',SEDE_NO_CORRESPONDE:'La sede propuesta no corresponde a la cobertura acreditada',REQUIERE_DECISION_FUNDADA:'Se necesita una decisión territorial fundada',COMPETENCIA_CENTRAL:'Competencia central: el domicilio no modifica por sí solo el principal'};
 return <fieldset><legend>Determinación territorial</legend>
  <p>El domicilio corresponde al requiriente. El domicilio del NNA, los hechos y la sede de captura se conservan como antecedentes distintos.</p>
  {orientacion&&<p role="status">{labels[orientacion.estado]??'Cobertura pendiente de comprobación'}{orientacion.sedeAcreditada&&` · ${orientacion.sedeAcreditada}`}</p>}
  <label>Condición del domicilio del requiriente<select required value={value.condicion} onChange={e=>changed({...value,condicion:e.target.value})}>
   <option value="">Evaluar antecedente disponible</option><option value="UTILIZABLE" disabled={!region}>Utilizable para competencia territorial</option><option value="AUSENTE" disabled={!!region}>Ausente</option><option value="CONTRADICTORIO">Contradictorio</option><option value="EXTRANJERO">Extranjero</option><option value="INUTILIZABLE">Inutilizable por otro motivo acreditado</option>
  </select></label>
  {regional&&value.condicion&&value.condicion!=='UTILIZABLE'&&<><label>Fundamento de la decisión territorial<textarea required maxLength={2000} value={value.fundamento} onChange={e=>changed({...value,fundamento:e.target.value})}/></label><label>Referencias territoriales utilizadas<textarea required maxLength={2000} value={value.referencias} onChange={e=>changed({...value,referencias:e.target.value})}/></label><p>La decisión conserva el domicilio declarado. Si faltan antecedentes suficientes, mantén la Solicitud pendiente.</p></>}
 </fieldset>;
}
