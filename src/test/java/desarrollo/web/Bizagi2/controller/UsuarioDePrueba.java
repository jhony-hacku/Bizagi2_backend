package desarrollo.web.Bizagi2.controller;

import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.Usuario;

// Usuario autenticado que reciben los controladores en las pruebas (@AuthenticationPrincipal)
final class UsuarioDePrueba {

    static final Integer EMPRESA_ID = 7;

    private UsuarioDePrueba() {
    }

    static Usuario administrador() {
        Empresa empresa = new Empresa();
        empresa.setId(EMPRESA_ID);
        empresa.setNombre("Acme SAS");
        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setNombre("Ana Perez");
        usuario.setEmail("ana@acme.com");
        usuario.setRolAcceso(RolAcceso.ADMINISTRADOR);
        usuario.setActivo(true);
        usuario.setEmpresa(empresa);
        return usuario;
    }
}
