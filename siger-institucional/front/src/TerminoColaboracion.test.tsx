import {render,screen,fireEvent,waitFor} from '@testing-library/react';
import {expect,test,vi} from 'vitest';
import {TerminosColaboracion} from './TerminoColaboracion';
import type {PropuestaTermino} from './TerminoColaboracion';

const row:PropuestaTermino={id:'peticion-a',case_id:'caso-b',revision:0,state:'PENDIENTE',reason_text:'La principal propone término',decision_text:null,requested_by:'jefe-a',created_ms:1,decided_by:null,decided_ms:null,puedeRetirar:false,puedeDecidir:true,estadoCaso:'EN_GESTION',historial:[]};
test('destino decide la propuesta sin enviar cierre ni reasignar el Caso',async()=>{
 const run=vi.fn().mockResolvedValue(undefined);render(<TerminosColaboracion registros={[row]} run={run} busy={false}/>);
 const accept=screen.getByText('Aceptar propuesta de término');expect((accept as HTMLButtonElement).disabled).toBe(true);
 fireEvent.change(screen.getByLabelText('Fundamento sobre la continuidad de la intervención'),{target:{value:'La unidad acepta evaluar el cierre'}});fireEvent.click(accept);
 await waitFor(()=>expect(run).toHaveBeenCalledWith('/nucleo/institucional/colaboraciones/terminos/peticion-a/decidir',{revision:0,decision:'ACEPTADA',fundamento:'La unidad acepta evaluar el cierre'}));
 expect(run).toHaveBeenCalledTimes(1);expect(screen.queryByText('Retirar propuesta de término')).toBeNull();
});
test('origen solo retira mientras la petición esté pendiente',async()=>{
 const run=vi.fn().mockResolvedValue(undefined);render(<TerminosColaboracion registros={[{...row,puedeDecidir:false,puedeRetirar:true}]} run={run} busy={false}/>);
 expect(screen.queryByText('Aceptar propuesta de término')).toBeNull();fireEvent.change(screen.getByLabelText('Fundamento sobre la continuidad de la intervención'),{target:{value:'Retirada fundada'}});fireEvent.click(screen.getByText('Retirar propuesta de término'));
 await waitFor(()=>expect(run).toHaveBeenCalledWith('/nucleo/institucional/colaboraciones/terminos/peticion-a/decidir',{revision:0,decision:'RETIRADA',fundamento:'Retirada fundada'}));
});
test('integrante sin jefatura consulta sin controles y aceptación mantiene cierre separado',()=>{
 render(<TerminosColaboracion registros={[{...row,state:'ACEPTADA',puedeRetirar:false,puedeDecidir:false,decision_text:'Acepta destino',decided_by:'jefe-b',decided_ms:2,historial:[{revision:1,action_code:'COLABORACION_TERMINO_ACEPTADA',actor_id:'jefe-b',at_ms:2,reason_text:'Acepta destino'}]}]} run={vi.fn()} busy={false}/>);
 expect(screen.getByText(/Propuesta aceptada. El Caso sigue abierto/)).toBeTruthy();expect(screen.queryByText('Aceptar propuesta de término')).toBeNull();expect(screen.getByText('Historia de esta propuesta')).toBeTruthy();
});
test('error conserva fundamento sin aparentar una decisión confirmada',async()=>{
 const run=vi.fn().mockRejectedValue(new Error('Cambió el estado'));render(<TerminosColaboracion registros={[row]} run={run} busy={false}/>);
 fireEvent.change(screen.getByLabelText('Fundamento sobre la continuidad de la intervención'),{target:{value:'Conservar esta razón'}});fireEvent.click(screen.getByText('Rechazar propuesta y continuar intervención'));
 expect((await screen.findByRole('alert')).textContent).toBe('Cambió el estado');expect((screen.getByLabelText('Fundamento sobre la continuidad de la intervención') as HTMLTextAreaElement).value).toBe('Conservar esta razón');expect(screen.getByText('Propuesta de término · PENDIENTE')).toBeTruthy();
});
