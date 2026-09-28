package desarrollo.web.Bizagi2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import desarrollo.web.Bizagi2.entities.Actividad;
import desarrollo.web.Bizagi2.entities.Arco;
import desarrollo.web.Bizagi2.entities.Evento;
import desarrollo.web.Bizagi2.entities.Gateway;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.TipoActividad;
import desarrollo.web.Bizagi2.entities.TipoEvento;
import desarrollo.web.Bizagi2.entities.TipoGateway;
import desarrollo.web.Bizagi2.entities.TipoParticipante;
import desarrollo.web.Bizagi2.repository.ArcoRepository;
import desarrollo.web.Bizagi2.repository.CorrelacionRepository;
import desarrollo.web.Bizagi2.repository.MensajeRepository;
import desarrollo.web.Bizagi2.repository.NodoFlujoRepository;
import desarrollo.web.Bizagi2.repository.PoolRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidacionServiceTest - Pruebas de reglas de consistencia del modelador BPMN")
class ValidacionServiceTest {

    @Mock
    private PoolRepository poolRepository;

    @Mock
    private NodoFlujoRepository nodoFlujoRepository;

    @Mock
    private ArcoRepository arcoRepository;

    @Mock
    private MensajeRepository mensajeRepository;

    @Mock
    private CorrelacionRepository correlacionRepository;

    @InjectMocks
    private ValidacionService validacionService;

    private Proceso proceso;
    private Pool poolEmpresa;

    @BeforeEach
    void setUp() {
        proceso = new Proceso();
        proceso.setId(1);
        proceso.setNombre("Proceso Test");

        poolEmpresa = new Pool();
        poolEmpresa.setId(10);
        poolEmpresa.setProceso(proceso);
        poolEmpresa.setNombre("Empresa");
        poolEmpresa.setTipoParticipante(TipoParticipante.EMPRESA_PROPIETARIA);
    }

    @Test
    @DisplayName("HU-10 & HU-13: Detecta elementos desconectados (sin camino de entrada o salida)")
    void validar_elementosDesconectados_generaAdvertencias() {
        when(poolRepository.findByProcesoId(1)).thenReturn(List.of(poolEmpresa));

        Actividad actividadAislada = new Actividad();
        actividadAislada.setId(101);
        actividadAislada.setNombre("Tarea Desconectada");
        actividadAislada.setTipo(TipoActividad.USUARIO);
        actividadAislada.setPool(poolEmpresa);

        when(nodoFlujoRepository.findByPoolId(10)).thenReturn(List.of(actividadAislada));
        when(arcoRepository.findByPoolId(10)).thenReturn(List.of());
        when(mensajeRepository.findByProcesoId(1)).thenReturn(List.of());

        List<Map<String, Object>> items = validacionService.validar(proceso);

        assertThat(items).isNotEmpty();
        boolean advertenciaEntrada = items.stream().anyMatch(i -> i.get("mensaje").toString().contains("no tiene camino de entrada"));
        boolean advertenciaSalida = items.stream().anyMatch(i -> i.get("mensaje").toString().contains("no tiene camino de salida"));
        assertThat(advertenciaEntrada).isTrue();
        assertThat(advertenciaSalida).isTrue();
    }

    @Test
    @DisplayName("HU-14 CA3 & CA4: Gateway exclusivo con arcos salientes sin condición genera error")
    void validar_gatewayDivergenteSinCondicion_generaErrores() {
        when(poolRepository.findByProcesoId(1)).thenReturn(List.of(poolEmpresa));

        Gateway gateway = new Gateway();
        gateway.setId(200);
        gateway.setNombre("Compuerta Decisión");
        gateway.setTipoGateway(TipoGateway.EXCLUSIVA);
        gateway.setPool(poolEmpresa);

        Actividad entrada = new Actividad();
        entrada.setId(100);
        entrada.setPool(poolEmpresa);

        Actividad salida1 = new Actividad();
        salida1.setId(101);
        salida1.setPool(poolEmpresa);

        Actividad salida2 = new Actividad();
        salida2.setId(102);
        salida2.setPool(poolEmpresa);

        Arco arcoEntrada = new Arco();
        arcoEntrada.setId(501);
        arcoEntrada.setOrigen(entrada);
        arcoEntrada.setDestino(gateway);

        Arco arcoSalida1 = new Arco();
        arcoSalida1.setId(502);
        arcoSalida1.setOrigen(gateway);
        arcoSalida1.setDestino(salida1);
        arcoSalida1.setCondicion(null); // Sin condición

        Arco arcoSalida2 = new Arco();
        arcoSalida2.setId(503);
        arcoSalida2.setOrigen(gateway);
        arcoSalida2.setDestino(salida2);
        arcoSalida2.setCondicion(""); // Vacía

        when(nodoFlujoRepository.findByPoolId(10)).thenReturn(List.of(gateway));
        when(arcoRepository.findByPoolId(10)).thenReturn(List.of(arcoEntrada, arcoSalida1, arcoSalida2));
        when(mensajeRepository.findByProcesoId(1)).thenReturn(List.of());

        List<Map<String, Object>> items = validacionService.validar(proceso);

        assertThat(items).isNotEmpty();
        boolean errorCondicion = items.stream().anyMatch(i -> i.get("mensaje").toString().contains("debe tener una condicion"));
        assertThat(errorCondicion).isTrue();
    }

    @Test
    @DisplayName("HU-14 CA3: Gateway con menos de 2 arcos salientes genera error de divergencia")
    void validar_gatewayConMenosDeDosSalientes_generaError() {
        when(poolRepository.findByProcesoId(1)).thenReturn(List.of(poolEmpresa));

        Gateway gateway = new Gateway();
        gateway.setId(200);
        gateway.setNombre("Compuerta");
        gateway.setTipoGateway(TipoGateway.EXCLUSIVA);
        gateway.setPool(poolEmpresa);

        when(nodoFlujoRepository.findByPoolId(10)).thenReturn(List.of(gateway));
        when(arcoRepository.findByPoolId(10)).thenReturn(List.of());
        when(mensajeRepository.findByProcesoId(1)).thenReturn(List.of());

        List<Map<String, Object>> items = validacionService.validar(proceso);

        boolean errorArcos = items.stream().anyMatch(i -> i.get("mensaje").toString().contains("debe tener al menos dos arcos salientes"));
        assertThat(errorArcos).isTrue();
    }

    @Test
    @DisplayName("HU-27 CA5: Message Catch de inicio con arcos entrantes genera ERROR bloqueante")
    void validar_messageCatchInicioConEntrantes_generaError() {
        when(poolRepository.findByProcesoId(1)).thenReturn(List.of(poolEmpresa));

        Evento mensajeInicio = new Evento();
        mensajeInicio.setId(300);
        mensajeInicio.setNombre("Inicio por Mensaje");
        mensajeInicio.setTipoEvento(TipoEvento.MENSAJE_RECEPCION_INICIO);
        mensajeInicio.setPool(poolEmpresa);

        Actividad origen = new Actividad();
        origen.setId(99);
        origen.setPool(poolEmpresa);

        Arco arcoEntrante = new Arco();
        arcoEntrante.setId(500);
        arcoEntrante.setOrigen(origen);
        arcoEntrante.setDestino(mensajeInicio);

        when(nodoFlujoRepository.findByPoolId(10)).thenReturn(List.of(mensajeInicio));
        when(arcoRepository.findByPoolId(10)).thenReturn(List.of(arcoEntrante));
        when(mensajeRepository.findByProcesoId(1)).thenReturn(List.of());

        List<String> errores = validacionService.errores(proceso);

        assertThat(errores).anyMatch(e -> e.contains("no puede tener arcos entrantes"));
    }
}
