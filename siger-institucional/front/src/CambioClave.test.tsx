import {render,screen,fireEvent,waitFor} from '@testing-library/react';
import {beforeEach,expect,test,vi} from 'vitest';
import App from './App';
import {CambioClave} from './CambioClave';
import * as api from './api';
vi.mock('./api',async original=>({...await original<typeof import('./api')>(),session:vi.fn(),catalog:vi.fn(),cambiarClavePersonal:vi.fn(),logout:vi.fn()}));
const temporal='Temporal-sintetica-123';const nueva='Personal-sintetica-456';
beforeEach(()=>{vi.clearAllMocks();vi.mocked(api.cambiarClavePersonal).mockResolvedValue(undefined);});
function escribir(){fireEvent.change(screen.getByLabelText('Contraseña actual'),{target:{value:temporal}});fireEvent.change(screen.getByLabelText('Nueva contraseña personal'),{target:{value:nueva}});fireEvent.change(screen.getByLabelText('Confirmar nueva contraseña'),{target:{value:nueva}});}
test('ACC-04 sesión temporal muestra cambio sin consultar catálogo o gestión',async()=>{
 vi.mocked(api.session).mockResolvedValue({autenticado:true,operador:null,modo:'LABORATORIO_INDIVIDUAL',cambioClaveObligatorio:true});render(<App/>);
 expect(await screen.findByText('Cambiar contraseña temporal')).toBeTruthy();expect(api.catalog).not.toHaveBeenCalled();expect(screen.queryByLabelText('Navegación principal')).toBeNull();
});
test('cambio conecta API personal, limpia campos y vuelve al login con confirmación',async()=>{
 vi.mocked(api.session).mockResolvedValueOnce({autenticado:true,operador:null,modo:'LABORATORIO_INDIVIDUAL',cambioClaveObligatorio:true}).mockResolvedValue({autenticado:false,operador:null,modo:'LABORATORIO_INDIVIDUAL'});
 render(<App/>);await screen.findByText('Cambiar contraseña temporal');escribir();fireEvent.click(screen.getByText('Cambiar contraseña y volver a ingresar'));
 await waitFor(()=>expect(api.cambiarClavePersonal).toHaveBeenCalledWith(temporal,nueva));expect(await screen.findByText('Acceso funcionario de laboratorio')).toBeTruthy();expect(screen.getByRole('status').textContent).toContain('Contraseña actualizada');expect(api.catalog).not.toHaveBeenCalled();
});
test('confirmación distinta y exceso UTF-8 no envían solicitud',async()=>{
 render(<CambioClave changed={vi.fn()} logout={vi.fn()}/>);escribir();fireEvent.change(screen.getByLabelText('Confirmar nueva contraseña'),{target:{value:'Otra-personal-123456'}});fireEvent.click(screen.getByText('Cambiar contraseña y volver a ingresar'));expect(await screen.findByRole('alert')).toBeTruthy();expect(api.cambiarClavePersonal).not.toHaveBeenCalled();
 fireEvent.change(screen.getByLabelText('Nueva contraseña personal'),{target:{value:'é'.repeat(40)}});fireEvent.change(screen.getByLabelText('Confirmar nueva contraseña'),{target:{value:'é'.repeat(40)}});fireEvent.click(screen.getByText('Cambiar contraseña y volver a ingresar'));expect(screen.getByRole('alert').textContent).toContain('72 bytes');expect(api.cambiarClavePersonal).not.toHaveBeenCalled();
});
test('error de credencial limpia secretos sin confirmar cambio',async()=>{
 const changed=vi.fn();vi.mocked(api.cambiarClavePersonal).mockRejectedValue(new api.ApiError(403,'CREDENCIAL_ACTUAL_INVALIDA'));
 render(<CambioClave changed={changed} logout={vi.fn()}/>);escribir();fireEvent.click(screen.getByText('Cambiar contraseña y volver a ingresar'));expect((await screen.findByRole('alert')).textContent).toContain('actual no es correcta');
 for(const label of ['Contraseña actual','Nueva contraseña personal','Confirmar nueva contraseña'])expect((screen.getByLabelText(label) as HTMLInputElement).value).toBe('');expect(changed).not.toHaveBeenCalled();
});
