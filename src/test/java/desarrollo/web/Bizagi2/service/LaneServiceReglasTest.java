package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.DatosDto.EMPRESA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Lane;
import desarrollo.web.Bizagi2.entities.RolProceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.LaneRepository;
import desarrollo.web.Bizagi2.repository.NodoFlujoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("LaneService - HU-22: modificar y reordenar lanes")
class LaneServiceReglasTest {

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
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private LaneService laneService;

    private final Usuario usuario = DatosDto.usuario();
    private Lane lane;

    @BeforeEach
    void setUp() {
        lane = DatosDto.lane();
    }

    private void laneExistente() {
        when(laneRepository.findById(40)).thenReturn(Optional.of(lane));
        when(laneRepository.save(any(Lane.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("listarPorPool verifica el pool y devuelve las lanes en orden")
    void listarPorPool() {
        when(laneRepository.findByPoolIdOrderByOrdenAscIdAsc(20)).thenReturn(List.of(lane));

        assertThat(laneService.listarPorPool(20, EMPRESA_ID)).containsExactly(lane);
        verify(poolService).buscarPorId(20, EMPRESA_ID);
    }

    @Test
    @DisplayName("buscarPorId: inexistente o de otra empresa responde no encontrado")
    void buscarPorIdNoEncontrado() {
        when(laneRepository.findById(99)).thenReturn(Optional.empty());
        when(laneRepository.findById(40)).thenReturn(Optional.of(lane));

        assertThatThrownBy(() -> laneService.buscarPorId(99, EMPRESA_ID)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> laneService.buscarPorId(40, 8)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Cambiar el rol sin nombre: la lane toma el nombre del nuevo rol")
    void actualizarRol() {
        laneExistente();
        RolProceso supervisor = DatosDto.rol();
        supervisor.setId(31);
        supervisor.setNombre("Supervisor");
        when(rolProcesoService.buscarPorId(31, EMPRESA_ID)).thenReturn(supervisor);
        Lane datos = new Lane();
        datos.setRolProceso(supervisor);

        Lane editada = laneService.actualizar(40, usuario, datos);

        assertThat(editada.getRolProceso().getId()).isEqualTo(31);
        assertThat(editada.getNombre()).isEqualTo("Supervisor");
        verify(poolService).verificarPermisoEstructura(usuario);
        verify(historialService).registrar(eq(usuario), any(), eq(AccionHistorial.EDITAR), eq("LANE"), eq(40), anyString());
    }

    @Test
    @DisplayName("Cambiar solo el nombre conserva el rol actual")
    void actualizarNombre() {
        laneExistente();
        Lane datos = new Lane();
        datos.setNombre("  Analistas senior ");

        Lane editada = laneService.actualizar(40, usuario, datos);

        assertThat(editada.getNombre()).isEqualTo("Analistas senior");
        assertThat(editada.getRolProceso().getId()).isEqualTo(30);
    }

    @Test
    @DisplayName("Reordenar exige los ids de todas las lanes del pool, sin repetir")
    void reordenarIncompleto() {
        when(poolService.buscarPorId(20, EMPRESA_ID)).thenReturn(lane.getPool());
        when(laneRepository.findByPoolIdOrderByOrdenAscIdAsc(20)).thenReturn(List.of(lane));

        assertThatThrownBy(() -> laneService.reordenar(20, usuario, List.of(40, 41)))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> laneService.reordenar(20, usuario, null))
                .isInstanceOf(ReglaNegocioException.class);
    }
}
