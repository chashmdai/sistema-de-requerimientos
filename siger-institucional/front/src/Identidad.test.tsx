import {render,screen,fireEvent,waitFor} from '@testing-library/react';
import {beforeEach,expect,test,vi} from 'vitest';
import {AdministracionTI,PerfilFuncionario} from './Identidad';
import * as api from './api';
vi.mock('./api',async original=>({...await original<typeof import('./api')>(),cuentas:vi.fn(),identidadPeticion:vi.fn(),organizacion:vi.fn()}));
const catalog:api.Catalog={unidades:[{codigo:'UPR',nombre:'Unidad sintética'}],sedes:[{id:'A',nombre:'Sede A'}],presencias:[{unidad:'UPR',sede:'A'}],operadores:[]};
const cuenta:api.Cuenta={id:'id1',usuario:'persona.lab',nombre:'Persona sintética',correo:'persona@example.invalid',cargo:'Cargo declarado',unidad:'UPR',sede:'A',activo:true,revision:3,asignaciones:[{id:'g1',role_code:'PROFESIONAL',scope_code:'UNIDAD_SEDE',unit_code:'UPR',site_id:'A',valid_from:1,valid_until:null,revoked_ms:null,granted_by:'ti',revoked_by:null}]};
beforeEach(()=>{vi.clearAllMocks();vi.mocked(api.cuentas).mockResolvedValue([cuenta]);vi.mocked(api.identidadPeticion).mockResolvedValue({});});
test('cargo no concede rol y combinar jefatura A con profesional B no concede jefatura B',()=>{
 const actor:api.Actor={id:'p',nombre:'Persona',rol:'JEFATURA',unidad:'UPR',sede:'A',cargo:'Director',accesos:[{rol:'COORDINADOR',alcance:'UNIDAD_SEDE',unidad:'UPR',sede:'A'},{rol:'PROFESIONAL',alcance:'UNIDAD_SEDE',unidad:'UPD',sede:'B'}]};
 expect(api.head(actor,'UPR','A')).toBe(true);expect(api.head(actor,'UPD','B')).toBe(false);expect(api.professional(actor,'UPD','B')).toBe(true);expect(api.distribute(actor,'A')).toBe(false);
 expect(api.head({...actor,accesos:[]})).toBe(false);
});
test('coordinador regional distribuye solo su sede y nunca web sin sede',()=>{
 const actor:api.Actor={id:'p',nombre:'Persona',rol:'JEFATURA',unidad:'UPR',sede:'A',accesos:[{rol:'COORDINADOR_REGIONAL',alcance:'SEDE',unidad:null,sede:'A'}]};
 expect(api.distribute(actor,'A')).toBe(true);expect(api.distribute(actor,'B')).toBe(false);expect(api.distribute(actor,null)).toBe(false);expect(api.head(actor,'UPD','A')).toBe(true);expect(api.head(actor,'UPD','B')).toBe(false);
});
test('perfil diferencia cargo de asignaciones sin atribuir facultad operativa a TI',()=>{
 render(<PerfilFuncionario actor={{id:'ti',nombre:'TI',rol:'TI',cargo:'Soporte',unidad:null,sede:null,accesos:[{rol:'TI',alcance:'INSTITUCION',unidad:null,sede:null}]}}/>);
 expect(screen.getByText('Soporte')).toBeTruthy();expect(screen.getByText(/TI no concede por sí solo acceso al contenido/)).toBeTruthy();
});
test('TI crea cuenta individual sin roles y borra contraseña del formulario al confirmar',async()=>{
 render(<AdministracionTI catalog={catalog}/>);await screen.findByText('Funcionarios registrados');
 fireEvent.change(screen.getByLabelText('Usuario individual'),{target:{value:'nueva.persona'}});fireEvent.change(screen.getByLabelText('Nombre del funcionario'),{target:{value:'Nombre sintético'}});fireEvent.change(screen.getByLabelText('Correo institucional'),{target:{value:'nueva@example.invalid'}});fireEvent.change(screen.getByLabelText('Cargo declarado'),{target:{value:'Administrativo'}});fireEvent.change(screen.getByLabelText('Contraseña individual inicial'),{target:{value:'Sintetica-Solo-Test-16'}});
 fireEvent.click(screen.getByText('Crear cuenta individual'));await waitFor(()=>expect(api.identidadPeticion).toHaveBeenCalledWith('/nucleo/ti/funcionarios','POST',expect.objectContaining({usuario:'nueva.persona',cargo:'Administrativo',unidad:null,sede:null})));expect(vi.mocked(api.identidadPeticion).mock.calls[0][2]).not.toHaveProperty('roles');await waitFor(()=>expect((screen.getByLabelText('Contraseña individual inicial')as HTMLInputElement).value).toBe(''));
});
test('desactivar incluye revision sin ocultar el efecto sobre sesiones vigentes',async()=>{
 render(<AdministracionTI catalog={catalog}/>);fireEvent.click(await screen.findByText('Administrar persona.lab'));fireEvent.click(screen.getByText('Desactivar cuenta e invalidar sesiones'));await waitFor(()=>expect(api.identidadPeticion).toHaveBeenCalledWith('/nucleo/ti/funcionarios/id1','PUT',expect.objectContaining({activo:false,revision:3})));
});
test('revocacion usa asignacion persistente y no borra cuenta ni documento',async()=>{
 render(<AdministracionTI catalog={catalog}/>);fireEvent.click(await screen.findByText('Administrar persona.lab'));fireEvent.click(screen.getByText('Revocar PROFESIONAL'));await waitFor(()=>expect(api.identidadPeticion).toHaveBeenCalledWith('/nucleo/ti/funcionarios/id1/roles/g1','DELETE',undefined));
});
test('rol TI exige ámbito institucional y no inventa alcance territorial',async()=>{
 render(<AdministracionTI catalog={catalog}/>);fireEvent.click(await screen.findByText('Administrar persona.lab'));fireEvent.change(screen.getByLabelText('Rol a otorgar'),{target:{value:'TI'}});fireEvent.click(screen.getByText('Otorgar rol y alcance'));await waitFor(()=>expect(api.identidadPeticion).toHaveBeenCalledWith('/nucleo/ti/funcionarios/id1/roles','POST',expect.objectContaining({rol:'TI',alcance:'INSTITUCION',unidad:null,sede:null,hasta:null})));
});
