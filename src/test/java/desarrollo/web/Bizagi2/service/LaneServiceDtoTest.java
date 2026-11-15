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
import desarrollo.web.Bizagi2.dto.lane.LaneCreateRequest;
import desarrollo.web.Bizagi2.dto.lane.LaneResponse;
import desarrollo.web.Bizagi2.dto.lane.LaneUpdateRequest;
import desarrollo.web.Bizagi2.entities.Lane;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.repository.LaneRepository;
import desarrollo.web.Bizagi2.repository.NodoFlujoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("LaneService - conversion entre DTOs y entidades")
class LaneServiceDtoTest {

    @Mock
    private LaneRepository laneRepository;
    @Mock
    private NodoFlujoRepository nodoFlujoRepository;
    @Mock
    private PoolService poolService;
    @Mock
    private RolProcesoService rolProcesoService;
    @Mock
    private ProcesoService procesoService;
    @Mock
    private HistorialService historialService;
    @Spy
    private ModelMapper modelMapper = new ModelMapperConfig().modelMapper();

    @InjectMocks
    private LaneService servicio;

    private LaneService laneService;
    private final Usuario usuario = DatosDto.usuario();
    private final Lane lane = DatosDto.lane();

    @BeforeEach
    void setUp() {
        laneService = spy(servicio);
    }

    @Test
    @DisplayName("aResponse aplana el pool y el rol de proceso de la lane")
    void aResponse() {
        LaneResponse dto = laneService.aResponse(lane);

        assertThat(dto.getPoolId()).isEqualTo(20);
        assertThat(dto.getRolProcesoId()).isEqualTo(30);
        assertThat(dto.getRolProcesoNombre()).isEqualTo("Analista");
    }

    @Test
    @DisplayName("listarPorPoolDto, buscarPorIdDto y reordenarDto devuelven DTOs")
    void consultas() {
        List<Integer> orden = List.of(40);
        doReturn(List.of(lane)).when(laneService).listarPorPool(20, EMPRESA_ID);
        doReturn(lane).when(laneService).buscarPorId(40, EMPRESA_ID);
        doReturn(List.of(lane)).when(laneService).reordenar(20, usuario, orden);

        assertThat(laneService.listarPorPoolDto(20, EMPRESA_ID)).extracting(LaneResponse::getId).containsExactly(40);
        assertThat(laneService.buscarPorIdDto(40, EMPRESA_ID).getNombre()).isEqualTo("Analistas");
        assertThat(laneService.reordenarDto(20, usuario, orden)).hasSize(1);
    }

    @Test
    @DisplayName("crear con LaneCreateRequest convierte rolProcesoId en la referencia al rol")
    void crear() {
        doReturn(lane).when(laneService).crear(eq(20), same(usuario), any(Lane.class));

        LaneResponse dto = laneService.crear(20, usuario, new LaneCreateRequest(30, "Analistas"));

        ArgumentCaptor<Lane> enviada = ArgumentCaptor.forClass(Lane.class);
        verify(laneService).crear(eq(20), same(usuario), enviada.capture());
        assertThat(enviada.getValue().getRolProceso().getId()).isEqualTo(30);
        assertThat(dto.getId()).isEqualTo(40);
    }

    @Test
    @DisplayName("actualizar con LaneUpdateRequest convierte rolProcesoId en la referencia al rol")
    void actualizar() {
        doReturn(lane).when(laneService).actualizar(eq(40), same(usuario), any(Lane.class));

        laneService.actualizar(40, usuario, new LaneUpdateRequest(31, null));

        ArgumentCaptor<Lane> enviada = ArgumentCaptor.forClass(Lane.class);
        verify(laneService).actualizar(eq(40), same(usuario), enviada.capture());
        assertThat(enviada.getValue().getRolProceso().getId()).isEqualTo(31);
    }
}
