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

import desarrollo.web.Bizagi2.dto.mensaje.MensajeRequest;
import desarrollo.web.Bizagi2.dto.mensaje.MensajeResponse;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.MensajeService;

@ExtendWith(MockitoExtension.class)
@DisplayName("MensajeController - HU-25 y HU-26: flujos de mensaje entre pools")
class MensajeControllerTest {

    @Mock
    private MensajeService mensajeService;

    @InjectMocks
    private MensajeController controller;

    private Usuario usuario;
    private MensajeResponse mensaje;

    @BeforeEach
    void setUp() {
        usuario = UsuarioDePrueba.administrador();
        mensaje = new MensajeResponse();
        mensaje.setId(3);
    }

    @Test
    @DisplayName("GET /api/procesos/{id}/mensajes responde 200")
    void listar() {
        when(mensajeService.listarPorProcesoDto(10, EMPRESA_ID)).thenReturn(List.of(mensaje));

        assertThat(controller.listar(usuario, 10).getBody()).containsExactly(mensaje);
    }

    @Test
    @DisplayName("POST /api/procesos/{id}/mensajes responde 201 con Location")
    void crear() {
        MensajeRequest datos = new MensajeRequest();
        when(mensajeService.crear(10, usuario, datos)).thenReturn(mensaje);

        ResponseEntity<MensajeResponse> respuesta = controller.crear(usuario, 10, datos);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation()).hasToString("/api/mensajes/3");
    }

    @Test
    @DisplayName("GET /api/mensajes/{id} responde 200")
    void buscar() {
        when(mensajeService.buscarPorIdDto(3, EMPRESA_ID)).thenReturn(mensaje);

        assertThat(controller.buscar(usuario, 3).getBody()).isSameAs(mensaje);
    }

    @Test
    @DisplayName("PUT /api/mensajes/{id} responde 200")
    void actualizar() {
        MensajeRequest datos = new MensajeRequest();
        when(mensajeService.actualizar(3, usuario, datos)).thenReturn(mensaje);

        assertThat(controller.actualizar(usuario, 3, datos).getBody()).isSameAs(mensaje);
    }

    @Test
    @DisplayName("DELETE /api/mensajes/{id} responde 204")
    void eliminar() {
        assertThat(controller.eliminar(usuario, 3).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(mensajeService).eliminar(3, usuario);
    }
}
