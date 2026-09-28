package desarrollo.web.Bizagi2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Actividad;
import desarrollo.web.Bizagi2.entities.Arco;
import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.Evento;
import desarrollo.web.Bizagi2.entities.Gateway;
import desarrollo.web.Bizagi2.entities.Lane;
import desarrollo.web.Bizagi2.entities.Mensaje;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.RolProceso;
import desarrollo.web.Bizagi2.entities.TipoActividad;
import desarrollo.web.Bizagi2.entities.TipoEvento;
import desarrollo.web.Bizagi2.entities.TipoGateway;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.ArcoRepository;
import desarrollo.web.Bizagi2.repository.MensajeRepository;
import desarrollo.web.Bizagi2.repository.NodoFlujoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("NodoFlujoServiceTest - Pruebas basadas en HU-08 a HU-10, HU-14 a HU-16 y HU-27")
class NodoFlujoServiceTest {

    @Mock
    private NodoFlujoRepository nodoFlujoRepository;

    @Mock
    private ArcoRepository arcoRepository;

    @Mock
    private MensajeRepository mensajeRepository;

    @Mock
    private PoolService poolService;

    @Mock
    private LaneService laneService;

    @Mock
    private ProcesoService procesoService;

    @Mock
    private HistorialService historialService;

    @Mock
    private ValidacionService validacionService;

    @InjectMocks
    private NodoFlujoService nodoFlujoService;

    private Usuario usuario;
    private Empresa empresa;
    private Proceso proceso;
    private Pool pool;
    private Lane lane;

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(1);

        usuario = new Usuario();
        usuario.setId(10);
        usuario.setRolAcceso(RolAcceso.EDITOR);
        usuario.setEmpresa(empresa);

        proceso = new Proceso();
        proceso.setId(20);
        proceso.setEmpresa(empresa);

        pool = new Pool();
        pool.setId(30);
        pool.setProceso(proceso);

        lane = new Lane();
        lane.setId(40);
        lane.setPool(pool);
        lane.setNombre("Finanzas");
    }

    // ---------- Actividades (HU-08, HU-09, HU-10) ----------

    @Test
    @DisplayName("HU-08 CA1 & CA2: Crear actividad vinculada a una lane con nombre y tipo")
    void crearActividad_datosValidos_exito() {
        when(laneService.buscarPorId(40, 1)).thenReturn(lane);
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(nodoFlujoRepository.contarActividadesConNombre(20, "Revisar factura", -1)).thenReturn(0L);
        when(nodoFlujoRepository.save(any(Actividad.class))).thenAnswer(i -> {
            Actividad a = i.getArgument(0);
            a.setId(100);
            return a;
        });

        Actividad datos = new Actividad();
        datos.setNombre("Revisar factura");
        datos.setTipo(TipoActividad.USUARIO);

        Actividad resultado = nodoFlujoService.crearActividad(40, usuario, datos);

        assertThat(resultado.getId()).isEqualTo(100);
        assertThat(resultado.getNombre()).isEqualTo("Revisar factura");
        assertThat(resultado.getTipo()).isEqualTo(TipoActividad.USUARIO);
        assertThat(resultado.getLane()).isEqualTo(lane);
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.CREAR), eq("ACTIVIDAD"), eq(100), anyString());
    }

    @Test
    @DisplayName("HU-08 CA3: Nombre de actividad duplicado en el proceso lanza ConflictoDominioException")
    void crearActividad_nombreDuplicado_lanzaConflicto() {
        when(laneService.buscarPorId(40, 1)).thenReturn(lane);
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(nodoFlujoRepository.contarActividadesConNombre(20, "Revisar factura", -1)).thenReturn(1L);

        Actividad datos = new Actividad();
        datos.setNombre("Revisar factura");
        datos.setTipo(TipoActividad.USUARIO);

        assertThatThrownBy(() -> nodoFlujoService.crearActividad(40, usuario, datos))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("Ya existe una actividad con ese nombre en el proceso");
    }

    @Test
    @DisplayName("HU-09 CA1 & CA2: Actualizar actividad y moverla de lane")
    void actualizarActividad_datosValidos_exito() {
        Actividad existente = new Actividad();
        existente.setId(100);
        existente.setNombre("Revisar");
        existente.setPool(pool);
        existente.setLane(lane);

        when(nodoFlujoRepository.findById(100)).thenReturn(Optional.of(existente));
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(nodoFlujoRepository.contarActividadesConNombre(20, "Revisar Documentos", 100)).thenReturn(0L);
        when(nodoFlujoRepository.save(any(Actividad.class))).thenAnswer(i -> i.getArgument(0));

        Actividad cambios = new Actividad();
        cambios.setNombre("Revisar Documentos");

        Actividad actualizada = nodoFlujoService.actualizarActividad(100, usuario, cambios);

        assertThat(actualizada.getNombre()).isEqualTo("Revisar Documentos");
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.EDITAR), eq("ACTIVIDAD"), eq(100), anyString());
    }

    @Test
    @DisplayName("HU-10 CA1 & CA2: Eliminar actividad elimina lógicamente arcos conectados")
    void eliminarActividad_desactivaActividadYArcos() {
        Actividad actividad = new Actividad();
        actividad.setId(100);
        actividad.setNombre("Actividad");
        actividad.setPool(pool);
        actividad.setActivo(true);

        Arco arco = new Arco();
        arco.setId(201);
        arco.setActivo(true);

        when(nodoFlujoRepository.findById(100)).thenReturn(Optional.of(actividad));
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(arcoRepository.findByOrigenIdOrDestinoId(100, 100)).thenReturn(List.of(arco));
        when(mensajeRepository.findByOrigenId(100)).thenReturn(List.of());
        when(validacionService.resultadoEliminacion(eq(proceso), anyInt(), anyInt())).thenReturn(Map.of("advertencias", List.of()));

        Map<String, Object> resultado = nodoFlujoService.eliminarActividad(100, usuario);

        assertThat(actividad.isActivo()).isFalse();
        assertThat(arco.isActivo()).isFalse();
        verify(arcoRepository).save(arco);
        verify(nodoFlujoRepository).save(actividad);
    }

    // ---------- Gateways (HU-14, HU-15, HU-16) ----------

    @Test
    @DisplayName("HU-14 CA1 & CA2: Crear gateway exclusivo, paralelo o inclusivo")
    void crearGateway_datosValidos_exito() {
        when(poolService.buscarPorId(30, 1)).thenReturn(pool);
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(nodoFlujoRepository.save(any(Gateway.class))).thenAnswer(i -> {
            Gateway g = i.getArgument(0);
            g.setId(300);
            return g;
        });

        Gateway datos = new Gateway();
        datos.setNombre("¿Aprobado?");
        datos.setTipoGateway(TipoGateway.EXCLUSIVA);

        Gateway resultado = nodoFlujoService.crearGateway(30, usuario, datos);

        assertThat(resultado.getId()).isEqualTo(300);
        assertThat(resultado.getTipoGateway()).isEqualTo(TipoGateway.EXCLUSIVA);
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.CREAR), eq("GATEWAY"), eq(300), anyString());
    }

    @Test
    @DisplayName("HU-15 CA4: Al pasar gateway a PARALELA se eliminan las condiciones de sus arcos salientes")
    void actualizarGateway_pasaAParalela_eliminaCondicionesDeArcosSalientes() {
        Gateway gateway = new Gateway();
        gateway.setId(300);
        gateway.setNombre("Gateway");
        gateway.setTipoGateway(TipoGateway.EXCLUSIVA);
        gateway.setPool(pool);

        Arco arcoSaliente = new Arco();
        arcoSaliente.setId(501);
        arcoSaliente.setCondicion("monto > 1000");

        when(nodoFlujoRepository.findById(300)).thenReturn(Optional.of(gateway));
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(arcoRepository.findByOrigenId(300)).thenReturn(List.of(arcoSaliente));
        when(nodoFlujoRepository.save(any(Gateway.class))).thenAnswer(i -> i.getArgument(0));

        Gateway datos = new Gateway();
        datos.setNombre("Gateway Paralelo");
        datos.setTipoGateway(TipoGateway.PARALELA);

        Gateway actualizado = nodoFlujoService.actualizarGateway(300, usuario, datos);

        assertThat(actualizado.getTipoGateway()).isEqualTo(TipoGateway.PARALELA);
        assertThat(arcoSaliente.getCondicion()).isNull();
        verify(arcoRepository).save(arcoSaliente);
    }

    // ---------- Eventos (HU-25 y HU-27) ----------

    @Test
    @DisplayName("HU-27 CA5: Un Message Catch de inicio no puede tener arcos entrantes")
    void actualizarEvento_mensajeCatchInicioConArcosEntrantes_lanzaConflicto() {
        Evento evento = new Evento();
        evento.setId(400);
        evento.setNombre("Evento Inicio");
        evento.setTipoEvento(TipoEvento.INICIO);
        evento.setPool(pool);

        when(nodoFlujoRepository.findById(400)).thenReturn(Optional.of(evento));
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(arcoRepository.existsByDestinoId(400)).thenReturn(true);

        Evento datos = new Evento();
        datos.setNombre("Evento Recepción Inicio");
        datos.setTipoEvento(TipoEvento.MENSAJE_RECEPCION_INICIO);

        assertThatThrownBy(() -> nodoFlujoService.actualizarEvento(400, usuario, datos))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("Un Message Catch de inicio no puede tener arcos entrantes");
    }
}
