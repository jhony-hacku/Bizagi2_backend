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

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Actividad;
import desarrollo.web.Bizagi2.entities.Arco;
import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.ArcoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArcoServiceTest - Pruebas basadas en HU-11, HU-12 y HU-13")
class ArcoServiceTest {

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

    @InjectMocks
    private ArcoService arcoService;

    private Usuario usuario;
    private Empresa empresa;
    private Proceso proceso;
    private Pool pool;
    private Actividad origen;
    private Actividad destino;

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(1);

        usuario = new Usuario();
        usuario.setId(10);
        usuario.setRolAcceso(RolAcceso.EDITOR);
        usuario.setEmpresa(empresa);

        proceso = new Proceso();
        proceso.setId(20);
        proceso.setEmpresa(empresa);

        pool = new Pool();
        pool.setId(30);
        pool.setProceso(proceso);

        origen = new Actividad();
        origen.setId(101);
        origen.setNombre("Actividad 1");
        origen.setPool(pool);

        destino = new Actividad();
        destino.setId(102);
        destino.setNombre("Actividad 2");
        destino.setPool(pool);
    }

    @Test
    @DisplayName("HU-11 CA1 & CA5: Crear arco de secuencia válido dentro del mismo pool")
    void crear_arcoValido_exito() {
        when(poolService.buscarPorId(30, 1)).thenReturn(pool);
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(nodoFlujoService.buscarNodo(101, 1)).thenReturn(origen);
        when(nodoFlujoService.buscarNodo(102, 1)).thenReturn(destino);
        when(arcoRepository.existsByOrigenIdAndDestinoId(101, 102)).thenReturn(false);
        when(arcoRepository.save(any(Arco.class))).thenAnswer(i -> {
            Arco a = i.getArgument(0);
            a.setId(500);
            return a;
        });

        Arco datos = new Arco();
        datos.setOrigen(origen);
        datos.setDestino(destino);

        Arco resultado = arcoService.crear(30, usuario, datos);

        assertThat(resultado.getId()).isEqualTo(500);
        assertThat(resultado.getOrigen()).isEqualTo(origen);
        assertThat(resultado.getDestino()).isEqualTo(destino);
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.CREAR), eq("ARCO"), eq(500), anyString());
    }

    @Test
    @DisplayName("HU-11 CA2: No se permite un arco cuyo origen y destino sean el mismo elemento")
    void crear_mismoOrigenYDestino_lanzaReglaNegocio() {
        when(poolService.buscarPorId(30, 1)).thenReturn(pool);
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(nodoFlujoService.buscarNodo(101, 1)).thenReturn(origen);

        Arco datos = new Arco();
        datos.setOrigen(origen);
        datos.setDestino(origen); // mismo nodo

        assertThatThrownBy(() -> arcoService.crear(30, usuario, datos))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Un arco no puede unir un elemento consigo mismo");
    }

    @Test
    @DisplayName("HU-11 CA3: No se permite un arco entre elementos de pools distintos")
    void crear_elementosEnDistintosPools_lanzaReglaNegocio() {
        Pool otroPool = new Pool();
        otroPool.setId(99);
        destino.setPool(otroPool);

        when(poolService.buscarPorId(30, 1)).thenReturn(pool);
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(nodoFlujoService.buscarNodo(101, 1)).thenReturn(origen);
        when(nodoFlujoService.buscarNodo(102, 1)).thenReturn(destino);

        Arco datos = new Arco();
        datos.setOrigen(origen);
        datos.setDestino(destino);

        assertThatThrownBy(() -> arcoService.crear(30, usuario, datos))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Un arco no puede cruzar pools");
    }

    @Test
    @DisplayName("HU-11 CA4: No se permiten dos arcos idénticos entre el mismo par de nodos")
    void crear_arcoDuplicado_lanzaConflicto() {
        when(poolService.buscarPorId(30, 1)).thenReturn(pool);
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(nodoFlujoService.buscarNodo(101, 1)).thenReturn(origen);
        when(nodoFlujoService.buscarNodo(102, 1)).thenReturn(destino);
        when(arcoRepository.existsByOrigenIdAndDestinoId(101, 102)).thenReturn(true);

        Arco datos = new Arco();
        datos.setOrigen(origen);
        datos.setDestino(destino);

        assertThatThrownBy(() -> arcoService.crear(30, usuario, datos))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("Ya existe un arco entre esos dos elementos");
    }

    @Test
    @DisplayName("HU-13 CA1 & CA4: Eliminar arco desactiva lógicamente y registra en historial")
    void eliminar_arcoExistente_desactivaLogicamente() {
        Arco arco = new Arco();
        arco.setId(500);
        arco.setPool(pool);
        arco.setOrigen(origen);
        arco.setDestino(destino);
        arco.setActivo(true);

        when(arcoRepository.findById(500)).thenReturn(Optional.of(arco));
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(validacionService.resultadoEliminacion(proceso, 1, 0)).thenReturn(Map.of("advertencias", List.of()));

        Map<String, Object> res = arcoService.eliminar(500, usuario);

        assertThat(arco.isActivo()).isFalse();
        verify(arcoRepository).save(arco);
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.ELIMINAR), eq("ARCO"), eq(500), anyString());
    }
}
