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

import desarrollo.web.Bizagi2.dto.nodo.ActividadCreateRequest;
import desarrollo.web.Bizagi2.dto.nodo.ActividadResponse;
import desarrollo.web.Bizagi2.dto.nodo.ActividadUpdateRequest;
import desarrollo.web.Bizagi2.dto.nodo.EventoRequest;
import desarrollo.web.Bizagi2.dto.nodo.EventoResponse;
import desarrollo.web.Bizagi2.dto.nodo.GatewayRequest;
import desarrollo.web.Bizagi2.dto.nodo.GatewayResponse;
import desarrollo.web.Bizagi2.dto.proceso.EliminacionResponse;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.NodoFlujoService;

@ExtendWith(MockitoExtension.class)
@DisplayName("NodoFlujoController - actividades, gateways y eventos (HU-07 a HU-10, HU-14, HU-25, HU-27)")
class NodoFlujoControllerTest {

    @Mock
    private NodoFlujoService nodoFlujoService;

    @InjectMocks
    private NodoFlujoController controller;

    private Usuario usuario;
    private final EliminacionResponse eliminacion = new EliminacionResponse();

    @BeforeEach
    void setUp() {
        usuario = UsuarioDePrueba.administrador();
    }

    // ---------- Actividades ----------

    @Test
    @DisplayName("Actividades: listar, buscar, actualizar y eliminar responden 200")
    void actividades() {
        ActividadResponse actividad = new ActividadResponse();
        ActividadUpdateRequest cambios = new ActividadUpdateRequest();
        when(nodoFlujoService.listarActividadesDto(20, EMPRESA_ID)).thenReturn(List.of(actividad));
        when(nodoFlujoService.buscarActividadDto(50, EMPRESA_ID)).thenReturn(actividad);
        when(nodoFlujoService.actualizarActividad(50, usuario, cambios)).thenReturn(actividad);
        when(nodoFlujoService.eliminarActividad(50, usuario)).thenReturn(eliminacion);

        assertThat(controller.listarActividades(usuario, 20).getBody()).containsExactly(actividad);
        assertThat(controller.buscarActividad(usuario, 50).getBody()).isSameAs(actividad);
        assertThat(controller.actualizarActividad(usuario, 50, cambios).getBody()).isSameAs(actividad);
        assertThat(controller.eliminarActividad(usuario, 50).getBody()).isSameAs(eliminacion);
    }

    @Test
    @DisplayName("POST /api/lanes/{laneId}/actividades responde 201 con Location")
    void crearActividad() {
        ActividadCreateRequest datos = new ActividadCreateRequest();
        ActividadResponse creada = new ActividadResponse();
        creada.setId(50);
        when(nodoFlujoService.crearActividad(40, usuario, datos)).thenReturn(creada);

        ResponseEntity<ActividadResponse> respuesta = controller.crearActividad(usuario, 40, datos);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation()).hasToString("/api/actividades/50");
    }

    // ---------- Gateways ----------

    @Test
    @DisplayName("Gateways: listar, buscar, actualizar y eliminar responden 200")
    void gateways() {
        GatewayResponse gateway = new GatewayResponse();
        GatewayRequest cambios = new GatewayRequest();
        when(nodoFlujoService.listarGatewaysDto(20, EMPRESA_ID)).thenReturn(List.of(gateway));
        when(nodoFlujoService.buscarGatewayDto(60, EMPRESA_ID)).thenReturn(gateway);
        when(nodoFlujoService.actualizarGateway(60, usuario, cambios)).thenReturn(gateway);
        when(nodoFlujoService.eliminarGateway(60, usuario)).thenReturn(eliminacion);

        assertThat(controller.listarGateways(usuario, 20).getBody()).containsExactly(gateway);
        assertThat(controller.buscarGateway(usuario, 60).getBody()).isSameAs(gateway);
        assertThat(controller.actualizarGateway(usuario, 60, cambios).getBody()).isSameAs(gateway);
        assertThat(controller.eliminarGateway(usuario, 60).getBody()).isSameAs(eliminacion);
    }

    @Test
    @DisplayName("POST /api/pools/{poolId}/gateways responde 201 con Location")
    void crearGateway() {
        GatewayRequest datos = new GatewayRequest();
        GatewayResponse creado = new GatewayResponse();
        creado.setId(60);
        when(nodoFlujoService.crearGateway(20, usuario, datos)).thenReturn(creado);

        ResponseEntity<GatewayResponse> respuesta = controller.crearGateway(usuario, 20, datos);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation()).hasToString("/api/gateways/60");
    }

    // ---------- Eventos ----------

    @Test
    @DisplayName("Eventos: listar, buscar, actualizar y eliminar responden 200")
    void eventos() {
        EventoResponse evento = new EventoResponse();
        EventoRequest cambios = new EventoRequest();
        when(nodoFlujoService.listarEventosDto(20, EMPRESA_ID)).thenReturn(List.of(evento));
        when(nodoFlujoService.buscarEventoDto(70, EMPRESA_ID)).thenReturn(evento);
        when(nodoFlujoService.actualizarEvento(70, usuario, cambios)).thenReturn(evento);
        when(nodoFlujoService.eliminarEvento(70, usuario)).thenReturn(eliminacion);

        assertThat(controller.listarEventos(usuario, 20).getBody()).containsExactly(evento);
        assertThat(controller.buscarEvento(usuario, 70).getBody()).isSameAs(evento);
        assertThat(controller.actualizarEvento(usuario, 70, cambios).getBody()).isSameAs(evento);
        assertThat(controller.eliminarEvento(usuario, 70).getBody()).isSameAs(eliminacion);
    }

    @Test
    @DisplayName("POST /api/pools/{poolId}/eventos responde 201 con Location")
    void crearEvento() {
        EventoRequest datos = new EventoRequest();
        EventoResponse creado = new EventoResponse();
        creado.setId(70);
        when(nodoFlujoService.crearEvento(20, usuario, datos)).thenReturn(creado);

        ResponseEntity<EventoResponse> respuesta = controller.crearEvento(usuario, 20, datos);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation()).hasToString("/api/eventos/70");
    }
}
