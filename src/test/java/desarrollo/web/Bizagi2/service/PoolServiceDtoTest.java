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
import desarrollo.web.Bizagi2.dto.pool.PoolRequest;
import desarrollo.web.Bizagi2.dto.pool.PoolResponse;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.TipoParticipante;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.repository.LaneRepository;
import desarrollo.web.Bizagi2.repository.MensajeRepository;
import desarrollo.web.Bizagi2.repository.NodoFlujoRepository;
import desarrollo.web.Bizagi2.repository.PoolRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("PoolService - conversion entre DTOs y entidades")
class PoolServiceDtoTest {

    @Mock
    private PoolRepository poolRepository;
    @Mock
    private LaneRepository laneRepository;
    @Mock
    private NodoFlujoRepository nodoFlujoRepository;
    @Mock
    private MensajeRepository mensajeRepository;
    @Mock
    private ProcesoService procesoService;
    @Mock
    private HistorialService historialService;
    @Spy
    private ModelMapper modelMapper = new ModelMapperConfig().modelMapper();

    @InjectMocks
    private PoolService servicio;

    private PoolService poolService;
    private final Usuario usuario = DatosDto.usuario();
    private final Pool pool = DatosDto.pool();

    @BeforeEach
    void setUp() {
        poolService = spy(servicio);
    }

    @Test
    @DisplayName("aResponse incluye el procesoId y el tipo de participante")
    void aResponse() {
        PoolResponse dto = poolService.aResponse(pool);

        assertThat(dto.getId()).isEqualTo(20);
        assertThat(dto.getProcesoId()).isEqualTo(10);
        assertThat(dto.getTipoParticipante()).isEqualTo(TipoParticipante.EMPRESA_PROPIETARIA);
    }

    @Test
    @DisplayName("listarPorProcesoDto y buscarPorIdDto devuelven DTOs")
    void consultas() {
        doReturn(List.of(pool)).when(poolService).listarPorProceso(10, EMPRESA_ID);
        doReturn(pool).when(poolService).buscarPorId(20, EMPRESA_ID);

        assertThat(poolService.listarPorProcesoDto(10, EMPRESA_ID)).extracting(PoolResponse::getNombre)
                .containsExactly("Acme");
        assertThat(poolService.buscarPorIdDto(20, EMPRESA_ID).getId()).isEqualTo(20);
    }

    @Test
    @DisplayName("crear y actualizar con PoolRequest convierten nombre y tipo")
    void crearYActualizar() {
        Pool externo = DatosDto.pool(21, TipoParticipante.PROVEEDOR);
        doReturn(externo).when(poolService).crear(eq(10), same(usuario), any(Pool.class));
        doReturn(externo).when(poolService).actualizar(eq(21), same(usuario), any(Pool.class));

        PoolResponse creado = poolService.crear(10, usuario, new PoolRequest("Proveedor", TipoParticipante.PROVEEDOR));
        PoolResponse editado = poolService.actualizar(21, usuario, new PoolRequest("Proveedor", TipoParticipante.PROVEEDOR));

        ArgumentCaptor<Pool> enviado = ArgumentCaptor.forClass(Pool.class);
        verify(poolService).crear(eq(10), same(usuario), enviado.capture());
        assertThat(enviado.getValue().getNombre()).isEqualTo("Proveedor");
        assertThat(enviado.getValue().getTipoParticipante()).isEqualTo(TipoParticipante.PROVEEDOR);
        assertThat(creado.getTipoParticipante()).isEqualTo(TipoParticipante.PROVEEDOR);
        assertThat(editado.getId()).isEqualTo(21);
    }
}
