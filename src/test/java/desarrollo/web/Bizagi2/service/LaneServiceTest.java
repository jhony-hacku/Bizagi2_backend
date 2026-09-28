package desarrollo.web.Bizagi2.service;

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

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.Lane;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.RolProceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.LaneRepository;
import desarrollo.web.Bizagi2.repository.NodoFlujoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("LaneServiceTest - Pruebas basadas en HU-22 y HU-24")
class LaneServiceTest {

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

    @InjectMocks
    private LaneService laneService;

    private Usuario admin;
    private Empresa empresa;
    private Proceso proceso;
    private Pool pool;
    private RolProceso rol;

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(1);

        admin = new Usuario();
        admin.setId(1);
        admin.setRolAcceso(RolAcceso.ADMINISTRADOR);
        admin.setEmpresa(empresa);

        proceso = new Proceso();
        proceso.setId(10);
        proceso.setEmpresa(empresa);

        pool = new Pool();
        pool.setId(20);
        pool.setProceso(proceso);
        pool.setNombre("Pool Empresa");

        rol = new RolProceso();
        rol.setId(30);
        rol.setNombre("Analista");
    }

    @Test
    @DisplayName("HU-22 CA1 & CA2: Crear lane asociándola a un rol de proceso")
    void crear_laneValida_exito() {
        when(poolService.buscarPorId(20, 1)).thenReturn(pool);
        when(procesoService.buscarActivo(10, 1)).thenReturn(proceso);
        when(rolProcesoService.buscarPorId(30, 1)).thenReturn(rol);
        when(laneRepository.findByPoolIdOrderByOrdenAscIdAsc(20)).thenReturn(List.of());
        when(laneRepository.save(any(Lane.class))).thenAnswer(i -> {
            Lane l = i.getArgument(0);
            l.setId(100);
            return l;
        });

        Lane datos = new Lane();
        datos.setRolProceso(rol);

        Lane resultado = laneService.crear(20, admin, datos);

        assertThat(resultado.getId()).isEqualTo(100);
        assertThat(resultado.getNombre()).isEqualTo("Analista");
        assertThat(resultado.getRolProceso().getId()).isEqualTo(30);
        assertThat(resultado.getOrden()).isEqualTo(0);
        verify(historialService).registrar(eq(admin), eq(proceso), eq(AccionHistorial.CREAR), eq("LANE"), eq(100), anyString());
    }

    @Test
    @DisplayName("HU-22 CA2: Crear lane sin rol de proceso lanza ReglaNegocioException")
    void crear_sinRol_lanzaReglaNegocio() {
        when(poolService.buscarPorId(20, 1)).thenReturn(pool);
        when(procesoService.buscarActivo(10, 1)).thenReturn(proceso);

        Lane datos = new Lane();

        assertThatThrownBy(() -> laneService.crear(20, admin, datos))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("La lane requiere un rol de proceso");
    }

    @Test
    @DisplayName("HU-22 CA1: Reordenar lanes actualiza el orden secuencial de todas las lanes del pool")
    void reordenar_idsValidos_actualizaOrden() {
        Lane lane1 = new Lane();
        lane1.setId(101);
        lane1.setPool(pool);
        lane1.setOrden(0);

        Lane lane2 = new Lane();
        lane2.setId(102);
        lane2.setPool(pool);
        lane2.setOrden(1);

        when(poolService.buscarPorId(20, 1)).thenReturn(pool);
        when(procesoService.buscarActivo(10, 1)).thenReturn(proceso);
        when(laneRepository.findByPoolIdOrderByOrdenAscIdAsc(20)).thenReturn(List.of(lane1, lane2));

        laneService.reordenar(20, admin, List.of(102, 101));

        assertThat(lane2.getOrden()).isEqualTo(0);
        assertThat(lane1.getOrden()).isEqualTo(1);
        verify(laneRepository).save(lane1);
        verify(laneRepository).save(lane2);
    }

    @Test
    @DisplayName("HU-22 CA6: No se puede eliminar una lane que contiene actividades")
    void eliminar_laneConActividades_lanzaConflicto() {
        Lane lane = new Lane();
        lane.setId(101);
        lane.setPool(pool);

        when(laneRepository.findById(101)).thenReturn(Optional.of(lane));
        when(procesoService.buscarActivo(10, 1)).thenReturn(proceso);
        when(nodoFlujoRepository.contarActividadesPorLane(101)).thenReturn(2L);

        assertThatThrownBy(() -> laneService.eliminar(101, admin))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("La lane tiene 2 actividad(es): reasignalas a otra lane primero");
    }

    @Test
    @DisplayName("HU-22 CA6: Eliminar lane vacía desactiva lógicamente (activo=false)")
    void eliminar_laneVacia_desactivaLogicamente() {
        Lane lane = new Lane();
        lane.setId(101);
        lane.setNombre("Lane Vacía");
        lane.setPool(pool);
        lane.setActivo(true);

        when(laneRepository.findById(101)).thenReturn(Optional.of(lane));
        when(procesoService.buscarActivo(10, 1)).thenReturn(proceso);
        when(nodoFlujoRepository.contarActividadesPorLane(101)).thenReturn(0L);

        laneService.eliminar(101, admin);

        assertThat(lane.isActivo()).isFalse();
        verify(laneRepository).save(lane);
        verify(historialService).registrar(eq(admin), eq(proceso), eq(AccionHistorial.ELIMINAR), eq("LANE"), eq(101), anyString());
    }
}
