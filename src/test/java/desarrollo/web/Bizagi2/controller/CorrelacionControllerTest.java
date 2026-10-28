package desarrollo.web.Bizagi2.controller;

import static desarrollo.web.Bizagi2.controller.UsuarioDePrueba.EMPRESA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import desarrollo.web.Bizagi2.dto.correlacion.CorrelacionRequest;
import desarrollo.web.Bizagi2.dto.correlacion.CorrelacionResponse;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.CorrelacionService;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorrelacionController - HU-28: clave de correlacion de un mensaje")
class CorrelacionControllerTest {

    @Mock
    private CorrelacionService correlacionService;

    @InjectMocks
    private CorrelacionController controller;

    private Usuario usuario;
    private CorrelacionResponse correlacion;

    @BeforeEach
    void setUp() {
        usuario = UsuarioDePrueba.administrador();
        correlacion = new CorrelacionResponse();
        correlacion.setId(9);
    }

    @Test
    @DisplayName("GET /api/mensajes/{id}/correlacion responde 200")
    void obtener() {
        when(correlacionService.obtenerDto(3, EMPRESA_ID)).thenReturn(correlacion);

        assertThat(controller.obtener(usuario, 3).getBody()).isSameAs(correlacion);
    }

    @Test
    @DisplayName("POST /api/mensajes/{id}/correlacion responde 201 con Location")
    void crear() {
        CorrelacionRequest datos = new CorrelacionRequest();
        when(correlacionService.crear(3, usuario, datos)).thenReturn(correlacion);

        ResponseEntity<CorrelacionResponse> respuesta = controller.crear(usuario, 3, datos);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation()).hasToString("/api/mensajes/3/correlacion");
        assertThat(respuesta.getBody()).isSameAs(correlacion);
    }

    @Test
    @DisplayName("PUT /api/mensajes/{id}/correlacion responde 200")
    void actualizar() {
        CorrelacionRequest datos = new CorrelacionRequest();
        when(correlacionService.actualizar(3, usuario, datos)).thenReturn(correlacion);

        assertThat(controller.actualizar(usuario, 3, datos).getBody()).isSameAs(correlacion);
    }

    @Test
    @DisplayName("DELETE /api/mensajes/{id}/correlacion responde 204")
    void eliminar() {
        assertThat(controller.eliminar(usuario, 3).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(correlacionService).eliminar(3, usuario);
    }
}
