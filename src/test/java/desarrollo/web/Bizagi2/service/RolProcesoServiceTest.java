package desarrollo.web.Bizagi2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.Lane;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.RolProceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.repository.LaneRepository;
import desarrollo.web.Bizagi2.repository.RolProcesoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("RolProcesoServiceTest - Pruebas basadas en HU-17 a HU-20")
class RolProcesoServiceTest {

    @Mock
    private RolProcesoRepository rolProcesoRepository;

    @Mock
    private LaneRepository laneRepository;

    @Mock
    private EmpresaService empresaService;

    @Mock
    private HistorialService historialService;

    @InjectMocks
    private RolProcesoService rolProcesoService;

    private Usuario usuario;
    private Empresa empresa;

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(1);
        empresa.setNombre("Empresa Uno");

        usuario = new Usuario();
        usuario.setId(1);
        usuario.setRolAcceso(RolAcceso.ADMINISTRADOR);
        usuario.setEmpresa(empresa);
    }

    @Test
    @DisplayName("HU-17 CA1 & CA2: Crear rol de proceso con nombre y descripción únicos en la empresa")
    void crear_rolValido_exito() {
        RolProceso datos = new RolProceso();
        datos.setNombre("Analista de Crédito");
        datos.setDescripcion("Evalúa solicitudes de crédito");

        when(rolProcesoRepository.existsByEmpresaIdAndNombreIgnoreCase(1, "Analista de Crédito")).thenReturn(false);
        when(empresaService.buscarPorId(1)).thenReturn(empresa);
        when(rolProcesoRepository.save(any(RolProceso.class))).thenAnswer(i -> {
            RolProceso r = i.getArgument(0);
            r.setId(10);
            return r;
        });

        RolProceso resultado = rolProcesoService.crear(usuario, datos);

        assertThat(resultado.getId()).isEqualTo(10);
        assertThat(resultado.getNombre()).isEqualTo("Analista de Crédito");
        assertThat(resultado.isActivo()).isTrue();
        verify(historialService).registrar(eq(usuario), eq(null), eq(AccionHistorial.CREAR), eq("ROL"), eq(10), anyString());
    }

    @Test
    @DisplayName("HU-17 CA2: Nombre de rol duplicado en la misma empresa lanza ConflictoDominioException")
    void crear_nombreDuplicado_lanzaConflicto() {
        RolProceso datos = new RolProceso();
        datos.setNombre("Analista");

        when(rolProcesoRepository.existsByEmpresaIdAndNombreIgnoreCase(1, "Analista")).thenReturn(true);

        assertThatThrownBy(() -> rolProcesoService.crear(usuario, datos))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("Ya existe un rol con ese nombre en la empresa");
    }

    @Test
    @DisplayName("HU-18 CA1 & CA2: Actualizar rol de proceso modifica nombre y registra en historial")
    void actualizar_nombreValido_exito() {
        RolProceso rol = new RolProceso();
        rol.setId(10);
        rol.setNombre("Analista Junior");
        rol.setEmpresa(empresa);

        when(rolProcesoRepository.findById(10)).thenReturn(Optional.of(rol));
        when(rolProcesoRepository.existsByEmpresaIdAndNombreIgnoreCaseAndIdNot(1, "Analista Senior", 10)).thenReturn(false);
        when(rolProcesoRepository.save(any(RolProceso.class))).thenAnswer(i -> i.getArgument(0));

        RolProceso datos = new RolProceso();
        datos.setNombre("Analista Senior");
        datos.setDescripcion("Nuevo alcance");

        RolProceso actualizado = rolProcesoService.actualizar(10, usuario, datos);

        assertThat(actualizado.getNombre()).isEqualTo("Analista Senior");
        verify(historialService).registrar(eq(usuario), eq(null), eq(AccionHistorial.EDITAR), eq("ROL"), eq(10), anyString());
    }

    @Test
    @DisplayName("HU-19 CA1 & CA2: No se puede eliminar un rol si está siendo usado por alguna lane")
    void eliminar_rolEnUso_lanzaConflictoConNombresDeProcesos() {
        RolProceso rol = new RolProceso();
        rol.setId(10);
        rol.setNombre("Supervisor");
        rol.setEmpresa(empresa);

        Proceso proceso = new Proceso();
        proceso.setId(2);
        proceso.setNombre("Aprobación Hipotecaria");

        Pool pool = new Pool();
        pool.setProceso(proceso);

        Lane lane = new Lane();
        lane.setId(5);
        lane.setPool(pool);
        lane.setRolProceso(rol);

        when(rolProcesoRepository.findById(10)).thenReturn(Optional.of(rol));
        when(laneRepository.findByRolProcesoId(10)).thenReturn(List.of(lane));

        assertThatThrownBy(() -> rolProcesoService.eliminar(10, usuario))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("El rol esta en uso y no se puede eliminar")
                .hasMessageContaining("Aprobación Hipotecaria");
    }

    @Test
    @DisplayName("HU-19 CA4: Eliminar rol sin uso lo desactiva lógicamente (activo=false)")
    void eliminar_rolSinUso_desactivaLogicamente() {
        RolProceso rol = new RolProceso();
        rol.setId(10);
        rol.setNombre("Rol Sin Uso");
        rol.setActivo(true);
        rol.setEmpresa(empresa);

        when(rolProcesoRepository.findById(10)).thenReturn(Optional.of(rol));
        when(laneRepository.findByRolProcesoId(10)).thenReturn(List.of());

        rolProcesoService.eliminar(10, usuario);

        assertThat(rol.isActivo()).isFalse();
        verify(rolProcesoRepository).save(rol);
        verify(historialService).registrar(eq(usuario), eq(null), eq(AccionHistorial.ELIMINAR), eq("ROL"), eq(10), anyString());
    }

    @Test
    @DisplayName("HU-20 CA1, CA4 & CA5: Listar roles indica si están en uso y en qué procesos")
    void listar_retornaRolesConIndicadoresDeUso() {
        RolProceso rol = new RolProceso();
        rol.setId(1);
        rol.setNombre("Auditor");
        rol.setDescripcion("Auditor interno");

        when(rolProcesoRepository.buscar(eq(1), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(rol)));
        when(laneRepository.findByRolProcesoId(1)).thenReturn(List.of());

        Page<Map<String, Object>> pagina = rolProcesoService.listar(1, "", 0, 10);

        assertThat(pagina).isNotNull();
        assertThat(pagina.getContent()).hasSize(1);
        Map<String, Object> fila = pagina.getContent().get(0);
        assertThat(fila.get("nombre")).isEqualTo("Auditor");
        assertThat(fila.get("enUso")).isEqualTo(false);
        assertThat(fila.get("eliminable")).isEqualTo(true);
    }
}
