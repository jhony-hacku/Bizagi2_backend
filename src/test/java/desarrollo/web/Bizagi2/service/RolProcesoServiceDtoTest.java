package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.DatosDto.EMPRESA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import desarrollo.web.Bizagi2.config.ModelMapperConfig;
import desarrollo.web.Bizagi2.dto.proceso.HistorialResponse;
import desarrollo.web.Bizagi2.dto.rol.RolProcesoDetalleResponse;
import desarrollo.web.Bizagi2.dto.rol.RolProcesoRequest;
import desarrollo.web.Bizagi2.dto.rol.RolProcesoResponse;
import desarrollo.web.Bizagi2.entities.RolProceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.repository.LaneRepository;
import desarrollo.web.Bizagi2.repository.RolProcesoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("RolProcesoService - DTOs y detalle de uso del rol (HU-20)")
class RolProcesoServiceDtoTest {

    @Mock
    private RolProcesoRepository rolProcesoRepository;
    @Mock
    private LaneRepository laneRepository;
    @Mock
    private EmpresaService empresaService;
    @Mock
    private HistorialService historialService;
    @Spy
    private ModelMapper modelMapper = new ModelMapperConfig().modelMapper();

    @InjectMocks
    private RolProcesoService servicio;

    private RolProcesoService rolProcesoService;
    private final Usuario usuario = DatosDto.usuario();
    private final RolProceso rol = DatosDto.rol();

    @BeforeEach
    void setUp() {
        rolProcesoService = spy(servicio);
    }

    @Test
    @DisplayName("detalle de un rol usado en una lane: enUso, no eliminable y con el proceso")
    void detalleConUso() {
        doReturn(rol).when(rolProcesoService).buscarPorId(30, EMPRESA_ID);
        when(laneRepository.findByRolProcesoId(30)).thenReturn(List.of(DatosDto.lane()));

        RolProcesoDetalleResponse detalle = rolProcesoService.detalle(30, EMPRESA_ID);

        assertThat(detalle.isEnUso()).isTrue();
        assertThat(detalle.isEliminable()).isFalse();
        assertThat(detalle.getProcesos()).extracting("nombre").containsExactly("Credito");
    }

    @Test
    @DisplayName("detalle de un rol sin lanes: eliminable")
    void detalleSinUso() {
        doReturn(rol).when(rolProcesoService).buscarPorId(30, EMPRESA_ID);
        when(laneRepository.findByRolProcesoId(30)).thenReturn(List.of());

        RolProcesoDetalleResponse detalle = rolProcesoService.detalle(30, EMPRESA_ID);

        assertThat(detalle.isEliminable()).isTrue();
        assertThat(detalle.getProcesos()).isEmpty();
    }

    @Test
    @DisplayName("disponiblesDto e historialDto devuelven DTOs")
    void consultas() {
        doReturn(List.of(rol)).when(rolProcesoService).disponibles(EMPRESA_ID);
        doReturn(List.of(DatosDto.historial())).when(rolProcesoService).historial(30, EMPRESA_ID);

        assertThat(rolProcesoService.disponiblesDto(EMPRESA_ID)).extracting(RolProcesoResponse::getNombre)
                .containsExactly("Analista");
        assertThat(rolProcesoService.historialDto(30, EMPRESA_ID)).extracting(HistorialResponse::getUsuarioNombre)
                .containsExactly("Ana Perez");
    }

    @Test
    @DisplayName("crear y actualizar con RolProcesoRequest devuelven el rol")
    void crearYActualizar() {
        doReturn(rol).when(rolProcesoService).crear(same(usuario), any(RolProceso.class));
        doReturn(rol).when(rolProcesoService).actualizar(eq(30), same(usuario), any(RolProceso.class));
        RolProcesoRequest datos = new RolProcesoRequest("Analista", "Revisa solicitudes");

        assertThat(rolProcesoService.crear(usuario, datos).getId()).isEqualTo(30);
        assertThat(rolProcesoService.actualizar(30, usuario, datos).getDescripcion()).isEqualTo("Revisa solicitudes");
    }
}
