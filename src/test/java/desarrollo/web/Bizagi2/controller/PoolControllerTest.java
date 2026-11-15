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

import desarrollo.web.Bizagi2.dto.pool.PoolRequest;
import desarrollo.web.Bizagi2.dto.pool.PoolResponse;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.PoolService;

@ExtendWith(MockitoExtension.class)
@DisplayName("PoolController - HU-21: pools del proceso")
class PoolControllerTest {

    @Mock
    private PoolService poolService;

    @InjectMocks
    private PoolController controller;

    private Usuario usuario;
    private PoolResponse pool;

    @BeforeEach
    void setUp() {
        usuario = UsuarioDePrueba.administrador();
        pool = new PoolResponse();
        pool.setId(20);
    }

    @Test
    @DisplayName("GET /api/procesos/{id}/pools responde 200")
    void listar() {
        when(poolService.listarPorProcesoDto(10, EMPRESA_ID)).thenReturn(List.of(pool));

        assertThat(controller.listar(usuario, 10).getBody()).containsExactly(pool);
    }

    @Test
    @DisplayName("POST /api/procesos/{id}/pools responde 201 con Location")
    void crear() {
        PoolRequest datos = new PoolRequest();
        when(poolService.crear(10, usuario, datos)).thenReturn(pool);

        ResponseEntity<PoolResponse> respuesta = controller.crear(usuario, 10, datos);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation()).hasToString("/api/pools/20");
    }

    @Test
    @DisplayName("GET /api/pools/{id} responde 200")
    void buscar() {
        when(poolService.buscarPorIdDto(20, EMPRESA_ID)).thenReturn(pool);

        assertThat(controller.buscar(usuario, 20).getBody()).isSameAs(pool);
    }

    @Test
    @DisplayName("PUT /api/pools/{id} responde 200")
    void actualizar() {
        PoolRequest datos = new PoolRequest();
        when(poolService.actualizar(20, usuario, datos)).thenReturn(pool);

        assertThat(controller.actualizar(usuario, 20, datos).getBody()).isSameAs(pool);
    }

    @Test
    @DisplayName("DELETE /api/pools/{id} responde 204")
    void eliminar() {
        assertThat(controller.eliminar(usuario, 20).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(poolService).eliminar(20, usuario);
    }
}
