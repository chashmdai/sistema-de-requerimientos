import {render, screen, fireEvent, waitFor} from '@testing-library/react';
import {beforeEach, expect, test, vi} from 'vitest';
import {Comunicacion} from './Comunicacion';
import * as api from './api';
vi.mock('./api', async original => ({...await original<typeof import('./api')>(), detail: vi.fn()}));
const regional:api.Actor={id:'regional',nombre:'Coordinación sintética',rol:'JEFATURA',unidad:'UPR',sede:'A',accesos:[{rol:'COORDINADOR_REGIONAL',alcance:'SEDE',unidad:null,sede:'A'},{rol:'COORDINADOR_REGIONAL',alcance:'SEDE',unidad:null,sede:'B'}]};
const catalog:api.Catalog={unidades:[],sedes:[],presencias:[{unidad:'UPR',sede:'A'},{unidad:'UPD',sede:'A'},{unidad:'UEP',sede:'B'}],operadores:[regional]};
const detail:api.Detail={comunicacion:{id:'com',secuencia:1,folio:'COM-sintetica',registradaEn:'2026-11-02T12:00:00Z',canal:'REGISTRO_FUNCIONARIO',estado:'ABIERTA',revision:0,requerimientoId:null,estadoRequerimiento:null,casosAbiertos:0,destinosPendientes:0,sedeOrigen:'A'},contenido:{texto:'Constancia sintética',tipoIdentidad:'MANUAL',identidad:null,declaracionNna:null,recibidaEnDeclarada:'2026-11-02T12:00:00Z',canalDeclarado:'TELÉFONO',registradoPor:'regional'},destinos:[],casos:[],historial:[],vinculos:[],respuestaDirecta:null,decisionAutor:null,decisionFecha:null,derivacionHabilitada:false,reaperturaHabilitada:false,pendientes:[]};
beforeEach(()=>{vi.clearAllMocks();HTMLDialogElement.prototype.showModal=function(){this.setAttribute('open','');};HTMLDialogElement.prototype.close=function(){this.removeAttribute('open');};vi.mocked(api.detail).mockResolvedValue(detail);});
test('REG-DIS-01 limita selección a sede origen aunque tenga coordinación en otras sedes',async()=>{
 const run=vi.fn().mockResolvedValue(undefined);
 render(<Comunicacion id="com" actor={regional} catalog={catalog} run={run} busy={false} version={0} navigate={vi.fn()}/>);
 fireEvent.click(await screen.findByText('Distribuir Comunicación'));
 expect(screen.queryByRole('option',{name:'UEP · B'})).toBeNull();expect(screen.getByRole('option',{name:'UPD · A'})).toBeTruthy();
 fireEvent.change(screen.getByLabelText('Unidad y sede'),{target:{value:'UPR|A'}});fireEvent.click(screen.getByText('Añadir destino'));fireEvent.click(screen.getByText('Confirmar distribución'));
 await waitFor(()=>expect(run).toHaveBeenCalledWith('/nucleo/comunicaciones/com/distribuir',{revision:0,destinos:[{unidad:'UPR',sede:'A',evaluador:'regional'}]}));
});
test('designación central independiente permite destinos institucionales',async()=>{
 const actor={...regional,accesos:[...regional.accesos!,{rol:'DISTRIBUCION',alcance:'INSTITUCION',unidad:null,sede:null}]};
 render(<Comunicacion id="com" actor={actor} catalog={catalog} run={vi.fn()} busy={false} version={0} navigate={vi.fn()}/>);
 fireEvent.click(await screen.findByText('Distribuir Comunicación'));expect(screen.getByRole('option',{name:'UEP · B'})).toBeTruthy();
});
