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
import org.springframework.security.access.AccessDeniedException;

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.TipoParticipante;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.LaneRepository;
import desarrollo.web.Bizagi2.repository.MensajeRepository;
import desarrollo.web.Bizagi2.repository.NodoFlujoRepository;
import desarrollo.web.Bizagi2.repository.PoolRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("PoolService - HU-21 y HU-24: modificar pools y permisos de estructura")
class PoolServiceReglasTest {

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
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private PoolService poolService;

    private final Usuario usuario = DatosDto.usuario();
    private Pool pool;

    @BeforeEach
    void setUp() {
        pool = DatosDto.pool();
    }

    private Pool cambios(String nombre, TipoParticipante tipo) {
        Pool datos = new Pool();
        datos.setNombre(nombre);
        datos.setTipoParticipante(tipo);
        return datos;
    }

    @Test
    @DisplayName("listarPorProceso verifica el proceso y devuelve sus pools")
    void listarPorProceso() {
        when(poolRepository.findByProcesoId(10)).thenReturn(List.of(pool));

        assertThat(poolService.listarPorProceso(10, EMPRESA_ID)).containsExactly(pool);
        verify(procesoService).buscarPorId(10, EMPRESA_ID);
    }

    @Test
    @DisplayName("buscarPorId: inexistente o de otra empresa responde no encontrado")
    void buscarPorIdNoEncontrado() {
        when(poolRepository.findById(99)).thenReturn(Optional.empty());
        when(poolRepository.findById(20)).thenReturn(Optional.of(pool));

        assertThatThrownBy(() -> poolService.buscarPorId(99, EMPRESA_ID)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> poolService.buscarPorId(20, 8)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("HU-24: el editor modifica la estructura solo si la empresa lo permite; el lector nunca")
    void permisoEstructura() {
        Usuario editor = DatosDto.usuario();
        editor.setRolAcceso(RolAcceso.EDITOR);
        Usuario lector = DatosDto.usuario();
        lector.setRolAcceso(RolAcceso.LECTOR);

        poolService.verificarPermisoEstructura(editor);
        editor.getEmpresa().setEditorModificaEstructura(false);

        assertThatThrownBy(() -> poolService.verificarPermisoEstructura(editor)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> poolService.verificarPermisoEstructura(lector)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("HU-21: un pool externo es caja negra")
    void verificarInterno() {
        poolService.verificarInterno(pool);

        assertThatThrownBy(() -> poolService.verificarInterno(DatosDto.pool(21, TipoParticipante.CLIENTE)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("caja negra");
    }

    @Test
    @DisplayName("Renombrar un pool lo guarda y lo registra en el historial")
    void actualizar() {
        when(poolRepository.findById(20)).thenReturn(Optional.of(pool));
        when(poolRepository.save(any(Pool.class))).thenAnswer(i -> i.getArgument(0));

        Pool editado = poolService.actualizar(20, usuario, cambios("  Acme Bogota ", TipoParticipante.EMPRESA_PROPIETARIA));

        assertThat(editado.getNombre()).isEqualTo("Acme Bogota");
        verify(historialService).registrar(eq(usuario), any(), eq(AccionHistorial.EDITAR), eq("POOL"), eq(20), anyString());
    }

    @Test
    @DisplayName("Un pool vacio puede pasar a ser externo")
    void actualizarAExternoVacio() {
        when(poolRepository.findById(20)).thenReturn(Optional.of(pool));
        when(laneRepository.existsByPoolId(20)).thenReturn(false);
        when(nodoFlujoRepository.existsByPoolId(20)).thenReturn(false);
        when(poolRepository.save(any(Pool.class))).thenAnswer(i -> i.getArgument(0));

        Pool editado = poolService.actualizar(20, usuario, cambios("Proveedor", TipoParticipante.PROVEEDOR));

        assertThat(editado.getTipoParticipante()).isEqualTo(TipoParticipante.PROVEEDOR);
    }

    @Test
    @DisplayName("Un pool con lanes o elementos no puede pasar a ser externo")
    void actualizarAExternoConContenido() {
        when(poolRepository.findById(20)).thenReturn(Optional.of(pool));
        when(laneRepository.existsByPoolId(20)).thenReturn(false);
        when(nodoFlujoRepository.existsByPoolId(20)).thenReturn(true);

        assertThatThrownBy(() -> poolService.actualizar(20, usuario, cambios("Proveedor", TipoParticipante.PROVEEDOR)))
                .isInstanceOf(ConflictoDominioException.class);
    }

    @Test
    @DisplayName("No se elimina un pool que recibe mensajes")
    void eliminarConMensajes() {
        when(poolRepository.findById(20)).thenReturn(Optional.of(pool));
        when(laneRepository.existsByPoolId(20)).thenReturn(false);
        when(nodoFlujoRepository.existsByPoolId(20)).thenReturn(false);
        when(mensajeRepository.existsByDestinoPoolId(20)).thenReturn(true);

        assertThatThrownBy(() -> poolService.eliminar(20, usuario)).isInstanceOf(ConflictoDominioException.class);
    }
}
