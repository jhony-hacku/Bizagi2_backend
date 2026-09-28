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
import org.springframework.security.access.AccessDeniedException;

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.TipoParticipante;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.LaneRepository;
import desarrollo.web.Bizagi2.repository.MensajeRepository;
import desarrollo.web.Bizagi2.repository.NodoFlujoRepository;
import desarrollo.web.Bizagi2.repository.PoolRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("PoolServiceTest - Pruebas basadas en HU-21, HU-23 y HU-24")
class PoolServiceTest {

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

    @InjectMocks
    private PoolService poolService;

    private Usuario admin;
    private Usuario lector;
    private Empresa empresa;
    private Proceso proceso;

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(1);
        empresa.setNombre("Empresa Uno");
        empresa.setEditorModificaEstructura(true);

        admin = new Usuario();
        admin.setId(1);
        admin.setRolAcceso(RolAcceso.ADMINISTRADOR);
        admin.setEmpresa(empresa);

        lector = new Usuario();
        lector.setId(2);
        lector.setRolAcceso(RolAcceso.LECTOR);
        lector.setEmpresa(empresa);

        proceso = new Proceso();
        proceso.setId(10);
        proceso.setEmpresa(empresa);
        proceso.setActivo(true);
    }

    @Test
    @DisplayName("HU-24 CA2 & CA3: Usuario con rol LECTOR no tiene permiso para crear o modificar estructura de pools")
    void verificarPermisoEstructura_lector_lanzaAccessDenied() {
        assertThatThrownBy(() -> poolService.verificarPermisoEstructura(lector))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No tienes permisos para modificar pools y lanes");
    }

    @Test
    @DisplayName("HU-21 CA1 & CA3: Crear pool de participante externo")
    void crear_poolExterno_exito() {
        when(procesoService.buscarActivo(10, 1)).thenReturn(proceso);
        when(poolRepository.save(any(Pool.class))).thenAnswer(i -> {
            Pool p = i.getArgument(0);
            p.setId(100);
            return p;
        });

        Pool datos = new Pool();
        datos.setNombre("Proveedor Logístico");
        datos.setTipoParticipante(TipoParticipante.PROVEEDOR);

        Pool resultado = poolService.crear(10, admin, datos);

        assertThat(resultado.getId()).isEqualTo(100);
        assertThat(resultado.getNombre()).isEqualTo("Proveedor Logístico");
        assertThat(resultado.getTipoParticipante()).isEqualTo(TipoParticipante.PROVEEDOR);
        verify(historialService).registrar(eq(admin), eq(proceso), eq(AccionHistorial.CREAR), eq("POOL"), eq(100), anyString());
    }

    @Test
    @DisplayName("HU-21 CA4: Verificar que pool externo es caja negra y rechaza elementos internos")
    void verificarInterno_poolExterno_lanzaReglaNegocio() {
        Pool poolExterno = new Pool();
        poolExterno.setNombre("Cliente");
        poolExterno.setTipoParticipante(TipoParticipante.CLIENTE);

        assertThatThrownBy(() -> poolService.verificarInterno(poolExterno))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("es un participante externo (caja negra) y no admite elementos internos");
    }

    @Test
    @DisplayName("HU-21 CA4: No se puede cambiar pool a externo si contiene lanes o elementos internos")
    void actualizar_pasaAExternoConLanes_lanzaConflicto() {
        Pool pool = new Pool();
        pool.setId(20);
        pool.setProceso(proceso);
        pool.setNombre("Pool Empresa");
        pool.setTipoParticipante(TipoParticipante.EMPRESA_PROPIETARIA);

        when(poolRepository.findById(20)).thenReturn(Optional.of(pool));
        when(procesoService.buscarActivo(10, 1)).thenReturn(proceso);
        when(laneRepository.existsByPoolId(20)).thenReturn(true);

        Pool datos = new Pool();
        datos.setNombre("Cambiado a Proveedor");
        datos.setTipoParticipante(TipoParticipante.PROVEEDOR);

        assertThatThrownBy(() -> poolService.actualizar(20, admin, datos))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("El pool tiene lanes o elementos y no puede pasar a ser un participante externo");
    }

    @Test
    @DisplayName("Eliminar pool solo se permite si está vacío sin lanes ni elementos")
    void eliminar_poolConLanes_lanzaConflicto() {
        Pool pool = new Pool();
        pool.setId(20);
        pool.setProceso(proceso);
        pool.setNombre("Pool");

        when(poolRepository.findById(20)).thenReturn(Optional.of(pool));
        when(procesoService.buscarActivo(10, 1)).thenReturn(proceso);
        when(laneRepository.existsByPoolId(20)).thenReturn(true);

        assertThatThrownBy(() -> poolService.eliminar(20, admin))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("El pool tiene lanes, elementos del flujo o mensajes dirigidos a el");
    }

    @Test
    @DisplayName("Eliminar pool vacío desactiva lógicamente (activo=false)")
    void eliminar_poolVacio_desactivaLogicamente() {
        Pool pool = new Pool();
        pool.setId(20);
        pool.setProceso(proceso);
        pool.setNombre("Pool");
        pool.setActivo(true);

        when(poolRepository.findById(20)).thenReturn(Optional.of(pool));
        when(procesoService.buscarActivo(10, 1)).thenReturn(proceso);
        when(laneRepository.existsByPoolId(20)).thenReturn(false);
        when(nodoFlujoRepository.existsByPoolId(20)).thenReturn(false);
        when(mensajeRepository.existsByDestinoPoolId(20)).thenReturn(false);

        poolService.eliminar(20, admin);

        assertThat(pool.isActivo()).isFalse();
        verify(poolRepository).save(pool);
        verify(historialService).registrar(eq(admin), eq(proceso), eq(AccionHistorial.ELIMINAR), eq("POOL"), eq(20), anyString());
    }
}
