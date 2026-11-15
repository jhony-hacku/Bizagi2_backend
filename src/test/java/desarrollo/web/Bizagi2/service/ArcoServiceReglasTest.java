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

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Actividad;
import desarrollo.web.Bizagi2.entities.Arco;
import desarrollo.web.Bizagi2.entities.Evento;
import desarrollo.web.Bizagi2.entities.Gateway;
import desarrollo.web.Bizagi2.entities.NodoFlujo;
import desarrollo.web.Bizagi2.entities.TipoActividad;
import desarrollo.web.Bizagi2.entities.TipoEvento;
import desarrollo.web.Bizagi2.entities.TipoGateway;
import desarrollo.web.Bizagi2.entities.TipoParticipante;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.ArcoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArcoService - HU-12: modificar un arco con las mismas reglas de la creacion")
class ArcoServiceReglasTest {

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
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ArcoService arcoService;

    private final Usuario usuario = DatosDto.usuario();
    private Arco arco;

    @BeforeEach
    void setUp() {
        arco = DatosDto.arco();
    }

    private Arco cambios(Integer origenId, Integer destinoId) {
        Arco datos = new Arco();
        datos.setOrigen(referencia(origenId));
        datos.setDestino(referencia(destinoId));
        return datos;
    }

    private NodoFlujo referencia(Integer id) {
        if (id == null) {
            return null;
        }
        NodoFlujo nodo = new NodoFlujo();
        nodo.setId(id);
        return nodo;
    }

    private void arcoExistente() {
        when(arcoRepository.findById(5)).thenReturn(Optional.of(arco));
        when(procesoService.buscarActivo(10, EMPRESA_ID)).thenReturn(arco.getPool().getProceso());
    }

    private void nodos(NodoFlujo origen, NodoFlujo destino) {
        when(nodoFlujoService.buscarNodo(origen.getId(), EMPRESA_ID)).thenReturn(origen);
        when(nodoFlujoService.buscarNodo(destino.getId(), EMPRESA_ID)).thenReturn(destino);
    }

    @Test
    @DisplayName("listarPorPool verifica el pool y devuelve sus arcos")
    void listarPorPool() {
        when(arcoRepository.findByPoolId(20)).thenReturn(List.of(arco));

        assertThat(arcoService.listarPorPool(20, EMPRESA_ID)).containsExactly(arco);
        verify(poolService).buscarPorId(20, EMPRESA_ID);
    }

    @Test
    @DisplayName("buscarPorId: inexistente o de otra empresa responde no encontrado")
    void buscarPorIdNoEncontrado() {
        when(arcoRepository.findById(99)).thenReturn(Optional.empty());
        when(arcoRepository.findById(5)).thenReturn(Optional.of(arco));

        assertThatThrownBy(() -> arcoService.buscarPorId(99, EMPRESA_ID)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> arcoService.buscarPorId(5, 8)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Cambiar el destino guarda el arco y lo registra en el historial")
    void actualizarDestino() {
        arcoExistente();
        Actividad origen = DatosDto.actividad(50, "Revisar", TipoActividad.TAREA);
        Actividad nuevoDestino = DatosDto.actividad(52, "Notificar", TipoActividad.SERVICIO);
        nodos(origen, nuevoDestino);
        when(arcoRepository.existsByOrigenIdAndDestinoId(50, 52)).thenReturn(false);
        when(arcoRepository.save(any(Arco.class))).thenAnswer(i -> i.getArgument(0));
        Arco datos = cambios(50, 52);
        datos.setEtiqueta("  avisar  ");

        Arco editado = arcoService.actualizar(5, usuario, datos);

        assertThat(editado.getDestino().getId()).isEqualTo(52);
        assertThat(editado.getEtiqueta()).isEqualTo("avisar");
        verify(historialService).registrar(eq(usuario), any(), eq(AccionHistorial.EDITAR), eq("ARCO"), eq(5), anyString());
    }

    @Test
    @DisplayName("Mantener el mismo par origen/destino no revisa duplicados")
    void actualizarMismoPar() {
        arcoExistente();
        nodos(arco.getOrigen(), arco.getDestino());
        when(arcoRepository.save(any(Arco.class))).thenAnswer(i -> i.getArgument(0));

        Arco editado = arcoService.actualizar(5, usuario, cambios(50, 51));

        assertThat(editado.getEtiqueta()).isNull();
    }

    @Test
    @DisplayName("Si ya existe un arco entre los nuevos elementos responde conflicto")
    void actualizarDuplicado() {
        arcoExistente();
        Actividad nuevoDestino = DatosDto.actividad(52, "Notificar", TipoActividad.SERVICIO);
        nodos(arco.getOrigen(), nuevoDestino);
        when(arcoRepository.existsByOrigenIdAndDestinoId(50, 52)).thenReturn(true);

        assertThatThrownBy(() -> arcoService.actualizar(5, usuario, cambios(50, 52)))
                .isInstanceOf(ConflictoDominioException.class);
    }

    @Test
    @DisplayName("Sin origen.id responde error de regla de negocio")
    void actualizarSinOrigen() {
        arcoExistente();

        assertThatThrownBy(() -> arcoService.actualizar(5, usuario, cambios(null, 51)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("origen");
    }

    @Test
    @DisplayName("Un arco no puede cruzar a otro pool")
    void actualizarCruzaPools() {
        arcoExistente();
        Actividad otroPool = DatosDto.actividad(52, "Despachar", TipoActividad.TAREA);
        otroPool.setPool(DatosDto.pool(21, TipoParticipante.EMPRESA_PROPIETARIA));
        nodos(arco.getOrigen(), otroPool);

        assertThatThrownBy(() -> arcoService.actualizar(5, usuario, cambios(50, 52)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("cruzar pools");
    }

    @Test
    @DisplayName("Un arco no puede unir un elemento consigo mismo")
    void actualizarMismoElemento() {
        arcoExistente();
        when(nodoFlujoService.buscarNodo(50, EMPRESA_ID)).thenReturn(arco.getOrigen());

        assertThatThrownBy(() -> arcoService.actualizar(5, usuario, cambios(50, 50)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("consigo mismo");
    }

    @Test
    @DisplayName("HU-27: un Message Catch de inicio no recibe arcos")
    void actualizarHaciaCatchDeInicio() {
        arcoExistente();
        Evento catchInicio = DatosDto.evento(71, "SolicitudRecibida", TipoEvento.MENSAJE_RECEPCION_INICIO);
        nodos(arco.getOrigen(), catchInicio);

        assertThatThrownBy(() -> arcoService.actualizar(5, usuario, cambios(50, 71)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Message Catch");
    }

    @Test
    @DisplayName("Solo los arcos que salen de un gateway llevan condicion")
    void condicionSinGateway() {
        arcoExistente();
        nodos(arco.getOrigen(), arco.getDestino());
        Arco datos = cambios(50, 51);
        datos.setCondicion("monto > 100");

        assertThatThrownBy(() -> arcoService.actualizar(5, usuario, datos))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("gateway");
    }

    @Test
    @DisplayName("Desde un gateway exclusivo se guarda la condicion; desde uno paralelo se descarta")
    void condicionSegunTipoDeGateway() {
        Gateway exclusivo = DatosDto.gateway();
        arco.setOrigen(exclusivo);
        arcoExistente();
        nodos(exclusivo, arco.getDestino());
        when(arcoRepository.save(any(Arco.class))).thenAnswer(i -> i.getArgument(0));
        Arco datos = cambios(60, 51);
        datos.setCondicion("monto > 100");

        assertThat(arcoService.actualizar(5, usuario, datos).getCondicion()).isEqualTo("monto > 100");

        exclusivo.setTipoGateway(TipoGateway.PARALELA);
        assertThat(arcoService.actualizar(5, usuario, datos).getCondicion()).isNull();
    }
}
