import {cleanup,fireEvent,render,screen,waitFor} from '@testing-library/react';
import {afterEach,beforeEach,expect,test,vi} from 'vitest';
import {BitacoraGestion} from './BitacoraGestion';
import type {VistaBitacora} from './BitacoraGestion';
import * as api from './api';
vi.mock('./api',async()=>({...await vi.importActual<typeof api>('./api'),gestionGet:vi.fn(),command:vi.fn()}));
const entry={id:'act-original',version:'1.0',descripcion:'Contacto efectivamente realizado',realizadaEn:'2026-11-05T12:00:00Z',registradaEn:'2026-11-06T12:00:00Z',registrador:'profesional',ejecutor:'profesional',rectifica:null,rectificadaPor:null,motivo:null};
const view:VistaBitacora={contexto:'CASO',recurso:'cas',revision:4,puedeRegistrar:true,entradas:[entry],pagina:0,tamanio:25,total:1};
beforeEach(()=>{HTMLDialogElement.prototype.showModal=function(){this.setAttribute("open","");};HTMLDialogElement.prototype.close=function(){this.removeAttribute("open");};vi.mocked(api.gestionGet).mockResolvedValue(view);vi.mocked(api.command).mockResolvedValue({id:'cas',revision:5});});
afterEach(()=>{cleanup();vi.resetAllMocks();});
async function fill(){fireEvent.click(await screen.findByText('Registrar gestión realizada'));fireEvent.change(screen.getByLabelText('Fecha y hora efectivas de la gestión'),{target:{value:'2026-11-05T09:30'}});fireEvent.change(screen.getByLabelText('Descripción de la gestión realizada'),{target:{value:'Gestión ingresada por quien la realizó'}});}

test('registra fecha efectiva expresa con contexto y revisión del backend',async()=>{
 const changed=vi.fn();render(<BitacoraGestion contexto="CASO" recurso="cas" version={1} changed={changed}/>);await fill();fireEvent.click(screen.getByText('Confirmar actuación realizada'));
 await waitFor(()=>expect(api.command).toHaveBeenCalledOnce());expect(api.command).toHaveBeenCalledWith('/nucleo/casos/cas/bitacora',{version:'1.0',revision:4,realizadaEn:new Date('2026-11-05T09:30').toISOString(),descripcion:'Gestión ingresada por quien la realizó',rectifica:null,motivo:null},expect.any(String));await waitFor(()=>expect(changed).toHaveBeenCalledOnce());
});
test('rectifica mediante otra constancia con motivo y sin alterar el texto mostrado',async()=>{
 render(<BitacoraGestion contexto="CASO" recurso="cas" version={1}/>);fireEvent.click(await screen.findByText('Rectificar constancia act-original'));
 fireEvent.change(screen.getByLabelText('Descripción de la gestión realizada'),{target:{value:'Descripción corregida'}});fireEvent.change(screen.getByLabelText('Motivo de rectificación'),{target:{value:'Antecedente posterior que corrige fecha y relato'}});fireEvent.click(screen.getByText('Registrar rectificación'));
 await waitFor(()=>expect(api.command).toHaveBeenCalledOnce());expect(vi.mocked(api.command).mock.calls[0][1]).toMatchObject({rectifica:'act-original',motivo:'Antecedente posterior que corrige fecha y relato',descripcion:'Descripción corregida'});expect(screen.getByText(entry.descripcion)).toBeTruthy();
});
test('histórico sin fecha efectiva o ejecutor no se completa por inferencia',async()=>{
 vi.mocked(api.gestionGet).mockResolvedValue({...view,entradas:[{...entry,version:null,realizadaEn:null,ejecutor:null}]});render(<BitacoraGestion contexto="CASO" recurso="cas" version={1}/>);
 expect(await screen.findByText('Fecha efectiva no acreditada en el registro histórico')).toBeTruthy();expect(screen.getByText('No acreditado separadamente en el registro histórico')).toBeTruthy();fireEvent.click(screen.getByText('Rectificar constancia act-original'));expect((screen.getByLabelText('Fecha y hora efectivas de la gestión') as HTMLInputElement).value).toBe('');expect(api.command).not.toHaveBeenCalled();
});
test('consulta por materia o cierre no presenta facultades de escritura',async()=>{
 vi.mocked(api.gestionGet).mockResolvedValue({...view,puedeRegistrar:false});render(<BitacoraGestion contexto="CASO" recurso="cas" version={1} abrir/>);await screen.findByText(entry.descripcion);expect(screen.queryByText('Registrar gestión realizada')).toBeNull();expect(screen.queryByText('Rectificar constancia act-original')).toBeNull();expect(screen.queryByLabelText('Descripción de la gestión realizada')).toBeNull();
});
test('usa la ruta REQ sin Caso con el mismo contrato y sin crear un expediente',async()=>{
 vi.mocked(api.gestionGet).mockResolvedValue({...view,contexto:'REQ',recurso:'req',entradas:[],total:0});render(<BitacoraGestion contexto="REQ" recurso="req" version={1}/>);await fill();fireEvent.click(screen.getByText('Confirmar actuación realizada'));await waitFor(()=>expect(api.command).toHaveBeenCalledOnce());expect(vi.mocked(api.command).mock.calls[0][0]).toBe('/nucleo/institucional/requerimientos/req/bitacora');
});
test('revocación en la misma vista elimina registros y controles anteriores',async()=>{
 const ui=render(<BitacoraGestion contexto="CASO" recurso="cas" version={1}/>);await screen.findByText(entry.descripcion);vi.mocked(api.gestionGet).mockRejectedValue(new api.ApiError(404));ui.rerender(<BitacoraGestion contexto="CASO" recurso="cas" version={2}/>);await screen.findByRole('alert');expect(screen.queryByText(entry.descripcion)).toBeNull();expect(screen.queryByText('Registrar gestión realizada')).toBeNull();
});
test('respuesta incierta conserva clave y payload aunque cambie la revisión',async()=>{
 vi.mocked(api.command).mockRejectedValueOnce(new Error('Conexión interrumpida')).mockResolvedValue({id:'cas',revision:5});const ui=render(<BitacoraGestion contexto="CASO" recurso="cas" version={1}/>);await fill();fireEvent.click(screen.getByText('Confirmar actuación realizada'));await screen.findByText('Reintentar confirmación');const first=vi.mocked(api.command).mock.calls[0];
 expect(screen.getByLabelText('Descripción de la gestión realizada').matches(':disabled')).toBe(true);vi.mocked(api.gestionGet).mockResolvedValue({...view,revision:5});ui.rerender(<BitacoraGestion contexto="CASO" recurso="cas" version={2}/>);fireEvent.click(await screen.findByText('Reintentar confirmación'));await waitFor(()=>expect(api.command).toHaveBeenCalledTimes(2));expect(vi.mocked(api.command).mock.calls[1]).toEqual(first);
});
test('conflicto definitivo mantiene borrador y requiere actualizar la revisión',async()=>{
 vi.mocked(api.command).mockRejectedValueOnce(new api.ApiError(409,'VERSION_OBSOLETA')).mockResolvedValue({id:'cas',revision:7});render(<BitacoraGestion contexto="CASO" recurso="cas" version={1}/>);await fill();fireEvent.click(screen.getByText('Confirmar actuación realizada'));await screen.findByRole('alert');const first=vi.mocked(api.command).mock.calls[0];
 expect((screen.getByLabelText('Descripción de la gestión realizada') as HTMLTextAreaElement).value).toBe('Gestión ingresada por quien la realizó');vi.mocked(api.gestionGet).mockResolvedValue({...view,revision:6});fireEvent.click(screen.getByText('Actualizar bitácora autorizada'));await screen.findByText('Confirmar actuación realizada');fireEvent.click(screen.getByText('Confirmar actuación realizada'));await waitFor(()=>expect(api.command).toHaveBeenCalledTimes(2));expect(vi.mocked(api.command).mock.calls[1][1]).toMatchObject({revision:6});expect(vi.mocked(api.command).mock.calls[1][2]).not.toBe(first[2]);
});
test('vínculo consulta la constancia dentro del mismo recurso y una denegación retira la vista',async()=>{
 vi.mocked(api.gestionGet).mockResolvedValueOnce({...view,entradas:[{...entry,rectificadaPor:'act-correccion'}]}).mockResolvedValueOnce({...entry,id:'act-correccion',descripcion:'Texto de rectificación',rectifica:entry.id});
 render(<BitacoraGestion contexto="CASO" recurso="cas" version={1}/>);fireEvent.click(await screen.findByText('Consultar rectificación vinculada'));expect(await screen.findByText('Texto de rectificación')).toBeTruthy();expect(api.gestionGet).toHaveBeenLastCalledWith('/nucleo/casos/cas/bitacora/constancias/act-correccion');fireEvent.click(screen.getByLabelText('Cerrar panel'));
 vi.mocked(api.gestionGet).mockRejectedValue(new api.ApiError(404));fireEvent.click(screen.getByText('Consultar rectificación vinculada'));await screen.findByRole('alert');expect(screen.queryByText(entry.descripcion)).toBeNull();expect(screen.queryByText('Texto de rectificación')).toBeNull();
});
test.each(['2026-11-05T12:13:14.123Z','2026-11-05T12:13:14.000Z','2026-11-05T12:13:00.000Z'])('rectificar solo texto conserva el instante %s aunque el navegador normalice ceros',async(exact)=>{
 vi.mocked(api.gestionGet).mockResolvedValue({...view,entradas:[{...entry,realizadaEn:exact}]});render(<BitacoraGestion contexto="CASO" recurso="cas" version={1}/>);fireEvent.click(await screen.findByText('Rectificar constancia act-original'));fireEvent.change(screen.getByLabelText('Motivo de rectificación'),{target:{value:'Solo corregir descripción, conservando el instante'}});fireEvent.click(screen.getByText('Registrar rectificación'));await waitFor(()=>expect(api.command).toHaveBeenCalledOnce());expect(vi.mocked(api.command).mock.calls[0][1]).toMatchObject({realizadaEn:exact});
});
