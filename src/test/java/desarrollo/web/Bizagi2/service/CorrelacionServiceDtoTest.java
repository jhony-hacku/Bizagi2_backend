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

import desarrollo.web.Bizagi2.config.ModelMapperConfig;
import desarrollo.web.Bizagi2.dto.correlacion.CorrelacionRequest;
import desarrollo.web.Bizagi2.dto.correlacion.CorrelacionResponse;
import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.AccionSinCaso;
import desarrollo.web.Bizagi2.entities.Correlacion;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.repository.CorrelacionRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorrelacionService - DTOs y eliminacion de la clave de correlacion (HU-28)")
class CorrelacionServiceDtoTest {

    @Mock
    private CorrelacionRepository correlacionRepository;
    @Mock
    private MensajeService mensajeService;
    @Mock
    private ProcesoService procesoService;
    @Mock
    private HistorialService historialService;
    @Spy
    private ModelMapper modelMapper = new ModelMapperConfig().modelMapper();

    @InjectMocks
    private CorrelacionService servicio;

    private CorrelacionService correlacionService;
    private final Usuario usuario = DatosDto.usuario();
    private final Correlacion correlacion = DatosDto.correlacion();

    @BeforeEach
    void setUp() {
        correlacionService = spy(servicio);
    }

    @Test
    @DisplayName("aResponse y obtenerDto incluyen el mensajeId")
    void consultas() {
        doReturn(correlacion).when(correlacionService).obtener(3, EMPRESA_ID);

        CorrelacionResponse dto = correlacionService.obtenerDto(3, EMPRESA_ID);

        assertThat(dto.getMensajeId()).isEqualTo(3);
        assertThat(dto.getCriterio()).isEqualTo("numeroPedido");
        assertThat(dto.getAccionSinCaso()).isEqualTo(AccionSinCaso.DESCARTAR);
    }

    @Test
    @DisplayName("crear y actualizar con CorrelacionRequest convierten criterio y accion")
    void crearYActualizar() {
        doReturn(correlacion).when(correlacionService).crear(eq(3), same(usuario), any(Correlacion.class));
        doReturn(correlacion).when(correlacionService).actualizar(eq(3), same(usuario), any(Correlacion.class));
        CorrelacionRequest datos = new CorrelacionRequest("numeroPedido", AccionSinCaso.DESCARTAR);

        correlacionService.crear(3, usuario, datos);
        CorrelacionResponse editada = correlacionService.actualizar(3, usuario, datos);

        ArgumentCaptor<Correlacion> enviada = ArgumentCaptor.forClass(Correlacion.class);
        verify(correlacionService).crear(eq(3), same(usuario), enviada.capture());
        assertThat(enviada.getValue().getCriterio()).isEqualTo("numeroPedido");
        assertThat(editada.getId()).isEqualTo(9);
    }

    @Test
    @DisplayName("eliminar borra la correlacion y lo registra en el historial del proceso")
    void eliminar() {
        Proceso proceso = DatosDto.proceso();
        doReturn(correlacion).when(correlacionService).obtener(3, EMPRESA_ID);
        when(procesoService.buscarActivo(10, EMPRESA_ID)).thenReturn(proceso);

        correlacionService.eliminar(3, usuario);

        verify(correlacionRepository).delete(correlacion);
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.ELIMINAR),
                eq("CORRELACION"), eq(9), any(String.class));
    }
}
