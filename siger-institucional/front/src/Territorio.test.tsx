import {render,screen,fireEvent,waitFor} from '@testing-library/react';
import {beforeEach,expect,test,vi} from 'vitest';
import {AsumirIngresoPropioU2,SolU2} from './NucleoU2';
import * as api from './api';
vi.mock('./api',async original=>({...await original<typeof import('./api')>(),u2Get:vi.fn(),detail:vi.fn()}));
const head:api.Actor={id:'head',nombre:'Jefatura sintética',rol:'JEFATURA',unidad:'UPR',sede:'A',accesos:[{rol:'COORDINADOR_REGIONAL',alcance:'SEDE',unidad:null,sede:'A'}]};
const catalog:api.Catalog={unidades:[],sedes:[],presencias:[{unidad:'UPR',sede:'A'}],operadores:[head],regiones:[{id:'R1',nombre:'Región sintética'}]};
const sol={id:'sol',communication_id:'com',state:'PENDIENTE',revision:2,unit_code:'UPR',site_id:'A',area_id:null,domicile_region:'R1',classification_text:'Propuesta sintética',puedeDecidir:true,territorio:{estado:'COBERTURA_ACREDITADA',sedeAcreditada:'A'}};
function props(){return {actor:head,catalog,run:vi.fn().mockResolvedValue(undefined),busy:false,version:0,navigate:vi.fn()};}
beforeEach(()=>{vi.clearAllMocks();vi.mocked(api.u2Get).mockResolvedValue(sol);});
async function fillSol(){await screen.findByText('Propuesta sintética');fireEvent.change(screen.getByLabelText('Persona encargada'),{target:{value:'head'}});fireEvent.change(screen.getByLabelText('Evaluación y fundamento'),{target:{value:'Evaluación legítima'}});}

test('la cobertura acreditada se asume por el contrato explícito y no concede permiso de gestión por lectura',async()=>{
 const p=props();render(<SolU2 id="sol" {...p}/>);await fillSol();expect(screen.getByText('Asumir y crear REQ sin Caso').hasAttribute('disabled')).toBe(true);
 fireEvent.change(screen.getByLabelText('Condición del domicilio del requiriente'),{target:{value:'UTILIZABLE'}});fireEvent.click(screen.getByText('Asumir y crear REQ sin Caso'));
 await waitFor(()=>expect(p.run).toHaveBeenCalledWith('/nucleo/institucional/solicitudes/sol/asumir/territorio',{version:'1.0',revision:2,responsable:'head',fundamento:'Evaluación legítima',condicionDomicilio:'UTILIZABLE',fundamentoTerritorial:null,referenciasTerritoriales:null}));
 expect(screen.queryByRole('option',{name:'Ausente'})?.hasAttribute('disabled')).toBe(true);
});
test('sin cobertura se cierra la asunción utilizable y la excepción fundada conserva la región declarada',async()=>{
 vi.mocked(api.u2Get).mockResolvedValue({...sol,territorio:{estado:'COBERTURA_NO_ACREDITADA',sedeAcreditada:null}});const p=props();render(<SolU2 id="sol" {...p}/>);await fillSol();fireEvent.change(screen.getByLabelText('Condición del domicilio del requiriente'),{target:{value:'UTILIZABLE'}});
 expect(screen.getByText('Asumir y crear REQ sin Caso').hasAttribute('disabled')).toBe(true);expect(p.run).not.toHaveBeenCalled();
 fireEvent.change(screen.getByLabelText('Condición del domicilio del requiriente'),{target:{value:'CONTRADICTORIO'}});fireEvent.change(screen.getByLabelText('Fundamento de la decisión territorial'),{target:{value:'Antecedentes discordantes'}});expect(screen.getByText('Asumir y crear REQ sin Caso').hasAttribute('disabled')).toBe(true);
 fireEvent.change(screen.getByLabelText('Referencias territoriales utilizadas'),{target:{value:'Contacto sintético verificado'}});fireEvent.click(screen.getByText('Asumir y crear REQ sin Caso'));
 await waitFor(()=>expect(p.run).toHaveBeenCalledWith(expect.any(String),expect.objectContaining({condicionDomicilio:'CONTRADICTORIO',fundamentoTerritorial:'Antecedentes discordantes',referenciasTerritoriales:'Contacto sintético verificado'})));expect(screen.getByText(/Domicilio del requiriente registrado: R1/)).toBeTruthy();
});
test('redirección conserva domicilio y no simula transferencia de REQ',async()=>{
 const p=props();render(<SolU2 id="sol" {...p}/>);await fillSol();fireEvent.change(screen.getByLabelText('Unidad, sede y área'),{target:{value:'UPR|A|'}});fireEvent.click(screen.getByText('Redirigir la misma SOL'));
 await waitFor(()=>expect(p.run).toHaveBeenCalledWith('/nucleo/institucional/solicitudes/sol/redirigir',expect.objectContaining({domicilioRegion:'R1',clasificacion:'Evaluación legítima'})));
});
test('pérdida de autorización elimina el detalle y las decisiones al recargar',async()=>{
 const p=props();const view=render(<SolU2 id="sol" {...p}/>);await fillSol();vi.mocked(api.u2Get).mockRejectedValue(new Error('Fuera de alcance'));view.rerender(<SolU2 id="sol" {...p} version={1}/>);await screen.findByRole('alert');expect(screen.queryByText('Propuesta sintética')).toBeNull();expect(screen.queryByText('Asumir y crear REQ sin Caso')).toBeNull();
});
test('jefatura propia consulta cobertura antes de enviar y distingue captura de principal',async()=>{
 vi.mocked(api.detail).mockResolvedValue({comunicacion:{revision:0}} as api.Detail);vi.mocked(api.u2Get).mockImplementation(async path=>path.includes('region=R1')?{estado:'COBERTURA_ACREDITADA',sedeAcreditada:'A'}:{estado:'REQUIERE_DECISION_FUNDADA',sedeAcreditada:null});
 const p=props();render(<AsumirIngresoPropioU2 id="com" {...p}/>);fireEvent.change(screen.getByLabelText('Unidad, sede y área'),{target:{value:'UPR|A|'}});fireEvent.change(screen.getByLabelText('Región declarada de domicilio del requiriente'),{target:{value:'R1'}});await screen.findByText(/Cobertura institucional vigente acreditada/);
 fireEvent.change(screen.getByLabelText('Persona encargada'),{target:{value:'head'}});fireEvent.change(screen.getByLabelText('Evaluación de jefatura'),{target:{value:'Ingreso propio evaluado'}});fireEvent.change(screen.getByLabelText('Condición del domicilio del requiriente'),{target:{value:'UTILIZABLE'}});fireEvent.click(screen.getByText('Asumir ingreso y crear REQ'));
 await waitFor(()=>expect(p.run).toHaveBeenCalledWith('/nucleo/institucional/comunicaciones/com/asumir-propia/territorio',expect.objectContaining({version:'1.0',domicilioRegion:'R1',condicionDomicilio:'UTILIZABLE',destino:{unidad:'UPR',sede:'A',area:null,evaluador:'head'}})));
 expect(api.u2Get).toHaveBeenCalledWith('/comunicaciones/com/territorio?unidad=UPR&sede=A&region=R1');
});
