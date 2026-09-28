package desarrollo.web.Bizagi2.historias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.EstadoProceso;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.PoolRepository;
import desarrollo.web.Bizagi2.repository.ProcesoCompartidoRepository;
import desarrollo.web.Bizagi2.repository.ProcesoRepository;
import desarrollo.web.Bizagi2.service.HistorialService;
import desarrollo.web.Bizagi2.service.ProcesoService;
import desarrollo.web.Bizagi2.service.ValidacionService;

@ExtendWith(MockitoExtension.class)
@DisplayName("HU-05: Editar proceso")
class HU05_EditarProcesoTest {

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

    private Usuario editor;
    private Empresa empresa;
    private Proceso procesoExistente;

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(1);
        empresa.setNombre("Empresa Principal");

        editor = new Usuario();
        editor.setId(5);
        editor.setNombre("Marcos");
        editor.setRolAcceso(RolAcceso.EDITOR);
        editor.setEmpresa(empresa);

        procesoExistente = new Proceso();
        procesoExistente.setId(10);
        procesoExistente.setNombre("Proceso Ventas");
        procesoExistente.setDescripcion("Descripcion inicial");
        procesoExistente.setCategoria("Comercial");
        procesoExistente.setEstado(EstadoProceso.BORRADOR);
        procesoExistente.setActivo(true);
        procesoExistente.setEmpresa(empresa);
    }

    @Test
    @DisplayName("CA2 & CA3: Editar nombre, descripción, categoría y registrar cambios en el historial")
    void ca2_ca3_actualizarProceso_exito_conHistorial() {
        when(procesoRepository.findById(10)).thenReturn(Optional.of(procesoExistente));
        when(procesoRepository.existsByEmpresaIdAndActivoTrueAndNombreIgnoreCaseAndIdNot(1, "Proceso Ventas Modificado", 10))
                .thenReturn(false);
        when(procesoRepository.save(any(Proceso.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Proceso datosNuevos = new Proceso();
        datosNuevos.setNombre("Proceso Ventas Modificado");
        datosNuevos.setDescripcion("Nueva descripcion detallada");
        datosNuevos.setCategoria("Comercial y Marketing");

        Proceso resultado = procesoService.actualizar(10, editor, datosNuevos);

        assertThat(resultado.getNombre()).isEqualTo("Proceso Ventas Modificado");
        assertThat(resultado.getDescripcion()).isEqualTo("Nueva descripcion detallada");
        assertThat(resultado.getCategoria()).isEqualTo("Comercial y Marketing");

        verify(historialService).registrar(
                eq(editor),
                eq(procesoExistente),
                eq(AccionHistorial.EDITAR),
                eq("PROCESO"),
                eq(10),
                contains("nombre")
        );
    }

    @Test
    @DisplayName("CA1 & CA5: No se puede editar un proceso inactivo (eliminado lógicamente)")
    void ca1_ca5_actualizarProceso_inactivo_lanzaConflicto() {
        procesoExistente.setActivo(false);
        when(procesoRepository.findById(10)).thenReturn(Optional.of(procesoExistente));

        Proceso datos = new Proceso();
        datos.setNombre("Nuevo Nombre");

        assertThatThrownBy(() -> procesoService.actualizar(10, editor, datos))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("no admite cambios");
    }

    @Test
    @DisplayName("CA4: No se puede editar un proceso de otra empresa (aislamiento)")
    void ca4_actualizarProceso_otraEmpresa_lanzaRecursoNoEncontrado() {
        Empresa otraEmpresa = new Empresa();
        otraEmpresa.setId(99);
        procesoExistente.setEmpresa(otraEmpresa);

        when(procesoRepository.findById(10)).thenReturn(Optional.of(procesoExistente));

        Proceso datos = new Proceso();
        datos.setNombre("Nuevo Nombre");

        assertThatThrownBy(() -> procesoService.actualizar(10, editor, datos))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Proceso no encontrado");
    }

    @Test
    @DisplayName("CA5: Nombre duplicado con otro proceso de la misma empresa lanza ConflictoDominioException")
    void ca5_actualizarProceso_nombreDuplicado_lanzaConflicto() {
        when(procesoRepository.findById(10)).thenReturn(Optional.of(procesoExistente));
        when(procesoRepository.existsByEmpresaIdAndActivoTrueAndNombreIgnoreCaseAndIdNot(1, "Proceso Existente Ya Usado", 10))
                .thenReturn(true);

        Proceso datos = new Proceso();
        datos.setNombre("Proceso Existente Ya Usado");

        assertThatThrownBy(() -> procesoService.actualizar(10, editor, datos))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("Ya existe un proceso con ese nombre en la empresa");
    }

    @Test
    @DisplayName("CA5: Al pasar a PUBLICADO, si el diagrama tiene errores de validación, se rechaza la publicación")
    void ca5_publicarProceso_conErroresDeValidacion_lanzaReglaNegocio() {
        when(procesoRepository.findById(10)).thenReturn(Optional.of(procesoExistente));
        when(procesoRepository.existsByEmpresaIdAndActivoTrueAndNombreIgnoreCaseAndIdNot(1, "Proceso Ventas", 10))
                .thenReturn(false);
        when(validacionService.errores(procesoExistente))
                .thenReturn(List.of("El proceso no tiene evento de fin", "Hay una compuerta divergente sin caminos"));

        Proceso datos = new Proceso();
        datos.setNombre("Proceso Ventas");
        datos.setEstado(EstadoProceso.PUBLICADO);

        assertThatThrownBy(() -> procesoService.actualizar(10, editor, datos))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("No se puede publicar el proceso")
                .hasMessageContaining("El proceso no tiene evento de fin");
    }

    @Test
    @DisplayName("CA5: Al pasar a PUBLICADO con diagrama coherente (sin errores), se actualiza exitosamente")
    void ca5_publicarProceso_sinErrores_publicaExitosamente() {
        when(procesoRepository.findById(10)).thenReturn(Optional.of(procesoExistente));
        when(procesoRepository.existsByEmpresaIdAndActivoTrueAndNombreIgnoreCaseAndIdNot(1, "Proceso Ventas", 10))
                .thenReturn(false);
        when(validacionService.errores(procesoExistente)).thenReturn(Collections.emptyList());
        when(procesoRepository.save(any(Proceso.class))).thenAnswer(i -> i.getArgument(0));

        Proceso datos = new Proceso();
        datos.setNombre("Proceso Ventas");
        datos.setEstado(EstadoProceso.PUBLICADO);

        Proceso resultado = procesoService.actualizar(10, editor, datos);

        assertThat(resultado.getEstado()).isEqualTo(EstadoProceso.PUBLICADO);
        verify(procesoRepository).save(procesoExistente);
    }
}
