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

import desarrollo.web.Bizagi2.entities.AccionFallo;
import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Actividad;
import desarrollo.web.Bizagi2.entities.Mensaje;
import desarrollo.web.Bizagi2.entities.NodoFlujo;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.TipoActividad;
import desarrollo.web.Bizagi2.entities.TipoDestino;
import desarrollo.web.Bizagi2.entities.TipoEvento;
import desarrollo.web.Bizagi2.entities.TipoParticipante;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.MensajeRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("MensajeService - HU-25 y HU-26: modificar un flujo de mensaje")
class MensajeServiceReglasTest {

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
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private MensajeService mensajeService;

    private final Usuario usuario = DatosDto.usuario();
    private Mensaje mensaje;
    private Proceso proceso;

    @BeforeEach
    void setUp() {
        mensaje = DatosDto.mensaje();
        proceso = mensaje.getProceso();
    }

    private Mensaje cambios(Integer origenId, Integer destinoPoolId) {
        Mensaje datos = new Mensaje();
        datos.setNombre("  PedidoConfirmado  ");
        datos.setContenido("numero:texto; total:numero");
        if (origenId != null) {
            NodoFlujo origen = new NodoFlujo();
            origen.setId(origenId);
            datos.setOrigen(origen);
        }
        if (destinoPoolId != null) {
            Pool destino = new Pool();
            destino.setId(destinoPoolId);
            datos.setDestinoPool(destino);
        }
        return datos;
    }

    private void mensajeExistente() {
        when(mensajeRepository.findById(3)).thenReturn(Optional.of(mensaje));
        when(procesoService.buscarActivo(10, EMPRESA_ID)).thenReturn(proceso);
    }

    private void origenYDestino(NodoFlujo origen, Pool destino) {
        when(nodoFlujoService.buscarNodo(origen.getId(), EMPRESA_ID)).thenReturn(origen);
        when(poolService.buscarPorId(destino.getId(), EMPRESA_ID)).thenReturn(destino);
    }

    @Test
    @DisplayName("listarPorProceso verifica el proceso y devuelve sus mensajes")
    void listarPorProceso() {
        when(mensajeRepository.findByProcesoId(10)).thenReturn(List.of(mensaje));

        assertThat(mensajeService.listarPorProceso(10, EMPRESA_ID)).containsExactly(mensaje);
        verify(procesoService).buscarPorId(10, EMPRESA_ID);
    }

    @Test
    @DisplayName("buscarPorId: inexistente o de otra empresa responde no encontrado")
    void buscarPorIdNoEncontrado() {
        when(mensajeRepository.findById(99)).thenReturn(Optional.empty());
        when(mensajeRepository.findById(3)).thenReturn(Optional.of(mensaje));

        assertThatThrownBy(() -> mensajeService.buscarPorId(99, EMPRESA_ID))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> mensajeService.buscarPorId(3, 8)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Modificar hacia un proveedor: guarda, recorta el nombre y no guarda datos de sistema externo")
    void actualizarHaciaProveedor() {
        mensajeExistente();
        origenYDestino(DatosDto.evento(70, "PedidoConfirmado", TipoEvento.MENSAJE_LANZAMIENTO),
                DatosDto.pool(21, TipoParticipante.PROVEEDOR));
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(i -> i.getArgument(0));
        Mensaje datos = cambios(70, 21);
        datos.setTipoDestino(TipoDestino.CORREO);

        Mensaje editado = mensajeService.actualizar(3, usuario, datos);

        assertThat(editado.getNombre()).isEqualTo("PedidoConfirmado");
        assertThat(editado.getContenido()).isEqualTo("numero:texto; total:numero");
        assertThat(editado.getTipoDestino()).isNull();
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.EDITAR), eq("MENSAJE"),
                eq(3), anyString());
    }

    @Test
    @DisplayName("HU-26: hacia un sistema externo que deriva a manejo de error se guarda la actividad de error")
    void actualizarHaciaSistemaExternoConActividadDeError() {
        mensajeExistente();
        Actividad envio = DatosDto.actividad(56, "Notificar", TipoActividad.ENVIO);
        origenYDestino(envio, DatosDto.pool(22, TipoParticipante.SISTEMA_EXTERNO));
        Actividad manejoError = DatosDto.actividad(55, "Atender fallo", TipoActividad.MANUAL);
        when(nodoFlujoService.buscarActividad(55, EMPRESA_ID)).thenReturn(manejoError);
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(i -> i.getArgument(0));
        Mensaje datos = cambios(56, 22);
        datos.setTipoDestino(TipoDestino.SERVICIO_WEB);
        datos.setMomento("Al aprobar");
        datos.setAccionFallo(AccionFallo.DERIVAR_A_MANEJO_ERROR);
        datos.setActividadError(DatosDto.actividad(55, null, null));

        Mensaje editado = mensajeService.actualizar(3, usuario, datos);

        assertThat(editado.getTipoDestino()).isEqualTo(TipoDestino.SERVICIO_WEB);
        assertThat(editado.getActividadError()).isSameAs(manejoError);
    }

    @Test
    @DisplayName("HU-26: la actividad de error debe ser del mismo proceso")
    void actividadDeErrorDeOtroProceso() {
        mensajeExistente();
        origenYDestino(DatosDto.evento(70, "Aviso", TipoEvento.MENSAJE_LANZAMIENTO),
                DatosDto.pool(22, TipoParticipante.SISTEMA_EXTERNO));
        Actividad deOtroProceso = DatosDto.actividad(55, "Atender fallo", TipoActividad.MANUAL);
        deOtroProceso.getPool().getProceso().setId(99);
        when(nodoFlujoService.buscarActividad(55, EMPRESA_ID)).thenReturn(deOtroProceso);
        Mensaje datos = cambios(70, 22);
        datos.setTipoDestino(TipoDestino.COLA);
        datos.setAccionFallo(AccionFallo.DERIVAR_A_MANEJO_ERROR);
        datos.setActividadError(DatosDto.actividad(55, null, null));

        assertThatThrownBy(() -> mensajeService.actualizar(3, usuario, datos))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("actividad de error");
    }

    @Test
    @DisplayName("Sin origen.id o sin destinoPool.id responde error de regla de negocio")
    void sinOrigenOSinDestino() {
        when(mensajeRepository.findById(3)).thenReturn(Optional.of(mensaje));
        when(procesoService.buscarActivo(10, EMPRESA_ID)).thenReturn(proceso);
        when(nodoFlujoService.buscarNodo(70, EMPRESA_ID))
                .thenReturn(DatosDto.evento(70, "Aviso", TipoEvento.MENSAJE_LANZAMIENTO));

        assertThatThrownBy(() -> mensajeService.actualizar(3, usuario, cambios(null, 21)))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("origen");
        assertThatThrownBy(() -> mensajeService.actualizar(3, usuario, cambios(70, null)))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("destinoPool");
    }

    @Test
    @DisplayName("Solo envian mensajes un Message Throw o una actividad de envio")
    void origenQueNoEnvia() {
        mensajeExistente();
        origenYDestino(DatosDto.actividad(50, "Revisar", TipoActividad.TAREA),
                DatosDto.pool(21, TipoParticipante.PROVEEDOR));

        assertThatThrownBy(() -> mensajeService.actualizar(3, usuario, cambios(50, 21)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("evento de lanzamiento");
    }

    @Test
    @DisplayName("El pool destino debe ser del mismo proceso")
    void destinoDeOtroProceso() {
        mensajeExistente();
        Pool otroProceso = DatosDto.pool(23, TipoParticipante.CLIENTE);
        otroProceso.getProceso().setId(99);
        origenYDestino(DatosDto.evento(70, "Aviso", TipoEvento.MENSAJE_LANZAMIENTO), otroProceso);

        assertThatThrownBy(() -> mensajeService.actualizar(3, usuario, cambios(70, 23)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("pertenecer al proceso");
    }

    @Test
    @DisplayName("HU-25: un mensaje dentro del mismo pool se rechaza")
    void mismoPool() {
        mensajeExistente();
        origenYDestino(DatosDto.evento(70, "Aviso", TipoEvento.MENSAJE_LANZAMIENTO), DatosDto.pool());

        assertThatThrownBy(() -> mensajeService.actualizar(3, usuario, cambios(70, 20)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("cruzar de un pool a otro");
    }

    @Test
    @DisplayName("HU-26: hacia un sistema externo se exige tipoDestino")
    void sistemaExternoSinTipoDestino() {
        mensajeExistente();
        origenYDestino(DatosDto.evento(70, "Aviso", TipoEvento.MENSAJE_LANZAMIENTO),
                DatosDto.pool(22, TipoParticipante.SISTEMA_EXTERNO));

        assertThatThrownBy(() -> mensajeService.actualizar(3, usuario, cambios(70, 22)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("tipoDestino");
    }
}
