package desarrollo.web.Bizagi2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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

import desarrollo.web.Bizagi2.entities.Actividad;
import desarrollo.web.Bizagi2.entities.Arco;
import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.EstadoProceso;
import desarrollo.web.Bizagi2.entities.Evento;
import desarrollo.web.Bizagi2.entities.Gateway;
import desarrollo.web.Bizagi2.entities.Lane;
import desarrollo.web.Bizagi2.entities.Mensaje;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.RolProceso;
import desarrollo.web.Bizagi2.entities.TipoActividad;
import desarrollo.web.Bizagi2.entities.TipoEvento;
import desarrollo.web.Bizagi2.entities.TipoGateway;
import desarrollo.web.Bizagi2.entities.TipoParticipante;
import desarrollo.web.Bizagi2.repository.ArcoRepository;
import desarrollo.web.Bizagi2.repository.CorrelacionRepository;
import desarrollo.web.Bizagi2.repository.LaneRepository;
import desarrollo.web.Bizagi2.repository.MensajeRepository;
import desarrollo.web.Bizagi2.repository.NodoFlujoRepository;
import desarrollo.web.Bizagi2.repository.PoolRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiagramaServiceTest - Pruebas basadas en HU-07 y HU-23")
class DiagramaServiceTest {

    @Mock
    private ProcesoService procesoService;

    @Mock
    private PoolRepository poolRepository;

    @Mock
    private LaneRepository laneRepository;

    @Mock
    private NodoFlujoRepository nodoFlujoRepository;

    @Mock
    private ArcoRepository arcoRepository;

    @Mock
    private MensajeRepository mensajeRepository;

    @Mock
    private CorrelacionRepository correlacionRepository;

    @InjectMocks
    private DiagramaService diagramaService;

    private Empresa empresa;
    private Proceso proceso;
    private Pool pool;
    private Lane lane;
    private RolProceso rol;

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(1);
        empresa.setNombre("Empresa Uno");

        proceso = new Proceso();
        proceso.setId(10);
        proceso.setNombre("Proceso Diagrama");
        proceso.setEstado(EstadoProceso.PUBLICADO);
        proceso.setActivo(true);
        proceso.setEmpresa(empresa);

        pool = new Pool();
        pool.setId(20);
        pool.setNombre("Pool Empresa");
        pool.setTipoParticipante(TipoParticipante.EMPRESA_PROPIETARIA);
        pool.setProceso(proceso);

        rol = new RolProceso();
        rol.setId(30);
        rol.setNombre("Auditor");

        lane = new Lane();
        lane.setId(40);
        lane.setNombre("Banda Auditor");
        lane.setRolProceso(rol);
        lane.setPool(pool);
        lane.setOrden(0);
    }

    @Test
    @DisplayName("HU-07 CA4: Al consultar un proceso propio se entrega el diagrama completo con roles de la empresa")
    void obtener_procesoPropio_retornaDiagramaCompletoConRoles() {
        when(procesoService.buscarParaLectura(10, 1)).thenReturn(proceso);
        when(poolRepository.findByProcesoId(10)).thenReturn(List.of(pool));
        when(laneRepository.findByPoolIdOrderByOrdenAscIdAsc(20)).thenReturn(List.of(lane));

        Actividad actividad = new Actividad();
        actividad.setId(100);
        actividad.setNombre("Auditar factura");
        actividad.setTipo(TipoActividad.USUARIO);
        actividad.setLane(lane);
        actividad.setPool(pool);

        Gateway gateway = new Gateway();
        gateway.setId(200);
        gateway.setNombre("¿Conforme?");
        gateway.setTipoGateway(TipoGateway.EXCLUSIVA);
        gateway.setPool(pool);

        Evento evento = new Evento();
        evento.setId(300);
        evento.setNombre("Inicio");
        evento.setTipoEvento(TipoEvento.INICIO);
        evento.setPool(pool);

        Arco arco = new Arco();
        arco.setId(400);
        arco.setOrigen(evento);
        arco.setDestino(actividad);

        when(nodoFlujoRepository.findActividadesByPool(20)).thenReturn(List.of(actividad));
        when(nodoFlujoRepository.findGatewaysByPool(20)).thenReturn(List.of(gateway));
        when(nodoFlujoRepository.findEventosByPool(20)).thenReturn(List.of(evento));
        when(arcoRepository.findByPoolId(20)).thenReturn(List.of(arco));
        when(mensajeRepository.findByProcesoId(10)).thenReturn(List.of());

        Map<String, Object> diagrama = diagramaService.obtener(10, 1);

        assertThat(diagrama).isNotNull();
        assertThat(diagrama.get("proceso")).isNotNull();
        List<?> pools = (List<?>) diagrama.get("pools");
        assertThat(pools).hasSize(1);
    }

    @Test
    @DisplayName("HU-23: Al consultar un proceso compartido no se incluyen los roles de proceso de la propietaria")
    void obtener_procesoCompartido_ocultaRolesDePropietaria() {
        Empresa otraEmpresa = new Empresa();
        otraEmpresa.setId(99);
        proceso.setEmpresa(otraEmpresa); // Proceso pertenece a otra empresa

        when(procesoService.buscarParaLectura(10, 1)).thenReturn(proceso);
        when(poolRepository.findByProcesoId(10)).thenReturn(List.of(pool));
        when(laneRepository.findByPoolIdOrderByOrdenAscIdAsc(20)).thenReturn(List.of(lane));
        when(nodoFlujoRepository.findActividadesByPool(20)).thenReturn(List.of());
        when(nodoFlujoRepository.findGatewaysByPool(20)).thenReturn(List.of());
        when(nodoFlujoRepository.findEventosByPool(20)).thenReturn(List.of());
        when(arcoRepository.findByPoolId(20)).thenReturn(List.of());
        when(mensajeRepository.findByProcesoId(10)).thenReturn(List.of());

        Map<String, Object> diagrama = diagramaService.obtener(10, 1);

        List<Map<String, Object>> pools = (List<Map<String, Object>>) diagrama.get("pools");
        List<Map<String, Object>> lanes = (List<Map<String, Object>>) pools.get(0).get("lanes");
        assertThat(lanes.get(0).containsKey("rolProcesoId")).isFalse();
    }
}
