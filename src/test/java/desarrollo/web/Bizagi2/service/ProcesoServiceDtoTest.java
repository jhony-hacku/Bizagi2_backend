package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.DatosDto.EMPRESA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import desarrollo.web.Bizagi2.config.ModelMapperConfig;
import desarrollo.web.Bizagi2.dto.proceso.AdvertenciaResponse;
import desarrollo.web.Bizagi2.dto.proceso.HistorialResponse;
import desarrollo.web.Bizagi2.dto.proceso.ProcesoRequest;
import desarrollo.web.Bizagi2.dto.proceso.ProcesoResponse;
import desarrollo.web.Bizagi2.entities.EstadoProceso;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.repository.PoolRepository;
import desarrollo.web.Bizagi2.repository.ProcesoCompartidoRepository;
import desarrollo.web.Bizagi2.repository.ProcesoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcesoService - conversion entre DTOs y entidades")
class ProcesoServiceDtoTest {

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
    @Spy
    private ModelMapper modelMapper = new ModelMapperConfig().modelMapper();

    @InjectMocks
    private ProcesoService servicio;

    private ProcesoService procesoService;
    private final Usuario usuario = DatosDto.usuario();
    private final Proceso proceso = DatosDto.proceso();

    @BeforeEach
    void setUp() {
        procesoService = spy(servicio);
    }

    @Test
    @DisplayName("listarDto convierte cada proceso de la pagina")
    void listarDto() {
        doReturn(new PageImpl<>(List.of(proceso))).when(procesoService)
                .listar(EMPRESA_ID, "cred", null, null, true, 0, 20);

        Page<ProcesoResponse> pagina = procesoService.listarDto(EMPRESA_ID, "cred", null, null, true, 0, 20);

        assertThat(pagina.getContent()).extracting(ProcesoResponse::getEmpresaId).containsExactly(EMPRESA_ID);
    }

    @Test
    @DisplayName("buscarParaLecturaDto e historialDto devuelven DTOs")
    void consultas() {
        doReturn(proceso).when(procesoService).buscarParaLectura(10, EMPRESA_ID);
        doReturn(List.of(DatosDto.historial())).when(procesoService).historial(10, EMPRESA_ID);

        assertThat(procesoService.buscarParaLecturaDto(10, EMPRESA_ID).getNombre()).isEqualTo("Credito");
        HistorialResponse cambio = procesoService.historialDto(10, EMPRESA_ID).get(0);
        assertThat(cambio.getUsuarioId()).isEqualTo(1);
        assertThat(cambio.getDetalle()).isEqualTo("Proceso modificado");
    }

    @Test
    @DisplayName("validar delega en ValidacionService con el proceso de la empresa")
    void validar() {
        AdvertenciaResponse advertencia = new AdvertenciaResponse();
        doReturn(proceso).when(procesoService).buscarPorId(10, EMPRESA_ID);
        when(validacionService.validar(proceso)).thenReturn(List.of(advertencia));

        assertThat(procesoService.validar(10, EMPRESA_ID)).containsExactly(advertencia);
    }

    @Test
    @DisplayName("crear y actualizar con ProcesoRequest convierten nombre, categoria y estado")
    void crearYActualizar() {
        doReturn(proceso).when(procesoService).crear(same(usuario), any(Proceso.class));
        doReturn(proceso).when(procesoService).actualizar(eq(10), same(usuario), any(Proceso.class));
        ProcesoRequest datos = new ProcesoRequest("Credito", "Aprobacion", "Finanzas", EstadoProceso.BORRADOR);

        procesoService.crear(usuario, datos);
        ProcesoResponse editado = procesoService.actualizar(10, usuario, datos);

        ArgumentCaptor<Proceso> enviado = ArgumentCaptor.forClass(Proceso.class);
        verify(procesoService).crear(same(usuario), enviado.capture());
        assertThat(enviado.getValue().getCategoria()).isEqualTo("Finanzas");
        assertThat(editado.getId()).isEqualTo(10);
    }
}
