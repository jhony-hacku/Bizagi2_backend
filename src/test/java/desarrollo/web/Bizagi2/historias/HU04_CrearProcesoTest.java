package desarrollo.web.Bizagi2.historias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.EstadoProceso;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.TipoParticipante;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.PoolRepository;
import desarrollo.web.Bizagi2.repository.ProcesoCompartidoRepository;
import desarrollo.web.Bizagi2.repository.ProcesoRepository;
import desarrollo.web.Bizagi2.service.HistorialService;
import desarrollo.web.Bizagi2.service.ProcesoService;
import desarrollo.web.Bizagi2.service.ValidacionService;

@ExtendWith(MockitoExtension.class)
@DisplayName("HU-04: Crear proceso")
class HU04_CrearProcesoTest {

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
        empresa.setNombre("Empresa Global");

        usuario = new Usuario();
        usuario.setId(10);
        usuario.setNombre("Ana");
        usuario.setRolAcceso(RolAcceso.EDITOR);
        usuario.setEmpresa(empresa);
    }

    @Test
    @DisplayName("CA1 & CA2 & CA3: Crear proceso registra nombre, descripción, categoría, estado BORRADOR y asociación a la empresa")
    void ca1_ca2_ca3_crearProceso_exito() {
        Proceso datos = new Proceso();
        datos.setNombre("Gestión de Compras");
        datos.setDescripcion("Proceso de compras de insumos");
        datos.setCategoria("Finanzas");

        when(procesoRepository.existsByEmpresaIdAndActivoTrueAndNombreIgnoreCase(1, "Gestión de Compras"))
                .thenReturn(false);

        when(procesoRepository.save(any(Proceso.class))).thenAnswer(invocation -> {
            Proceso p = invocation.getArgument(0);
            p.setId(50);
            return p;
        });

        Proceso resultado = procesoService.crear(usuario, datos);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(50);
        assertThat(resultado.getNombre()).isEqualTo("Gestión de Compras");
        assertThat(resultado.getDescripcion()).isEqualTo("Proceso de compras de insumos");
        assertThat(resultado.getCategoria()).isEqualTo("Finanzas");
        assertThat(resultado.getEstado()).isEqualTo(EstadoProceso.BORRADOR);
        assertThat(resultado.isActivo()).isTrue();
        assertThat(resultado.getEmpresa().getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("CA4: El nombre del proceso debe ser único dentro de la misma empresa")
    void ca4_crearProceso_nombreDuplicado_lanzaConflicto() {
        Proceso datos = new Proceso();
        datos.setNombre("Gestión de Compras");

        when(procesoRepository.existsByEmpresaIdAndActivoTrueAndNombreIgnoreCase(1, "Gestión de Compras"))
                .thenReturn(true);

        assertThatThrownBy(() -> procesoService.crear(usuario, datos))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("Ya existe un proceso con ese nombre en la empresa");
    }

    @Test
    @DisplayName("CA5: Al crear el proceso se crea automáticamente el pool inicial de la empresa y se registra en el historial")
    void ca5_crearProceso_creaPoolEmpresaYRegistraHistorial() {
        Proceso datos = new Proceso();
        datos.setNombre("Facturación Electrónica");

        when(procesoRepository.existsByEmpresaIdAndActivoTrueAndNombreIgnoreCase(1, "Facturación Electrónica"))
                .thenReturn(false);

        when(procesoRepository.save(any(Proceso.class))).thenAnswer(invocation -> {
            Proceso p = invocation.getArgument(0);
            p.setId(55);
            return p;
        });

        Proceso resultado = procesoService.crear(usuario, datos);

        ArgumentCaptor<Pool> poolCaptor = ArgumentCaptor.forClass(Pool.class);
        verify(poolRepository).save(poolCaptor.capture());
        Pool poolCreado = poolCaptor.getValue();
        assertThat(poolCreado.getProceso()).isEqualTo(resultado);
        assertThat(poolCreado.getNombre()).isEqualTo("Empresa Global");
        assertThat(poolCreado.getTipoParticipante()).isEqualTo(TipoParticipante.EMPRESA_PROPIETARIA);

        verify(historialService).registrar(
                eq(usuario),
                eq(resultado),
                eq(AccionHistorial.CREAR),
                eq("PROCESO"),
                eq(55),
                any(String.class)
        );
    }

    @Test
    @DisplayName("CA1: Validación de nombre obligatorio vacío o nulo")
    void ca1_crearProceso_nombreFaltante_lanzaReglaNegocio() {
        Proceso datos = new Proceso();
        datos.setNombre("   ");

        assertThatThrownBy(() -> procesoService.crear(usuario, datos))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("nombre");
    }
}
