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

import desarrollo.web.Bizagi2.dto.diagrama.DiagramaResponse;
import desarrollo.web.Bizagi2.dto.proceso.AdvertenciaResponse;
import desarrollo.web.Bizagi2.dto.proceso.CompartirRequest;
import desarrollo.web.Bizagi2.dto.proceso.EmpresaInvitadaResponse;
import desarrollo.web.Bizagi2.dto.proceso.HistorialResponse;
import desarrollo.web.Bizagi2.dto.proceso.ProcesoCompartidoResponse;
import desarrollo.web.Bizagi2.dto.proceso.ProcesoRequest;
import desarrollo.web.Bizagi2.dto.proceso.ProcesoResponse;
import desarrollo.web.Bizagi2.entities.EstadoProceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.DiagramaService;
import desarrollo.web.Bizagi2.service.ProcesoCompartidoService;
import desarrollo.web.Bizagi2.service.ProcesoService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcesoController - HU-04 a HU-10, HU-15 y HU-23")
class ProcesoControllerTest {

    @Mock
    private ProcesoService procesoService;

    @Mock
    private DiagramaService diagramaService;

    @Mock
    private ProcesoCompartidoService procesoCompartidoService;

    @InjectMocks
    private ProcesoController controller;

    private Usuario usuario;
    private ProcesoResponse proceso;

    @BeforeEach
    void setUp() {
        usuario = UsuarioDePrueba.administrador();
        proceso = new ProcesoResponse();
        proceso.setId(10);
    }

    @Test
    @DisplayName("GET /api/procesos responde una pagina filtrada (HU-06)")
    void listar() {
        when(procesoService.listarDto(EMPRESA_ID, "cred", EstadoProceso.BORRADOR, "Finanzas", true, 0, 20))
                .thenReturn(new PageImpl<>(List.of(proceso)));

        ResponseEntity<PagedModel<ProcesoResponse>> respuesta =
                controller.listar(usuario, "cred", EstadoProceso.BORRADOR, "Finanzas", true, 0, 20);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().getContent()).containsExactly(proceso);
    }

    @Test
    @DisplayName("GET /api/procesos/{id} responde el proceso (propio o compartido)")
    void buscar() {
        when(procesoService.buscarParaLecturaDto(10, EMPRESA_ID)).thenReturn(proceso);

        assertThat(controller.buscar(usuario, 10).getBody()).isSameAs(proceso);
    }

    @Test
    @DisplayName("GET /api/procesos/compartidos lista los procesos compartidos con la empresa")
    void compartidosConmigo() {
        ProcesoCompartidoResponse compartido = new ProcesoCompartidoResponse();
        when(procesoCompartidoService.compartidosConmigo(EMPRESA_ID)).thenReturn(List.of(compartido));

        assertThat(controller.compartidosConmigo(usuario).getBody()).containsExactly(compartido);
    }

    @Test
    @DisplayName("GET /api/procesos/{id}/diagrama responde el diagrama completo")
    void diagrama() {
        DiagramaResponse diagrama = new DiagramaResponse();
        when(diagramaService.obtener(10, EMPRESA_ID)).thenReturn(diagrama);

        assertThat(controller.diagrama(usuario, 10).getBody()).isSameAs(diagrama);
    }

    @Test
    @DisplayName("GET /api/procesos/{id}/validacion responde las advertencias del diagrama")
    void validacion() {
        AdvertenciaResponse advertencia = new AdvertenciaResponse();
        when(procesoService.validar(10, EMPRESA_ID)).thenReturn(List.of(advertencia));

        assertThat(controller.validacion(usuario, 10).getBody()).containsExactly(advertencia);
    }

    @Test
    @DisplayName("GET /api/procesos/{id}/compartir lista las empresas invitadas (HU-23)")
    void invitadas() {
        EmpresaInvitadaResponse invitada = new EmpresaInvitadaResponse();
        when(procesoCompartidoService.invitadas(10, EMPRESA_ID)).thenReturn(List.of(invitada));

        assertThat(controller.invitadas(usuario, 10).getBody()).containsExactly(invitada);
    }

    @Test
    @DisplayName("POST /api/procesos/{id}/compartir responde 201 con la empresa invitada")
    void compartir() {
        CompartirRequest datos = new CompartirRequest();
        datos.setNit("800111222");
        EmpresaInvitadaResponse invitada = new EmpresaInvitadaResponse();
        when(procesoCompartidoService.compartir(10, usuario, "800111222")).thenReturn(invitada);

        ResponseEntity<EmpresaInvitadaResponse> respuesta = controller.compartir(usuario, 10, datos);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isSameAs(invitada);
    }

    @Test
    @DisplayName("DELETE /api/procesos/{id}/compartir/{empresaId} responde 204")
    void dejarDeCompartir() {
        assertThat(controller.dejarDeCompartir(usuario, 10, 8).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(procesoCompartidoService).dejarDeCompartir(10, usuario, 8);
    }

    @Test
    @DisplayName("GET /api/procesos/{id}/historial responde los cambios del proceso")
    void historial() {
        HistorialResponse cambio = new HistorialResponse();
        when(procesoService.historialDto(10, EMPRESA_ID)).thenReturn(List.of(cambio));

        assertThat(controller.historial(usuario, 10).getBody()).containsExactly(cambio);
    }

    @Test
    @DisplayName("POST /api/procesos responde 201 con Location (HU-04)")
    void crear() {
        ProcesoRequest datos = new ProcesoRequest();
        when(procesoService.crear(usuario, datos)).thenReturn(proceso);

        ResponseEntity<ProcesoResponse> respuesta = controller.crear(usuario, datos);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation()).hasToString("/api/procesos/10");
    }

    @Test
    @DisplayName("PUT /api/procesos/{id} responde 200 (HU-05)")
    void actualizar() {
        ProcesoRequest datos = new ProcesoRequest();
        when(procesoService.actualizar(10, usuario, datos)).thenReturn(proceso);

        assertThat(controller.actualizar(usuario, 10, datos).getBody()).isSameAs(proceso);
    }

    @Test
    @DisplayName("DELETE /api/procesos/{id} hace la eliminacion logica y responde 204")
    void eliminar() {
        assertThat(controller.eliminar(usuario, 10).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(procesoService).eliminar(10, usuario);
    }
}
