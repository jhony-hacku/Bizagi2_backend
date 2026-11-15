package desarrollo.web.Bizagi2.controller;

import static desarrollo.web.Bizagi2.controller.UsuarioDePrueba.EMPRESA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import desarrollo.web.Bizagi2.dto.usuario.UsuarioCreateRequest;
import desarrollo.web.Bizagi2.dto.usuario.UsuarioResponse;
import desarrollo.web.Bizagi2.dto.usuario.UsuarioUpdateRequest;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioController - HU-02: usuarios de la empresa")
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController controller;

    private Usuario usuario;
    private UsuarioResponse respuestaUsuario;

    @BeforeEach
    void setUp() {
        usuario = UsuarioDePrueba.administrador();
        respuestaUsuario = new UsuarioResponse();
        respuestaUsuario.setId(2);
    }

    @Test
    @DisplayName("GET /api/usuarios/me responde con el usuario autenticado")
    void me() {
        when(usuarioService.aResponse(usuario)).thenReturn(respuestaUsuario);

        assertThat(controller.me(usuario).getBody()).isSameAs(respuestaUsuario);
    }

    @Test
    @DisplayName("GET /api/usuarios responde 200 con los usuarios de la empresa")
    void listar() {
        when(usuarioService.listarDto(EMPRESA_ID)).thenReturn(List.of(respuestaUsuario));

        assertThat(controller.listar(usuario).getBody()).containsExactly(respuestaUsuario);
    }

    @Test
    @DisplayName("GET /api/usuarios/{id} responde 200")
    void buscar() {
        when(usuarioService.buscarPorIdDto(2, EMPRESA_ID)).thenReturn(respuestaUsuario);

        assertThat(controller.buscar(usuario, 2).getBody()).isSameAs(respuestaUsuario);
    }

    @Test
    @DisplayName("POST /api/usuarios responde 201 con Location")
    void crear() {
        UsuarioCreateRequest datos = new UsuarioCreateRequest();
        when(usuarioService.crear(EMPRESA_ID, datos)).thenReturn(respuestaUsuario);

        ResponseEntity<UsuarioResponse> respuesta = controller.crear(usuario, datos);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation()).hasToString("/api/usuarios/2");
    }

    @Test
    @DisplayName("PUT /api/usuarios/{id} responde 200")
    void actualizar() {
        UsuarioUpdateRequest datos = new UsuarioUpdateRequest();
        when(usuarioService.actualizar(2, EMPRESA_ID, datos)).thenReturn(respuestaUsuario);

        assertThat(controller.actualizar(usuario, 2, datos).getBody()).isSameAs(respuestaUsuario);
    }

    @Test
    @DisplayName("DELETE /api/usuarios/{id} desactiva y responde 204")
    void desactivar() {
        assertThat(controller.desactivar(usuario, 2).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(usuarioService).desactivar(2, EMPRESA_ID);
    }

    @Test
    @DisplayName("PUT /api/usuarios/{id}/activar responde 200")
    void activar() {
        when(usuarioService.activarDto(2, EMPRESA_ID)).thenReturn(respuestaUsuario);

        assertThat(controller.activar(usuario, 2).getBody()).isSameAs(respuestaUsuario);
    }
}
