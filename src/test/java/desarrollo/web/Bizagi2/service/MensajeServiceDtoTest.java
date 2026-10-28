package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.DatosDto.EMPRESA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

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

import desarrollo.web.Bizagi2.config.ModelMapperConfig;
import desarrollo.web.Bizagi2.dto.mensaje.MensajeRequest;
import desarrollo.web.Bizagi2.dto.mensaje.MensajeResponse;
import desarrollo.web.Bizagi2.entities.AccionFallo;
import desarrollo.web.Bizagi2.entities.Mensaje;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.repository.MensajeRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("MensajeService - conversion entre DTOs y entidades")
class MensajeServiceDtoTest {

    @Mock
    private MensajeRepository mensajeRepository;
    @Mock
    private ProcesoService procesoService;
    @Mock
    private PoolService poolService;
    @Mock
    private NodoFlujoService nodoFlujoService;
    @Mock
    private HistorialService historialService;
    @Spy
    private ModelMapper modelMapper = new ModelMapperConfig().modelMapper();

    @InjectMocks
    private MensajeService servicio;

    private MensajeService mensajeService;
    private final Usuario usuario = DatosDto.usuario();
    private final Mensaje mensaje = DatosDto.mensaje();

    @BeforeEach
    void setUp() {
        mensajeService = spy(servicio);
    }

    private MensajeRequest request() {
        MensajeRequest datos = new MensajeRequest();
        datos.setNombre("PedidoConfirmado");
        datos.setOrigenId(70);
        datos.setDestinoPoolId(21);
        datos.setAccionFallo(AccionFallo.DERIVAR_A_MANEJO_ERROR);
        datos.setActividadErrorId(55);
        return datos;
    }

    @Test
    @DisplayName("aResponse aplana proceso, origen y pool destino")
    void aResponse() {
        MensajeResponse dto = mensajeService.aResponse(mensaje);

        assertThat(dto.getProcesoId()).isEqualTo(10);
        assertThat(dto.getOrigenId()).isEqualTo(70);
        assertThat(dto.getDestinoPoolId()).isEqualTo(21);
        assertThat(dto.getDestinoPoolNombre()).isEqualTo("Proveedor");
    }

    @Test
    @DisplayName("listarPorProcesoDto y buscarPorIdDto devuelven DTOs")
    void consultas() {
        doReturn(List.of(mensaje)).when(mensajeService).listarPorProceso(10, EMPRESA_ID);
        doReturn(mensaje).when(mensajeService).buscarPorId(3, EMPRESA_ID);

        assertThat(mensajeService.listarPorProcesoDto(10, EMPRESA_ID)).extracting(MensajeResponse::getId)
                .containsExactly(3);
        assertThat(mensajeService.buscarPorIdDto(3, EMPRESA_ID).getNombre()).isEqualTo("PedidoConfirmado");
    }

    @Test
    @DisplayName("crear con MensajeRequest convierte origenId, destinoPoolId y actividadErrorId")
    void crear() {
        doReturn(mensaje).when(mensajeService).crear(eq(10), same(usuario), any(Mensaje.class));

        MensajeResponse dto = mensajeService.crear(10, usuario, request());

        ArgumentCaptor<Mensaje> enviado = ArgumentCaptor.forClass(Mensaje.class);
        verify(mensajeService).crear(eq(10), same(usuario), enviado.capture());
        assertThat(enviado.getValue().getOrigen().getId()).isEqualTo(70);
        assertThat(enviado.getValue().getDestinoPool().getId()).isEqualTo(21);
        assertThat(enviado.getValue().getActividadError().getId()).isEqualTo(55);
        assertThat(dto.getId()).isEqualTo(3);
    }

    @Test
    @DisplayName("actualizar con MensajeRequest devuelve el mensaje modificado")
    void actualizar() {
        doReturn(mensaje).when(mensajeService).actualizar(eq(3), same(usuario), any(Mensaje.class));

        assertThat(mensajeService.actualizar(3, usuario, request()).getId()).isEqualTo(3);
    }
}
