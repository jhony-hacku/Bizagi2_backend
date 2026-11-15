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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import desarrollo.web.Bizagi2.dto.proceso.HistorialResponse;
import desarrollo.web.Bizagi2.dto.rol.RolProcesoDetalleResponse;
import desarrollo.web.Bizagi2.dto.rol.RolProcesoRequest;
import desarrollo.web.Bizagi2.dto.rol.RolProcesoResponse;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.RolProcesoService;

@ExtendWith(MockitoExtension.class)
@DisplayName("RolProcesoController - HU-17 a HU-20: roles de proceso")
class RolProcesoControllerTest {

    @Mock
    private RolProcesoService rolProcesoService;

    @InjectMocks
    private RolProcesoController controller;

    private Usuario usuario;
    private RolProcesoResponse rol;

    @BeforeEach
    void setUp() {
        usuario = UsuarioDePrueba.administrador();
        rol = new RolProcesoResponse();
        rol.setId(30);
    }

    @Test
    @DisplayName("GET /api/roles-proceso responde una pagina con los roles y su uso")
    void listar() {
        RolProcesoDetalleResponse detalle = new RolProcesoDetalleResponse();
        when(rolProcesoService.listar(EMPRESA_ID, "anal", 0, 20)).thenReturn(new PageImpl<>(List.of(detalle)));

        ResponseEntity<PagedModel<RolProcesoDetalleResponse>> respuesta = controller.listar(usuario, "anal", 0, 20);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().getContent()).containsExactly(detalle);
    }

    @Test
    @DisplayName("GET /api/roles-proceso/{id} responde el detalle del rol")
    void detalle() {
        RolProcesoDetalleResponse detalle = new RolProcesoDetalleResponse();
        when(rolProcesoService.detalle(30, EMPRESA_ID)).thenReturn(detalle);

        assertThat(controller.detalle(usuario, 30).getBody()).isSameAs(detalle);
    }

    @Test
    @DisplayName("POST /api/roles-proceso responde 201 con Location")
    void crear() {
        RolProcesoRequest datos = new RolProcesoRequest();
        when(rolProcesoService.crear(usuario, datos)).thenReturn(rol);

        ResponseEntity<RolProcesoResponse> respuesta = controller.crear(usuario, datos);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation()).hasToString("/api/roles-proceso/30");
    }

    @Test
    @DisplayName("PUT /api/roles-proceso/{id} responde 200")
    void actualizar() {
        RolProcesoRequest datos = new RolProcesoRequest();
        when(rolProcesoService.actualizar(30, usuario, datos)).thenReturn(rol);

        assertThat(controller.actualizar(usuario, 30, datos).getBody()).isSameAs(rol);
    }

    @Test
    @DisplayName("DELETE /api/roles-proceso/{id} responde 204")
    void eliminar() {
        assertThat(controller.eliminar(usuario, 30).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(rolProcesoService).eliminar(30, usuario);
    }

    @Test
    @DisplayName("GET /api/roles-proceso/{id}/historial responde los cambios del rol")
    void historial() {
        HistorialResponse cambio = new HistorialResponse();
        when(rolProcesoService.historialDto(30, EMPRESA_ID)).thenReturn(List.of(cambio));

        assertThat(controller.historial(usuario, 30).getBody()).containsExactly(cambio);
    }
}
