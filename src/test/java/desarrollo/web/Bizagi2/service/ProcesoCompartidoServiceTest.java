package desarrollo.web.Bizagi2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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
import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.EstadoProceso;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.ProcesoCompartido;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.EmpresaRepository;
import desarrollo.web.Bizagi2.repository.ProcesoCompartidoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcesoCompartidoServiceTest - Pruebas basadas en HU-23")
class ProcesoCompartidoServiceTest {

    @Mock
    private ProcesoCompartidoRepository procesoCompartidoRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private ProcesoService procesoService;

    @Mock
    private HistorialService historialService;

    @InjectMocks
    private ProcesoCompartidoService procesoCompartidoService;

    private Usuario admin;
    private Empresa empresaPropietaria;
    private Empresa empresaInvitada;
    private Proceso proceso;

    @BeforeEach
    void setUp() {
        empresaPropietaria = new Empresa();
        empresaPropietaria.setId(1);
        empresaPropietaria.setNombre("Empresa A");
        empresaPropietaria.setNit("900111");

        empresaInvitada = new Empresa();
        empresaInvitada.setId(2);
        empresaInvitada.setNombre("Empresa B");
        empresaInvitada.setNit("900222");

        admin = new Usuario();
        admin.setId(10);
        admin.setRolAcceso(RolAcceso.ADMINISTRADOR);
        admin.setEmpresa(empresaPropietaria);

        proceso = new Proceso();
        proceso.setId(100);
        proceso.setNombre("Proceso Compartible");
        proceso.setEmpresa(empresaPropietaria);
    }

    @Test
    @DisplayName("HU-23 CA1 & CA3: Compartir proceso con otra empresa por su NIT")
    void compartir_empresaValida_exito() {
        when(procesoService.buscarActivo(100, 1)).thenReturn(proceso);
        when(empresaRepository.findByNit("900222")).thenReturn(Optional.of(empresaInvitada));
        when(procesoCompartidoRepository.findByProcesoIdAndEmpresaId(100, 2)).thenReturn(Optional.empty());
        when(procesoCompartidoRepository.save(any(ProcesoCompartido.class))).thenAnswer(i -> {
            ProcesoCompartido pc = i.getArgument(0);
            pc.setId(5);
            return pc;
        });

        Map<String, Object> resultado = procesoCompartidoService.compartir(100, admin, "900222");

        assertThat(resultado.get("empresaId")).isEqualTo(2);
        assertThat(resultado.get("empresa")).isEqualTo("Empresa B");
        verify(historialService).registrar(eq(admin), eq(proceso), eq(AccionHistorial.COMPARTIR), eq("PROCESO"), eq(100), anyString());
    }

    @Test
    @DisplayName("HU-23 CA1: No se puede compartir un proceso con la propia empresa propietaria")
    void compartir_conMismaEmpresa_lanzaReglaNegocio() {
        when(procesoService.buscarActivo(100, 1)).thenReturn(proceso);
        when(empresaRepository.findByNit("900111")).thenReturn(Optional.of(empresaPropietaria));

        assertThatThrownBy(() -> procesoCompartidoService.compartir(100, admin, "900111"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("El proceso ya es de tu empresa");
    }

    @Test
    @DisplayName("HU-23 CA1: Si ya está compartido con esa empresa y activo, lanza ConflictoDominioException")
    void compartir_yaCompartido_lanzaConflicto() {
        ProcesoCompartido activo = new ProcesoCompartido();
        activo.setId(5);
        activo.setActivo(true);

        when(procesoService.buscarActivo(100, 1)).thenReturn(proceso);
        when(empresaRepository.findByNit("900222")).thenReturn(Optional.of(empresaInvitada));
        when(procesoCompartidoRepository.findByProcesoIdAndEmpresaId(100, 2)).thenReturn(Optional.of(activo));

        assertThatThrownBy(() -> procesoCompartidoService.compartir(100, admin, "900222"))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("El proceso ya esta compartido con esa empresa");
    }

    @Test
    @DisplayName("HU-23: Dejar de compartir desactiva lógicamente la compartición")
    void dejarDeCompartir_procesoCompartido_desactivaLogicamente() {
        ProcesoCompartido pc = new ProcesoCompartido();
        pc.setId(5);
        pc.setEmpresa(empresaInvitada);
        pc.setActivo(true);

        when(procesoService.buscarActivo(100, 1)).thenReturn(proceso);
        when(procesoCompartidoRepository.findByProcesoIdAndEmpresaId(100, 2)).thenReturn(Optional.of(pc));

        procesoCompartidoService.dejarDeCompartir(100, admin, 2);

        assertThat(pc.isActivo()).isFalse();
        verify(procesoCompartidoRepository).save(pc);
        verify(historialService).registrar(eq(admin), eq(proceso), eq(AccionHistorial.DEJAR_DE_COMPARTIR), eq("PROCESO"), eq(100), anyString());
    }

    @Test
    @DisplayName("HU-23 CA4: Listar procesos compartidos conmigo para consulta en solo lectura")
    void compartidosConmigo_retornaProcesosCompartidos() {
        Proceso compartido = new Proceso();
        compartido.setId(88);
        compartido.setNombre("Proceso Externo");
        compartido.setEmpresa(empresaPropietaria);
        compartido.setEstado(EstadoProceso.PUBLICADO);
        compartido.setActivo(true);

        ProcesoCompartido pc = new ProcesoCompartido();
        pc.setProceso(compartido);
        pc.setEmpresa(empresaInvitada);
        pc.setFecha(Instant.now());
        pc.setActivo(true);

        when(procesoCompartidoRepository.findByEmpresaIdAndActivoTrue(2)).thenReturn(List.of(pc));

        List<Map<String, Object>> lista = procesoCompartidoService.compartidosConmigo(2);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).get("procesoId")).isEqualTo(88);
        assertThat(lista.get(0).get("empresaPropietaria")).isEqualTo("Empresa A");
    }
}
