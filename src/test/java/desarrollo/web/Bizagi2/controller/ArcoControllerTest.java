package desarrollo.web.Bizagi2.controller;

import static desarrollo.web.Bizagi2.controller.UsuarioDePrueba.EMPRESA_ID;
import static org.assertj.core.api.Assertions.assertThat;
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

import desarrollo.web.Bizagi2.dto.arco.ArcoRequest;
import desarrollo.web.Bizagi2.dto.arco.ArcoResponse;
import desarrollo.web.Bizagi2.dto.proceso.EliminacionResponse;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.ArcoService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArcoController - HU-11 a HU-13: arcos de secuencia")
class ArcoControllerTest {

    @Mock
    private ArcoService arcoService;

    @InjectMocks
    private ArcoController controller;

    private Usuario usuario;
    private ArcoResponse arco;

    @BeforeEach
    void setUp() {
        usuario = UsuarioDePrueba.administrador();
        arco = new ArcoResponse();
        arco.setId(5);
        arco.setPoolId(20);
    }

    @Test
    @DisplayName("GET /api/pools/{poolId}/arcos responde 200 con los arcos del pool")
    void listar() {
        when(arcoService.listarPorPoolDto(20, EMPRESA_ID)).thenReturn(List.of(arco));

        ResponseEntity<List<ArcoResponse>> respuesta = controller.listar(usuario, 20);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(arco);
    }

    @Test
    @DisplayName("POST /api/pools/{poolId}/arcos responde 201 con Location del arco creado")
    void crear() {
        ArcoRequest datos = new ArcoRequest(1, 2, "Si", null);
        when(arcoService.crear(20, usuario, datos)).thenReturn(arco);

        ResponseEntity<ArcoResponse> respuesta = controller.crear(usuario, 20, datos);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation()).hasToString("/api/arcos/5");
        assertThat(respuesta.getBody()).isSameAs(arco);
    }

    @Test
    @DisplayName("GET /api/arcos/{id} responde 200")
    void buscar() {
        when(arcoService.buscarPorIdDto(5, EMPRESA_ID)).thenReturn(arco);

        assertThat(controller.buscar(usuario, 5).getBody()).isSameAs(arco);
    }

    @Test
    @DisplayName("PUT /api/arcos/{id} responde 200 con el arco modificado")
    void actualizar() {
        ArcoRequest datos = new ArcoRequest(1, 3, null, null);
        when(arcoService.actualizar(5, usuario, datos)).thenReturn(arco);

        ResponseEntity<ArcoResponse> respuesta = controller.actualizar(usuario, 5, datos);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isSameAs(arco);
    }

    @Test
    @DisplayName("DELETE /api/arcos/{id} responde 200 con el resumen de la eliminacion")
    void eliminar() {
        EliminacionResponse eliminacion = new EliminacionResponse();
        when(arcoService.eliminar(5, usuario)).thenReturn(eliminacion);

        ResponseEntity<EliminacionResponse> respuesta = controller.eliminar(usuario, 5);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isSameAs(eliminacion);
    }
}
