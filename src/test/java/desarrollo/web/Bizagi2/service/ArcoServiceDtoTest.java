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
import desarrollo.web.Bizagi2.dto.arco.ArcoRequest;
import desarrollo.web.Bizagi2.dto.arco.ArcoResponse;
import desarrollo.web.Bizagi2.entities.Arco;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.repository.ArcoRepository;

// Capa DTO de ArcoService: el request se convierte a entidad (ids planos -> referencias)
// y la entidad se devuelve como ArcoResponse.
@ExtendWith(MockitoExtension.class)
@DisplayName("ArcoService - conversion entre DTOs y entidades")
class ArcoServiceDtoTest {

    @Mock
    private ArcoRepository arcoRepository;
    @Mock
    private PoolService poolService;
    @Mock
    private NodoFlujoService nodoFlujoService;
    @Mock
    private ProcesoService procesoService;
    @Mock
    private HistorialService historialService;
    @Mock
    private ValidacionService validacionService;
    @Spy
    private ModelMapper modelMapper = new ModelMapperConfig().modelMapper();

    @InjectMocks
    private ArcoService servicio;

    private ArcoService arcoService;
    private final Usuario usuario = DatosDto.usuario();
    private final Arco arco = DatosDto.arco();

    @BeforeEach
    void setUp() {
        arcoService = spy(servicio);
    }

    @Test
    @DisplayName("aResponse aplana pool, origen y destino")
    void aResponse() {
        ArcoResponse dto = arcoService.aResponse(arco);

        assertThat(dto.getId()).isEqualTo(5);
        assertThat(dto.getPoolId()).isEqualTo(20);
        assertThat(dto.getOrigenId()).isEqualTo(50);
        assertThat(dto.getOrigenNombre()).isEqualTo("Revisar");
        assertThat(dto.getDestinoId()).isEqualTo(51);
        assertThat(dto.getEtiqueta()).isEqualTo("siguiente");
    }

    @Test
    @DisplayName("listarPorPoolDto y buscarPorIdDto devuelven DTOs")
    void consultas() {
        doReturn(List.of(arco)).when(arcoService).listarPorPool(20, EMPRESA_ID);
        doReturn(arco).when(arcoService).buscarPorId(5, EMPRESA_ID);

        assertThat(arcoService.listarPorPoolDto(20, EMPRESA_ID)).extracting(ArcoResponse::getId).containsExactly(5);
        assertThat(arcoService.buscarPorIdDto(5, EMPRESA_ID).getDestinoNombre()).isEqualTo("Aprobar");
    }

    @Test
    @DisplayName("crear con ArcoRequest pasa origenId y destinoId como referencias")
    void crear() {
        doReturn(arco).when(arcoService).crear(eq(20), same(usuario), any(Arco.class));

        ArcoResponse dto = arcoService.crear(20, usuario, new ArcoRequest(50, 51, "siguiente", null));

        ArgumentCaptor<Arco> enviado = ArgumentCaptor.forClass(Arco.class);
        verify(arcoService).crear(eq(20), same(usuario), enviado.capture());
        assertThat(enviado.getValue().getOrigen().getId()).isEqualTo(50);
        assertThat(enviado.getValue().getDestino().getId()).isEqualTo(51);
        assertThat(dto.getId()).isEqualTo(5);
    }

    @Test
    @DisplayName("actualizar con ArcoRequest devuelve el arco modificado")
    void actualizar() {
        doReturn(arco).when(arcoService).actualizar(eq(5), same(usuario), any(Arco.class));

        ArcoResponse dto = arcoService.actualizar(5, usuario, new ArcoRequest(50, 51, null, "monto > 100"));

        ArgumentCaptor<Arco> enviado = ArgumentCaptor.forClass(Arco.class);
        verify(arcoService).actualizar(eq(5), same(usuario), enviado.capture());
        assertThat(enviado.getValue().getCondicion()).isEqualTo("monto > 100");
        assertThat(dto.getPoolId()).isEqualTo(20);
    }
}
