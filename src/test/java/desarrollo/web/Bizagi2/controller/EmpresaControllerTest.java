package desarrollo.web.Bizagi2.controller;

import static desarrollo.web.Bizagi2.controller.UsuarioDePrueba.EMPRESA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import desarrollo.web.Bizagi2.dto.empresa.EmpresaRequest;
import desarrollo.web.Bizagi2.dto.empresa.EmpresaResponse;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.EmpresaService;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmpresaController - HU-24: consultar y editar la empresa")
class EmpresaControllerTest {

    @Mock
    private EmpresaService empresaService;

    @InjectMocks
    private EmpresaController controller;

    private final Usuario usuario = UsuarioDePrueba.administrador();

    @Test
    @DisplayName("GET /api/empresa responde 200 con la empresa del usuario autenticado")
    void miEmpresa() {
        EmpresaResponse empresa = new EmpresaResponse();
        when(empresaService.buscarPorIdDto(EMPRESA_ID)).thenReturn(empresa);

        ResponseEntity<EmpresaResponse> respuesta = controller.miEmpresa(usuario);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isSameAs(empresa);
    }

    @Test
    @DisplayName("PUT /api/empresa responde 200 con la empresa modificada")
    void actualizar() {
        EmpresaRequest datos = new EmpresaRequest();
        EmpresaResponse empresa = new EmpresaResponse();
        when(empresaService.actualizar(EMPRESA_ID, datos)).thenReturn(empresa);

        assertThat(controller.actualizar(usuario, datos).getBody()).isSameAs(empresa);
    }
}
