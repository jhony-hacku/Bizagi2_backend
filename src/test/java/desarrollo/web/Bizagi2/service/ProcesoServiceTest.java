package desarrollo.web.Bizagi2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.EstadoProceso;
import desarrollo.web.Bizagi2.entities.Historial;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.TipoParticipante;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.PoolRepository;
import desarrollo.web.Bizagi2.repository.ProcesoCompartidoRepository;
import desarrollo.web.Bizagi2.repository.ProcesoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcesoServiceTest - Pruebas basadas en HU-04, HU-05, HU-06 y HU-07")
class ProcesoServiceTest {

    @Mock
    private ProcesoRepository procesoRepository;

    @Mock
    private PoolRepository poolRepository;

    @Mock
    private ProcesoCompartidoRepository procesoCompartidoRepository;

    @Mock
    private HistorialService historialService;

    @Mock
    private ValidacionService validacionService;

    @InjectMocks
    private ProcesoService procesoService;

    private Usuario usuario;
    private Empresa empresa;

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(1);
        empresa.setNombre("Empresa Uno");

        usuario = new Usuario();
        usuario.setId(10);
        usuario.setRolAcceso(RolAcceso.EDITOR);
        usuario.setEmpresa(empresa);
    }

    @Test
    @DisplayName("HU-04 CA1, CA2 & CA3: Crear proceso nace en BORRADOR y crea pool de empresa")
    void crear_datosValidos_creaProcesoYPool() {
        Proceso datos = new Proceso();
        datos.setNombre("Proceso Ventas");
        datos.setDescripcion("Descripcion de ventas");
        datos.setCategoria("Comercial");

        when(procesoRepository.existsByEmpresaIdAndActivoTrueAndNombreIgnoreCase(1, "Proceso Ventas")).thenReturn(false);
        when(procesoRepository.save(any(Proceso.class))).thenAnswer(i -> {
            Proceso p = i.getArgument(0);
            p.setId(100);
            return p;
        });

        Proceso resultado = procesoService.crear(usuario, datos);

        assertThat(resultado.getId()).isEqualTo(100);
        assertThat(resultado.getEstado()).isEqualTo(EstadoProceso.BORRADOR);
        assertThat(resultado.isActivo()).isTrue();

        ArgumentCaptor<Pool> poolCaptor = ArgumentCaptor.forClass(Pool.class);
        verify(poolRepository).save(poolCaptor.capture());
        assertThat(poolCaptor.getValue().getTipoParticipante()).isEqualTo(TipoParticipante.EMPRESA_PROPIETARIA);
        verify(historialService).registrar(eq(usuario), eq(resultado), eq(AccionHistorial.CREAR), eq("PROCESO"), eq(100), anyString());
    }

    @Test
    @DisplayName("HU-04 CA4: Nombre duplicado en la misma empresa lanza ConflictoDominioException")
    void crear_nombreDuplicado_lanzaConflicto() {
        Proceso datos = new Proceso();
        datos.setNombre("Proceso Ventas");

        when(procesoRepository.existsByEmpresaIdAndActivoTrueAndNombreIgnoreCase(1, "Proceso Ventas")).thenReturn(true);

        assertThatThrownBy(() -> procesoService.crear(usuario, datos))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("Ya existe un proceso con ese nombre en la empresa");
    }

    @Test
    @DisplayName("HU-05 CA2 & CA3: Actualizar proceso registra los cambios en el historial")
    void actualizar_datosValidos_registraHistorial() {
        Proceso existente = new Proceso();
        existente.setId(5);
        existente.setNombre("Proceso Original");
        existente.setEstado(EstadoProceso.BORRADOR);
        existente.setActivo(true);
        existente.setEmpresa(empresa);

        when(procesoRepository.findById(5)).thenReturn(Optional.of(existente));
        when(procesoRepository.existsByEmpresaIdAndActivoTrueAndNombreIgnoreCaseAndIdNot(1, "Proceso Modificado", 5)).thenReturn(false);
        when(procesoRepository.save(any(Proceso.class))).thenAnswer(i -> i.getArgument(0));

        Proceso datos = new Proceso();
        datos.setNombre("Proceso Modificado");
        datos.setDescripcion("Nueva desc");

        Proceso actualizado = procesoService.actualizar(5, usuario, datos);

        assertThat(actualizado.getNombre()).isEqualTo("Proceso Modificado");
        verify(historialService).registrar(eq(usuario), eq(existente), eq(AccionHistorial.EDITAR), eq("PROCESO"), eq(5), contains("nombre"));
    }

    @Test
    @DisplayName("HU-05 CA5: Pasar a PUBLICADO con errores en el diagrama es rechazado")
    void actualizar_publicarConErrores_lanzaReglaNegocio() {
        Proceso existente = new Proceso();
        existente.setId(5);
        existente.setNombre("Proceso");
        existente.setEstado(EstadoProceso.BORRADOR);
        existente.setActivo(true);
        existente.setEmpresa(empresa);

        when(procesoRepository.findById(5)).thenReturn(Optional.of(existente));
        when(procesoRepository.existsByEmpresaIdAndActivoTrueAndNombreIgnoreCaseAndIdNot(1, "Proceso", 5)).thenReturn(false);
        when(validacionService.errores(existente)).thenReturn(List.of("Falta evento de inicio"));

        Proceso datos = new Proceso();
        datos.setNombre("Proceso");
        datos.setEstado(EstadoProceso.PUBLICADO);

        assertThatThrownBy(() -> procesoService.actualizar(5, usuario, datos))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("No se puede publicar el proceso: Falta evento de inicio");
    }

    @Test
    @DisplayName("HU-06 CA2 & CA5: Eliminar proceso es eliminación lógica (activo=false) y registra en historial")
    void eliminar_procesoActivo_desactivaLogicamente() {
        Proceso existente = new Proceso();
        existente.setId(5);
        existente.setActivo(true);
        existente.setEmpresa(empresa);

        when(procesoRepository.findById(5)).thenReturn(Optional.of(existente));

        procesoService.eliminar(5, usuario);

        assertThat(existente.isActivo()).isFalse();
        verify(procesoRepository).save(existente);
        verify(historialService).registrar(eq(usuario), eq(existente), eq(AccionHistorial.ELIMINAR), eq("PROCESO"), eq(5), anyString());
    }

    @Test
    @DisplayName("HU-07 CA1 & CA2: Listar procesos con paginación y filtros")
    void listar_parametrosValidos_retornaPagina() {
        Page<Proceso> paginaMock = new PageImpl<>(List.of(new Proceso()));
        when(procesoRepository.buscar(eq(1), eq(true), anyString(), eq(false), any(), anyString(), any(Pageable.class)))
                .thenReturn(paginaMock);

        Page<Proceso> resultado = procesoService.listar(1, "ventas", null, null, true, 0, 10);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("HU-07 CA1: Paginación con valores inválidos lanza ReglaNegocioException")
    void listar_paginacionInvalida_lanzaReglaNegocio() {
        assertThatThrownBy(() -> procesoService.listar(1, "", null, null, true, -1, 10))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Paginacion invalida");

        assertThatThrownBy(() -> procesoService.listar(1, "", null, null, true, 0, 500))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Paginacion invalida");
    }

    @Test
    @DisplayName("HU-07 CA5: Consultar historial del proceso")
    void historial_procesoId_retornaListaHistorial() {
        Proceso proceso = new Proceso();
        proceso.setId(8);
        proceso.setEmpresa(empresa);

        when(procesoRepository.findById(8)).thenReturn(Optional.of(proceso));
        when(historialService.delProceso(8)).thenReturn(List.of(new Historial()));

        List<Historial> hist = procesoService.historial(8, 1);
        assertThat(hist).hasSize(1);
    }

    @Test
    @DisplayName("HU-23 CA4: Buscar para lectura permite consultar proceso compartido en modo solo lectura")
    void buscarParaLectura_procesoCompartido_retornaProceso() {
        Empresa otraEmpresa = new Empresa();
        otraEmpresa.setId(99);

        Proceso proceso = new Proceso();
        proceso.setId(15);
        proceso.setEmpresa(otraEmpresa);
        proceso.setActivo(true);

        when(procesoRepository.findById(15)).thenReturn(Optional.of(proceso));
        when(procesoCompartidoRepository.existsByProcesoIdAndEmpresaIdAndActivoTrue(15, 1)).thenReturn(true);

        Proceso resultado = procesoService.buscarParaLectura(15, 1);
        assertThat(resultado.getId()).isEqualTo(15);
    }
}
