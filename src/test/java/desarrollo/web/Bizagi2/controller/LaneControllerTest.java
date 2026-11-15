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

import desarrollo.web.Bizagi2.dto.lane.LaneCreateRequest;
import desarrollo.web.Bizagi2.dto.lane.LaneResponse;
import desarrollo.web.Bizagi2.dto.lane.LaneUpdateRequest;
import desarrollo.web.Bizagi2.dto.rol.RolProcesoResponse;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.LaneService;
import desarrollo.web.Bizagi2.service.PoolService;
import desarrollo.web.Bizagi2.service.RolProcesoService;

@ExtendWith(MockitoExtension.class)
@DisplayName("LaneController - HU-22: lanes del pool asociadas a un rol de proceso")
class LaneControllerTest {

    @Mock
    private LaneService laneService;

    @Mock
    private RolProcesoService rolProcesoService;

    @Mock
    private PoolService poolService;

    @InjectMocks
    private LaneController controller;

    private Usuario usuario;
    private LaneResponse lane;

    @BeforeEach
    void setUp() {
        usuario = UsuarioDePrueba.administrador();
        lane = new LaneResponse();
        lane.setId(40);
    }

    @Test
    @DisplayName("GET /api/pools/{poolId}/lanes responde 200 con las lanes en orden")
    void listar() {
        when(laneService.listarPorPoolDto(20, EMPRESA_ID)).thenReturn(List.of(lane));

        assertThat(controller.listar(usuario, 20).getBody()).containsExactly(lane);
    }

    @Test
    @DisplayName("GET /api/pools/{poolId}/roles-disponibles valida el pool y lista los roles de la empresa")
    void rolesDisponibles() {
        RolProcesoResponse rol = new RolProcesoResponse();
        when(rolProcesoService.disponiblesDto(EMPRESA_ID)).thenReturn(List.of(rol));

        ResponseEntity<List<RolProcesoResponse>> respuesta = controller.rolesDisponibles(usuario, 20);

        verify(poolService).buscarPorId(20, EMPRESA_ID);
        assertThat(respuesta.getBody()).containsExactly(rol);
    }

    @Test
    @DisplayName("POST /api/pools/{poolId}/lanes responde 201 con Location")
    void crear() {
        LaneCreateRequest datos = new LaneCreateRequest();
        when(laneService.crear(20, usuario, datos)).thenReturn(lane);

        ResponseEntity<LaneResponse> respuesta = controller.crear(usuario, 20, datos);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation()).hasToString("/api/lanes/40");
    }

    @Test
    @DisplayName("PUT /api/pools/{poolId}/lanes/orden responde 200 con el nuevo orden")
    void reordenar() {
        List<Integer> orden = List.of(41, 40);
        when(laneService.reordenarDto(20, usuario, orden)).thenReturn(List.of(lane));

        assertThat(controller.reordenar(usuario, 20, orden).getBody()).containsExactly(lane);
    }

    @Test
    @DisplayName("GET /api/lanes/{id} responde 200")
    void buscar() {
        when(laneService.buscarPorIdDto(40, EMPRESA_ID)).thenReturn(lane);

        assertThat(controller.buscar(usuario, 40).getBody()).isSameAs(lane);
    }

    @Test
    @DisplayName("PUT /api/lanes/{id} responde 200")
    void actualizar() {
        LaneUpdateRequest datos = new LaneUpdateRequest();
        when(laneService.actualizar(40, usuario, datos)).thenReturn(lane);

        assertThat(controller.actualizar(usuario, 40, datos).getBody()).isSameAs(lane);
    }

    @Test
    @DisplayName("DELETE /api/lanes/{id} responde 204")
    void eliminar() {
        assertThat(controller.eliminar(usuario, 40).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(laneService).eliminar(40, usuario);
    }
}
