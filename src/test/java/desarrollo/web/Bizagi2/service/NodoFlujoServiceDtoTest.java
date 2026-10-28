package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.DatosDto.EMPRESA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import desarrollo.web.Bizagi2.config.ModelMapperConfig;
import desarrollo.web.Bizagi2.dto.nodo.ActividadCreateRequest;
import desarrollo.web.Bizagi2.dto.nodo.ActividadResponse;
import desarrollo.web.Bizagi2.dto.nodo.ActividadUpdateRequest;
import desarrollo.web.Bizagi2.dto.nodo.EventoRequest;
import desarrollo.web.Bizagi2.dto.nodo.EventoResponse;
import desarrollo.web.Bizagi2.dto.nodo.GatewayRequest;
import desarrollo.web.Bizagi2.dto.nodo.GatewayResponse;
import desarrollo.web.Bizagi2.entities.Actividad;
import desarrollo.web.Bizagi2.entities.Evento;
import desarrollo.web.Bizagi2.entities.Gateway;
import desarrollo.web.Bizagi2.entities.TipoActividad;
import desarrollo.web.Bizagi2.entities.TipoEvento;
import desarrollo.web.Bizagi2.entities.TipoGateway;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.repository.ArcoRepository;
import desarrollo.web.Bizagi2.repository.MensajeRepository;
import desarrollo.web.Bizagi2.repository.NodoFlujoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("NodoFlujoService - conversion de actividades, gateways y eventos a DTO")
class NodoFlujoServiceDtoTest {

    @Mock
    private NodoFlujoRepository nodoFlujoRepository;
    @Mock
    private ArcoRepository arcoRepository;
    @Mock
    private MensajeRepository mensajeRepository;
    @Mock
    private PoolService poolService;
    @Mock
    private LaneService laneService;
    @Mock
    private ProcesoService procesoService;
    @Mock
    private HistorialService historialService;
    @Mock
    private ValidacionService validacionService;
    @Spy
    private ModelMapper modelMapper = new ModelMapperConfig().modelMapper();

    @InjectMocks
    private NodoFlujoService servicio;

    private NodoFlujoService nodoFlujoService;
    private final Usuario usuario = DatosDto.usuario();

    @BeforeEach
    void setUp() {
        nodoFlujoService = spy(servicio);
    }

    // ---------- Actividades ----------

    @Test
    @DisplayName("Actividades: las consultas devuelven poolId y laneId")
    void consultasDeActividades() {
        Actividad actividad = DatosDto.actividad(50, "Revisar", TipoActividad.TAREA);
        doReturn(List.of(actividad)).when(nodoFlujoService).listarActividades(20, EMPRESA_ID);
        doReturn(actividad).when(nodoFlujoService).buscarActividad(50, EMPRESA_ID);

        ActividadResponse dto = nodoFlujoService.buscarActividadDto(50, EMPRESA_ID);

        assertThat(nodoFlujoService.listarActividadesDto(20, EMPRESA_ID)).hasSize(1);
        assertThat(dto.getPoolId()).isEqualTo(20);
        assertThat(dto.getLaneId()).isEqualTo(40);
        assertThat(dto.getTipo()).isEqualTo(TipoActividad.TAREA);
    }

    @Test
    @DisplayName("Actividades: crear y actualizar convierten el request (laneId -> lane)")
    void crearYActualizarActividad() {
        Actividad actividad = DatosDto.actividad(50, "Revisar", TipoActividad.TAREA);
        doReturn(actividad).when(nodoFlujoService).crearActividad(eq(40), same(usuario), any(Actividad.class));
        doReturn(actividad).when(nodoFlujoService).actualizarActividad(eq(50), same(usuario), any(Actividad.class));

        nodoFlujoService.crearActividad(40, usuario,
                new ActividadCreateRequest("Revisar", TipoActividad.TAREA, null, 100.0, 50.0));
        nodoFlujoService.actualizarActividad(50, usuario,
                new ActividadUpdateRequest("Revisar", TipoActividad.TAREA, null, null, null, 41));

        ArgumentCaptor<Actividad> creada = ArgumentCaptor.forClass(Actividad.class);
        verify(nodoFlujoService).crearActividad(eq(40), same(usuario), creada.capture());
        assertThat(creada.getValue().getPosicionX()).isEqualTo(100.0);
        ArgumentCaptor<Actividad> editada = ArgumentCaptor.forClass(Actividad.class);
        verify(nodoFlujoService).actualizarActividad(eq(50), same(usuario), editada.capture());
        assertThat(editada.getValue().getLane().getId()).isEqualTo(41);
    }

    // ---------- Gateways ----------

    @Test
    @DisplayName("Gateways: consultas, crear y actualizar devuelven GatewayResponse")
    void gateways() {
        Gateway gateway = DatosDto.gateway();
        GatewayRequest datos = new GatewayRequest("Aprobado?", TipoGateway.EXCLUSIVA, 200.0, 80.0);
        doReturn(gateway).when(nodoFlujoService).buscarGateway(60, EMPRESA_ID);
        doReturn(gateway).when(nodoFlujoService).crearGateway(eq(20), same(usuario), any(Gateway.class));
        doReturn(gateway).when(nodoFlujoService).actualizarGateway(eq(60), same(usuario), any(Gateway.class));

        GatewayResponse dto = nodoFlujoService.buscarGatewayDto(60, EMPRESA_ID);

        assertThat(dto.getPoolId()).isEqualTo(20);
        assertThat(dto.getTipoGateway()).isEqualTo(TipoGateway.EXCLUSIVA);
        assertThat(nodoFlujoService.crearGateway(20, usuario, datos).getId()).isEqualTo(60);
        assertThat(nodoFlujoService.actualizarGateway(60, usuario, datos).getNombre()).isEqualTo("Aprobado?");
    }

    @Test
    @DisplayName("Gateways: listarGatewaysDto convierte los gateways del pool")
    void listarGateways() {
        doReturn(List.of(DatosDto.gateway())).when(nodoFlujoService).listarGateways(20, EMPRESA_ID);

        assertThat(nodoFlujoService.listarGatewaysDto(20, EMPRESA_ID)).extracting(GatewayResponse::getId)
                .containsExactly(60);
    }

    // ---------- Eventos ----------

    @Test
    @DisplayName("Eventos: consultas, crear y actualizar devuelven EventoResponse con su contenido")
    void eventos() {
        Evento evento = DatosDto.evento(70, "PedidoConfirmado", TipoEvento.MENSAJE_LANZAMIENTO);
        evento.setContenido("numero:texto");
        EventoRequest datos = new EventoRequest("PedidoConfirmado", TipoEvento.MENSAJE_LANZAMIENTO, 10.0, 10.0,
                "numero:texto", null, null, Set.of());
        doReturn(List.of(evento)).when(nodoFlujoService).listarEventos(20, EMPRESA_ID);
        doReturn(evento).when(nodoFlujoService).buscarEvento(70, EMPRESA_ID);
        doReturn(evento).when(nodoFlujoService).crearEvento(eq(20), same(usuario), any(Evento.class));
        doReturn(evento).when(nodoFlujoService).actualizarEvento(eq(70), same(usuario), any(Evento.class));

        EventoResponse dto = nodoFlujoService.buscarEventoDto(70, EMPRESA_ID);

        assertThat(nodoFlujoService.listarEventosDto(20, EMPRESA_ID)).hasSize(1);
        assertThat(dto.getTipoEvento()).isEqualTo(TipoEvento.MENSAJE_LANZAMIENTO);
        assertThat(dto.getContenido()).isEqualTo("numero:texto");
        assertThat(nodoFlujoService.crearEvento(20, usuario, datos).getId()).isEqualTo(70);
        assertThat(nodoFlujoService.actualizarEvento(70, usuario, datos).getPoolId()).isEqualTo(20);
    }
}
