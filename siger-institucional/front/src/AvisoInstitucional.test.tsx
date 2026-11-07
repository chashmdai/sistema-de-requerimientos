import {act,render,screen,fireEvent} from '@testing-library/react';
import {beforeEach,expect,test,vi} from 'vitest';
import {AvisoInstitucional,EnrutamientoAvisos} from './AvisoInstitucional';
import App from './App';import * as api from './api';
vi.mock('./api',async original=>({...await original<typeof import('./api')>(),gestionGet:vi.fn(),leerNotificacion:vi.fn(),session:vi.fn(),catalog:vi.fn(),contadorNotificaciones:vi.fn(),command:vi.fn()}));
const catalog:api.Catalog={unidades:[],sedes:[{id:'A',nombre:'Sede sintética'}],presencias:[],operadores:[],areas:[{id:'AREA_A',unidad:'UPR',sede:'A',nombre:'Área origen',jefatura:'COORDINACION',desde:'2000-01-01T00:00:00Z',hasta:'2099-01-01T00:00:00Z'},{id:'AREA_B',unidad:'UPR',sede:'A',nombre:'Área destino',jefatura:'COORDINACION',desde:'2000-01-01T00:00:00Z',hasta:'2099-01-01T00:00:00Z'}]};
const data={tipo:'SOL_REDIRIGIDA',fecha:'2026-11-07T08:00:00Z',folio:'COM-SINTETICA',origen:{unidad:'UPR',sede:'A',area:'AREA_A'},destino:{unidad:'UPR',sede:'A',area:'AREA_B'},ruta:null,pendienteActual:true};
beforeEach(()=>{vi.clearAllMocks();window.location.hash='';vi.mocked(api.gestionGet).mockResolvedValue(data);vi.mocked(api.leerNotificacion).mockResolvedValue(undefined);vi.mocked(api.catalog).mockResolvedValue(catalog);vi.mocked(api.contadorNotificaciones).mockResolvedValue({pendientes:1});vi.mocked(api.session).mockResolvedValue({autenticado:true,operador:{id:'head',nombre:'Jefatura sintética',rol:'JEFATURA',unidad:'UPR',sede:'A'},modo:'LABORATORIO',notificacionesHabilitadas:true});});

test('origen consulta referencia mínima sin ofrecer acceso a SOL actual',async()=>{
 render(<AvisoInstitucional id="notice" version={0} catalog={catalog}/>);await screen.findByText('Solicitud institucional redirigida');expect(screen.getByText('Área origen',{exact:false})).toBeTruthy();expect(screen.queryByText('Consultar recurso autorizado')).toBeNull();fireEvent.click(screen.getByText('Marcar aviso como leído'));await screen.findByText('Lectura del aviso registrada.');expect(api.leerNotificacion).toHaveBeenCalledWith('notice');expect(api.command).not.toHaveBeenCalled();
});
test('enlace se ofrece solo cuando el backend acredita recurso vigente',async()=>{
 vi.mocked(api.gestionGet).mockResolvedValue({...data,ruta:'sol/sol-autorizada'});render(<AvisoInstitucional id="notice" version={0} catalog={catalog}/>);expect((await screen.findByText('Consultar recurso autorizado')).getAttribute('href')).toBe('#sol/sol-autorizada');
});
test('contexto nuevo denegado descarta respuesta tardía del aviso anterior',async()=>{
 let resolve!:(v:typeof data)=>void;const pending=new Promise<typeof data>(r=>{resolve=r;});vi.mocked(api.gestionGet).mockReturnValueOnce(pending).mockRejectedValueOnce(new api.ApiError(404,'FUERA_DE_ALCANCE'));const view=render(<AvisoInstitucional id="old" version={0} catalog={catalog}/>);view.rerender(<AvisoInstitucional id="new" version={1} catalog={catalog}/>);await screen.findByRole('alert');await act(async()=>{resolve(data);});expect(screen.queryByText('COM-SINTETICA',{exact:false})).toBeNull();expect(screen.queryByText('Marcar aviso como leído')).toBeNull();
});
test('enrutamiento distingue pendiente de destinatario y aviso creado de envío o lectura',()=>{
 render(<EnrutamientoAvisos avisos={[{tipo:'SOL_DISTRIBUIDA',fecha:data.fecha,estado:'SIN_DESTINATARIO_VIGENTE'},{tipo:'REQ_ASIGNADO',fecha:data.fecha,estado:'PROYECTADO'}]}/>);expect(screen.getByText(/Pendiente de destinatario habilitado/)).toBeTruthy();expect(screen.getByText(/Avisos creados/)).toBeTruthy();expect(screen.queryByText('Correo enviado')).toBeNull();expect(screen.queryByText('Gestión confirmada')).toBeNull();
});
test('ruta de la aplicación conecta notificación propia con paquete institucional',async()=>{
 window.location.hash='#aviso-institucional/notice';render(<App/>);await screen.findByText('Solicitud institucional redirigida');expect(api.gestionGet).toHaveBeenCalledWith('/nucleo/notificaciones/notice/contexto-institucional');expect(screen.getByRole('heading',{name:'Aviso institucional'})).toBeTruthy();
});
test('saliente consulta constancia de asignación sin vínculo a Caso ni acuse automático',async()=>{
 vi.mocked(api.gestionGet).mockResolvedValue({...data,tipo:'CASO_ASIGNADO',origen:null,ruta:null,pendienteActual:false});render(<AvisoInstitucional id="aviso-saliente" version={0} catalog={catalog}/>);await screen.findByText('Caso asignado o reasignado');expect(screen.queryByText('Consultar recurso autorizado')).toBeNull();expect(screen.queryByText('Acusar mi asignación actual')).toBeNull();fireEvent.click(screen.getByText('Marcar aviso como leído'));await screen.findByText('Lectura del aviso registrada.');expect(api.leerNotificacion).toHaveBeenCalledWith('aviso-saliente');expect(api.command).not.toHaveBeenCalled();
});
test('cierre de REQ se consulta como decisión registrada sin inventar una reapertura',async()=>{
 vi.mocked(api.gestionGet).mockResolvedValue({...data,tipo:'REQ_TERMINADO',origen:null,ruta:'requerimiento/req-cerrado',pendienteActual:false});render(<AvisoInstitucional id="cierre" version={0} catalog={catalog}/>);await screen.findByText('Cierre explícito de Requerimiento registrado');expect(screen.getByText('Referencia histórica conservada.')).toBeTruthy();expect(screen.getByText('Consultar recurso autorizado').getAttribute('href')).toBe('#requerimiento/req-cerrado');expect(screen.queryByText('Aprobar reapertura del mismo Caso')).toBeNull();expect(api.command).not.toHaveBeenCalled();
});

test('toma de COM muestra referencia sin inventar destino o acuse',async()=>{vi.mocked(api.gestionGet).mockResolvedValue({...data,tipo:'COM_TOMADA',origen:null,destino:null,ruta:'comunicacion/com-sintetica'});render(<AvisoInstitucional id="toma" version={0} catalog={catalog}/>);await screen.findByText('Comunicación tomada para clasificar');expect(screen.queryByText('Destino:',{exact:false})).toBeNull();expect(screen.getByText('Consultar recurso autorizado').getAttribute('href')).toBe('#comunicacion/com-sintetica');expect(api.leerNotificacion).not.toHaveBeenCalled();expect(api.command).not.toHaveBeenCalled();});
