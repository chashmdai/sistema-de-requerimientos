import {render,screen,fireEvent,waitFor} from '@testing-library/react';
import {beforeEach,expect,test,vi} from 'vitest';
import {RectificacionesIngreso} from './RectificacionesIngreso';
import type {DatosIngreso,VistaRectificaciones} from './RectificacionesIngreso';
import * as api from './api';
vi.mock('./api',async original=>({...await original<typeof import('./api')>(),gestionGet:vi.fn(),command:vi.fn()}));
const original:DatosIngreso={texto:'Original de captura conservado',canalDeclarado:'TELÉFONO',recibidaEnDeclarada:'2026-11-02T12:00:30.120Z',regionAsunto:'R1'};
const data:VistaRectificaciones={version:'1.0',revision:3,original,vigente:original,entradas:[],total:0,pagina:0,tamanio:25};
const props={comunicacion:'com',catalog:{unidades:[],sedes:[],presencias:[],operadores:[],regiones:[{id:'R1',nombre:'Región sintética1'},{id:'R2',nombre:'Región sintética2'}]},version:0,changed:vi.fn()};
beforeEach(()=>{vi.clearAllMocks();vi.mocked(api.gestionGet).mockResolvedValue(data);vi.mocked(api.command).mockResolvedValue({id:'com',revision:4});});
async function editar(){fireEvent.click(await screen.findByText('Rectificar datos de captura'));fireEvent.change(screen.getByLabelText('Antecedente de captura corregido'),{target:{value:'Antecedente corregido con fundamento'}});fireEvent.change(screen.getByLabelText('Motivo de corrección de captura'),{target:{value:'Errata contrastada con respaldo'}});}

test('envía el contrato tipado con revisión, preserva precisión temporal y nunca edita original',async()=>{
 render(<RectificacionesIngreso {...props}/>);await editar();fireEvent.change(screen.getByLabelText('Región del asunto corregida'),{target:{value:'R2'}});const input=screen.getByLabelText('Recepción declarada corregida') as HTMLInputElement;fireEvent.change(input,{target:{value:input.value.replace(/\.120$/,'.12')}});fireEvent.click(screen.getByText('Confirmar rectificación de captura'));
 await waitFor(()=>expect(api.command).toHaveBeenCalledWith('/nucleo/comunicaciones/com/rectificaciones',{version:'1.0',revision:3,datos:{...original,texto:'Antecedente corregido con fundamento',regionAsunto:'R2'},motivo:'Errata contrastada con respaldo'},expect.any(String)));expect(props.changed).toHaveBeenCalledOnce();
});
test('respuesta incierta congela payload y clave aun si cambió la revisión visible',async()=>{
 vi.mocked(api.command).mockRejectedValueOnce(new api.ApiError(503,'NO_CONFIRMADO')).mockResolvedValue({id:'com',revision:4});const v=render(<RectificacionesIngreso {...props}/>);await editar();fireEvent.click(screen.getByText('Confirmar rectificación de captura'));await screen.findByText('Reintentar la misma rectificación');const first=vi.mocked(api.command).mock.calls[0];
 vi.mocked(api.gestionGet).mockResolvedValue({...data,revision:4});v.rerender(<RectificacionesIngreso {...props} version={1}/>);await screen.findByText('Reintentar la misma rectificación');expect(screen.getByLabelText('Antecedente de captura corregido').closest('fieldset')?.hasAttribute('disabled')).toBe(true);fireEvent.click(screen.getByText('Reintentar la misma rectificación'));await waitFor(()=>expect(api.command).toHaveBeenCalledTimes(2));expect(vi.mocked(api.command).mock.calls[1]).toEqual(first);
});
test('revocación al consultar constancia borra valores, conteos y vista anterior',async()=>{
 const entry={secuencia:7,revision:3,autor:'jefatura',registradaEn:'2026-11-02T13:00:00Z',motivo:'Corrección acreditada',anterior:original,nuevo:{...original,texto:'Valor actual protegido'}};vi.mocked(api.gestionGet).mockResolvedValueOnce({...data,vigente:entry.nuevo,entradas:[entry],total:1}).mockRejectedValueOnce(new api.ApiError(404,'FUERA_DE_ALCANCE'));
 render(<RectificacionesIngreso {...props}/>);fireEvent.click(await screen.findByText('Consultar constancia 7'));await screen.findByRole('alert');expect(screen.queryByText('Valor actual protegido')).toBeNull();expect(screen.queryByText('1 rectificaciones autorizadas.')).toBeNull();expect(screen.queryByText('Rectificar datos de captura')).toBeNull();
});
test('listados y constancia individual usan solo el recurso autorizado y la página exacta',async()=>{
 const entry={secuencia:7,revision:3,autor:'jefatura',registradaEn:'2026-11-02T13:00:00Z',motivo:'Corrección acreditada',anterior:original,nuevo:{...original,texto:'Valor actual'}};vi.mocked(api.gestionGet).mockResolvedValueOnce({...data,entradas:[entry],total:26}).mockResolvedValueOnce(entry).mockResolvedValue({...data,pagina:1,total:26});render(<RectificacionesIngreso {...props}/>);
 fireEvent.click(await screen.findByText('Consultar constancia 7'));await screen.findByRole('article',{name:'Constancia de rectificación'});expect(api.gestionGet).toHaveBeenCalledWith('/nucleo/comunicaciones/com/rectificaciones/7');fireEvent.click(screen.getByText('Rectificaciones siguientes'));await waitFor(()=>expect(api.gestionGet).toHaveBeenCalledWith('/nucleo/comunicaciones/com/rectificaciones?pagina=1&tamanio=25'));
});
test('rechazo definitivo permite revisar el borrador sin reenviar una clave obsoleta',async()=>{
 vi.mocked(api.command).mockRejectedValueOnce(new api.ApiError(409,'VERSION_OBSOLETA')).mockResolvedValue({id:'com',revision:4});render(<RectificacionesIngreso {...props}/>);await editar();fireEvent.click(screen.getByText('Confirmar rectificación de captura'));await screen.findByRole('alert');const first=vi.mocked(api.command).mock.calls[0];expect((screen.getByLabelText('Antecedente de captura corregido') as HTMLTextAreaElement).value).toBe('Antecedente corregido con fundamento');
 vi.mocked(api.gestionGet).mockResolvedValue({...data,revision:4});fireEvent.click(screen.getByText('Actualizar historial de captura'));await waitFor(()=>expect(api.gestionGet).toHaveBeenCalledTimes(2));await screen.findByText('Datos de captura vigentes para consulta');fireEvent.click(screen.getByText('Confirmar rectificación de captura'));await waitFor(()=>expect(api.command).toHaveBeenCalledTimes(2));expect(vi.mocked(api.command).mock.calls[1][2]).not.toBe(first[2]);expect(vi.mocked(api.command).mock.calls[1][1]).toEqual(expect.objectContaining({revision:4}));
});
