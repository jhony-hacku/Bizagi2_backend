package desarrollo.web.Bizagi2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import desarrollo.web.Bizagi2.entities.NodoFlujo;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.RolAcceso;
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
@DisplayName("NodoFlujoServiceTest - Pruebas basadas en HU-08 a HU-10, HU-14 a HU-16 y HU-25 a HU-27")
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

    // =====================================================================
    // Helpers
    // =====================================================================

    /** Nodo que pertenece al pool/proceso/empresa del setUp. */
    private <T extends NodoFlujo> T enMiPool(T nodo, Integer id) {
        nodo.setId(id);
        nodo.setPool(pool);
        return nodo;
    }

    /** Nodo que pertenece a OTRA empresa (aislamiento multiempresa). */
    private <T extends NodoFlujo> T deOtraEmpresa(T nodo, Integer id) {
        Empresa otra = new Empresa();
        otra.setId(2);
        Proceso procesoAjeno = new Proceso();
        procesoAjeno.setId(77);
        procesoAjeno.setEmpresa(otra);
        Pool poolAjeno = new Pool();
        poolAjeno.setId(88);
        poolAjeno.setProceso(procesoAjeno);
        nodo.setId(id);
        nodo.setPool(poolAjeno);
        return nodo;
    }

    private Evento datosEvento(TipoEvento tipo) {
        Evento datos = new Evento();
        datos.setNombre("Evento");
        datos.setTipoEvento(tipo);
        return datos;
    }

    private Evento eventoExistente(TipoEvento tipo) {
        Evento evento = enMiPool(new Evento(), 400);
        evento.setNombre("Evento");
        evento.setTipoEvento(tipo);
        return evento;
    }

    /** Actividad de un proceso distinto al del setUp, pero de la misma empresa. */
    private Actividad actividadDeOtroProceso(Integer id) {
        Proceso otroProceso = new Proceso();
        otroProceso.setId(99);
        otroProceso.setEmpresa(empresa);
        Pool otroPool = new Pool();
        otroPool.setId(31);
        otroPool.setProceso(otroProceso);
        Actividad actividad = new Actividad();
        actividad.setId(id);
        actividad.setPool(otroPool);
        return actividad;
    }

    private void stubCrearEvento() {
        when(poolService.buscarPorId(30, 1)).thenReturn(pool);
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        lenient().when(nodoFlujoRepository.save(any(Evento.class))).thenAnswer(i -> {
            Evento e = i.getArgument(0);
            e.setId(400);
            return e;
        });
    }

    private void stubActualizarEvento(Evento existente) {
        when(nodoFlujoRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        lenient().when(nodoFlujoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
    }

    // =====================================================================
    // Consultas: listar*
    // =====================================================================

    @Test
    @DisplayName("listarActividades: valida el pool de la empresa y devuelve las actividades del pool")
    void listarActividades_poolDeLaEmpresa_devuelveActividadesDelPool() {
        Actividad a = enMiPool(new Actividad(), 100);
        when(nodoFlujoRepository.findActividadesByPool(30)).thenReturn(List.of(a));

        List<Actividad> resultado = nodoFlujoService.listarActividades(30, 1);

        assertThat(resultado).containsExactly(a);
        verify(poolService).buscarPorId(30, 1);
    }

    @Test
    @DisplayName("listarGateways: valida el pool de la empresa y devuelve los gateways del pool")
    void listarGateways_poolDeLaEmpresa_devuelveGatewaysDelPool() {
        Gateway g = enMiPool(new Gateway(), 300);
        when(nodoFlujoRepository.findGatewaysByPool(30)).thenReturn(List.of(g));

        List<Gateway> resultado = nodoFlujoService.listarGateways(30, 1);

        assertThat(resultado).containsExactly(g);
        verify(poolService).buscarPorId(30, 1);
    }

    @Test
    @DisplayName("listarEventos: valida el pool de la empresa y devuelve los eventos del pool")
    void listarEventos_poolDeLaEmpresa_devuelveEventosDelPool() {
        Evento e = eventoExistente(TipoEvento.INICIO);
        when(nodoFlujoRepository.findEventosByPool(30)).thenReturn(List.of(e));

        List<Evento> resultado = nodoFlujoService.listarEventos(30, 1);

        assertThat(resultado).containsExactly(e);
        verify(poolService).buscarPorId(30, 1);
    }

    @Test
    @DisplayName("listarEventos: pool de otra empresa propaga la excepcion y no consulta el repositorio")
    void listarEventos_poolDeOtraEmpresa_lanzaNoEncontradoYNoConsulta() {
        when(poolService.buscarPorId(30, 2)).thenThrow(new RecursoNoEncontradoException("Pool no encontrado"));

        assertThatThrownBy(() -> nodoFlujoService.listarEventos(30, 2))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(nodoFlujoRepository, never()).findEventosByPool(anyInt());
    }

    // =====================================================================
    // Consultas: buscar* (existencia, tipo correcto y aislamiento entre empresas)
    // =====================================================================

    @Test
    @DisplayName("buscarNodo: existe y es de la empresa -> lo devuelve")
    void buscarNodo_existeYEsDeLaEmpresa_lodevuelve() {
        Gateway g = enMiPool(new Gateway(), 300);
        when(nodoFlujoRepository.findById(300)).thenReturn(Optional.of(g));

        assertThat(nodoFlujoService.buscarNodo(300, 1)).isSameAs(g);
    }

    @Test
    @DisplayName("buscarNodo: no existe -> RecursoNoEncontradoException")
    void buscarNodo_noExiste_lanzaNoEncontrado() {
        when(nodoFlujoRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> nodoFlujoService.buscarNodo(999, 1))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Elemento no encontrado");
    }

    @Test
    @DisplayName("buscarNodo: nodo de OTRA empresa -> mismo error que si no existiera (aislamiento)")
    void buscarNodo_deOtraEmpresa_lanzaNoEncontrado() {
        Gateway ajeno = deOtraEmpresa(new Gateway(), 300);
        when(nodoFlujoRepository.findById(300)).thenReturn(Optional.of(ajeno));

        assertThatThrownBy(() -> nodoFlujoService.buscarNodo(300, 1))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Elemento no encontrado");
    }

    @Test
    @DisplayName("buscarActividad: el nodo es una Actividad -> la devuelve")
    void buscarActividad_esActividad_laDevuelve() {
        Actividad a = enMiPool(new Actividad(), 100);
        when(nodoFlujoRepository.findById(100)).thenReturn(Optional.of(a));

        assertThat(nodoFlujoService.buscarActividad(100, 1)).isSameAs(a);
    }

    @Test
    @DisplayName("buscarActividad: el nodo existe pero es un Gateway -> 'Actividad no encontrada'")
    void buscarActividad_esGateway_lanzaNoEncontrado() {
        when(nodoFlujoRepository.findById(300)).thenReturn(Optional.of(enMiPool(new Gateway(), 300)));

        assertThatThrownBy(() -> nodoFlujoService.buscarActividad(300, 1))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Actividad no encontrada");
    }

    @Test
    @DisplayName("buscarGateway: el nodo es un Gateway -> lo devuelve")
    void buscarGateway_esGateway_loDevuelve() {
        Gateway g = enMiPool(new Gateway(), 300);
        when(nodoFlujoRepository.findById(300)).thenReturn(Optional.of(g));

        assertThat(nodoFlujoService.buscarGateway(300, 1)).isSameAs(g);
    }

    @Test
    @DisplayName("buscarGateway: el nodo existe pero es una Actividad -> 'Gateway no encontrado'")
    void buscarGateway_esActividad_lanzaNoEncontrado() {
        when(nodoFlujoRepository.findById(100)).thenReturn(Optional.of(enMiPool(new Actividad(), 100)));

        assertThatThrownBy(() -> nodoFlujoService.buscarGateway(100, 1))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Gateway no encontrado");
    }

    @Test
    @DisplayName("buscarEvento: el nodo es un Evento -> lo devuelve")
    void buscarEvento_esEvento_loDevuelve() {
        Evento e = eventoExistente(TipoEvento.INICIO);
        when(nodoFlujoRepository.findById(400)).thenReturn(Optional.of(e));

        assertThat(nodoFlujoService.buscarEvento(400, 1)).isSameAs(e);
    }

    @Test
    @DisplayName("buscarEvento: el nodo existe pero es una Actividad -> 'Evento no encontrado'")
    void buscarEvento_esActividad_lanzaNoEncontrado() {
        when(nodoFlujoRepository.findById(100)).thenReturn(Optional.of(enMiPool(new Actividad(), 100)));

        assertThatThrownBy(() -> nodoFlujoService.buscarEvento(100, 1))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Evento no encontrado");
    }

    // =====================================================================
    // Actividades (HU-08, HU-09, HU-10)
    // =====================================================================

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
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.CREAR), eq("ACTIVIDAD"),
                eq(100), anyString());
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

        verify(nodoFlujoRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-09 CA1 & CA2: Actualizar actividad y moverla de lane")
    void actualizarActividad_datosValidos_exito() {
        Actividad existente = enMiPool(new Actividad(), 100);
        existente.setNombre("Revisar");
        existente.setLane(lane);

        when(nodoFlujoRepository.findById(100)).thenReturn(Optional.of(existente));
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(nodoFlujoRepository.contarActividadesConNombre(20, "Revisar Documentos", 100)).thenReturn(0L);
        when(nodoFlujoRepository.save(any(Actividad.class))).thenAnswer(i -> i.getArgument(0));

        Actividad cambios = new Actividad();
        cambios.setNombre("  Revisar Documentos  ");

        Actividad actualizada = nodoFlujoService.actualizarActividad(100, usuario, cambios);

        assertThat(actualizada.getNombre()).isEqualTo("Revisar Documentos");
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.EDITAR), eq("ACTIVIDAD"),
                eq(100), anyString());
    }

    private void stubActualizarActividad(Actividad existente) {
        when(nodoFlujoRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        lenient().when(nodoFlujoRepository.save(any(Actividad.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("HU-09: actualizarActividad mueve la actividad a otra lane del MISMO pool")
    void actualizarActividad_laneDelMismoPool_cambiaLane() {
        Actividad existente = enMiPool(new Actividad(), 100);
        existente.setLane(lane);
        stubActualizarActividad(existente);

        Lane destino = new Lane();
        destino.setId(41);
        destino.setPool(pool);
        when(laneService.buscarPorId(41, 1)).thenReturn(destino);

        Actividad cambios = new Actividad();
        cambios.setNombre("Revisar");
        Lane referencia = new Lane();
        referencia.setId(41);
        cambios.setLane(referencia);

        Actividad resultado = nodoFlujoService.actualizarActividad(100, usuario, cambios);

        assertThat(resultado.getLane()).isSameAs(destino);
    }

    @Test
    @DisplayName("HU-09: actualizarActividad con lane de OTRO pool -> ReglaNegocioException y no guarda")
    void actualizarActividad_laneDeOtroPool_lanzaReglaNegocio() {
        Actividad existente = enMiPool(new Actividad(), 100);
        existente.setLane(lane);
        stubActualizarActividad(existente);

        Pool otroPool = new Pool();
        otroPool.setId(31);
        otroPool.setProceso(proceso);
        Lane laneAjena = new Lane();
        laneAjena.setId(41);
        laneAjena.setPool(otroPool);
        when(laneService.buscarPorId(41, 1)).thenReturn(laneAjena);

        Actividad cambios = new Actividad();
        cambios.setNombre("Revisar");
        Lane referencia = new Lane();
        referencia.setId(41);
        cambios.setLane(referencia);

        assertThatThrownBy(() -> nodoFlujoService.actualizarActividad(100, usuario, cambios))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La lane debe pertenecer al mismo pool de la actividad");

        assertThat(existente.getLane()).isSameAs(lane);
        verify(nodoFlujoRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-09: actualizarActividad con lane sin id no consulta laneService ni cambia la lane")
    void actualizarActividad_laneSinId_noCambiaLane() {
        Actividad existente = enMiPool(new Actividad(), 100);
        existente.setLane(lane);
        stubActualizarActividad(existente);

        Actividad cambios = new Actividad();
        cambios.setNombre("Revisar");
        cambios.setLane(new Lane());

        Actividad resultado = nodoFlujoService.actualizarActividad(100, usuario, cambios);

        assertThat(resultado.getLane()).isSameAs(lane);
        verify(laneService, never()).buscarPorId(any(), any());
    }

    @Test
    @DisplayName("HU-09: actualizarActividad cambia el tipo cuando no envia mensajes")
    void actualizarActividad_cambioDeTipoSinMensajes_cambiaTipo() {
        Actividad existente = enMiPool(new Actividad(), 100);
        existente.setTipo(TipoActividad.ENVIO);
        stubActualizarActividad(existente);
        when(mensajeRepository.findByOrigenId(100)).thenReturn(List.of());

        Actividad cambios = new Actividad();
        cambios.setNombre("Revisar");
        cambios.setTipo(TipoActividad.USUARIO);

        Actividad resultado = nodoFlujoService.actualizarActividad(100, usuario, cambios);

        assertThat(resultado.getTipo()).isEqualTo(TipoActividad.USUARIO);
    }

    @Test
    @DisplayName("HU-09: actividad que envia mensajes no puede dejar de ser ENVIO")
    void actualizarActividad_cambioDeTipoConMensajes_lanzaConflicto() {
        Actividad existente = enMiPool(new Actividad(), 100);
        existente.setTipo(TipoActividad.ENVIO);
        stubActualizarActividad(existente);
        when(mensajeRepository.findByOrigenId(100)).thenReturn(List.of(new Mensaje()));

        Actividad cambios = new Actividad();
        cambios.setNombre("Revisar");
        cambios.setTipo(TipoActividad.USUARIO);

        assertThatThrownBy(() -> nodoFlujoService.actualizarActividad(100, usuario, cambios))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("debe seguir siendo de tipo ENVIO");

        assertThat(existente.getTipo()).isEqualTo(TipoActividad.ENVIO);
        verify(nodoFlujoRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-09: actividad que envia mensajes puede seguir siendo ENVIO (no consulta mensajes)")
    void actualizarActividad_tipoEnvioConMensajes_permitido() {
        Actividad existente = enMiPool(new Actividad(), 100);
        existente.setTipo(TipoActividad.ENVIO);
        stubActualizarActividad(existente);

        Actividad cambios = new Actividad();
        cambios.setNombre("Enviar factura");
        cambios.setTipo(TipoActividad.ENVIO);

        Actividad resultado = nodoFlujoService.actualizarActividad(100, usuario, cambios);

        assertThat(resultado.getTipo()).isEqualTo(TipoActividad.ENVIO);
        verify(mensajeRepository, never()).findByOrigenId(anyInt());
    }

    @Test
    @DisplayName("HU-08 CA3: actualizarActividad con nombre ya usado por otra actividad -> Conflicto y no guarda")
    void actualizarActividad_nombreDuplicado_lanzaConflicto() {
        Actividad existente = enMiPool(new Actividad(), 100);
        stubActualizarActividad(existente);
        when(nodoFlujoRepository.contarActividadesConNombre(20, "Duplicada", 100)).thenReturn(1L);

        Actividad cambios = new Actividad();
        cambios.setNombre("Duplicada");

        assertThatThrownBy(() -> nodoFlujoService.actualizarActividad(100, usuario, cambios))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessage("Ya existe una actividad con ese nombre en el proceso");

        verify(nodoFlujoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Aislamiento: actualizarActividad de OTRA empresa -> RecursoNoEncontradoException")
    void actualizarActividad_deOtraEmpresa_lanzaNoEncontrado() {
        when(nodoFlujoRepository.findById(100)).thenReturn(Optional.of(deOtraEmpresa(new Actividad(), 100)));

        Actividad cambios = new Actividad();
        cambios.setNombre("Revisar");

        assertThatThrownBy(() -> nodoFlujoService.actualizarActividad(100, usuario, cambios))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(nodoFlujoRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-10 CA1 & CA2: Eliminar actividad elimina lógicamente arcos conectados")
    void eliminarActividad_desactivaActividadYArcos() {
        Actividad actividad = enMiPool(new Actividad(), 100);
        actividad.setNombre("Actividad");
        actividad.setActivo(true);

        Arco arco = new Arco();
        arco.setId(201);
        arco.setActivo(true);

        Map<String, Object> esperado = Map.of("advertencias", List.of());

        when(nodoFlujoRepository.findById(100)).thenReturn(Optional.of(actividad));
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(arcoRepository.findByOrigenIdOrDestinoId(100, 100)).thenReturn(List.of(arco));
        when(mensajeRepository.findByOrigenId(100)).thenReturn(List.of());
        when(validacionService.resultadoEliminacion(proceso, 1, 0)).thenReturn(esperado);

        Map<String, Object> resultado = nodoFlujoService.eliminarActividad(100, usuario);

        assertThat(resultado).isSameAs(esperado);
        assertThat(actividad.isActivo()).isFalse();
        assertThat(arco.isActivo()).isFalse();
        verify(arcoRepository).save(arco);
        verify(nodoFlujoRepository).save(actividad);
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.ELIMINAR), eq("ARCO"), eq(201),
                anyString());
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.ELIMINAR), eq("ACTIVIDAD"),
                eq(100), anyString());
    }

    // =====================================================================
    // Gateways (HU-14, HU-15, HU-16)
    // =====================================================================

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
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.CREAR), eq("GATEWAY"), eq(300),
                anyString());
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

    @Test
    @DisplayName("HU-15: gateway que ya era PARALELA y sigue PARALELA no toca los arcos")
    void actualizarGateway_yaEraParalela_noTocaArcos() {
        Gateway gateway = enMiPool(new Gateway(), 300);
        gateway.setTipoGateway(TipoGateway.PARALELA);
        when(nodoFlujoRepository.findById(300)).thenReturn(Optional.of(gateway));
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(nodoFlujoRepository.save(any(Gateway.class))).thenAnswer(i -> i.getArgument(0));

        Gateway datos = new Gateway();
        datos.setNombre("Renombrado");
        datos.setTipoGateway(TipoGateway.PARALELA);

        Gateway resultado = nodoFlujoService.actualizarGateway(300, usuario, datos);

        assertThat(resultado.getNombre()).isEqualTo("Renombrado");
        verify(arcoRepository, never()).findByOrigenId(anyInt());
        verify(arcoRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-15: pasar de PARALELA a EXCLUSIVA no borra condiciones ni toca arcos")
    void actualizarGateway_deParalelaAExclusiva_noTocaArcos() {
        Gateway gateway = enMiPool(new Gateway(), 300);
        gateway.setTipoGateway(TipoGateway.PARALELA);
        when(nodoFlujoRepository.findById(300)).thenReturn(Optional.of(gateway));
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(nodoFlujoRepository.save(any(Gateway.class))).thenAnswer(i -> i.getArgument(0));

        Gateway datos = new Gateway();
        datos.setNombre("Gateway");
        datos.setTipoGateway(TipoGateway.EXCLUSIVA);

        Gateway resultado = nodoFlujoService.actualizarGateway(300, usuario, datos);

        assertThat(resultado.getTipoGateway()).isEqualTo(TipoGateway.EXCLUSIVA);
        verify(arcoRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-16: eliminarGateway desactiva el gateway y registra ELIMINAR 'GATEWAY'")
    void eliminarGateway_desactivaGatewayYRegistraHistorial() {
        Gateway gateway = enMiPool(new Gateway(), 300);
        gateway.setNombre("Gateway");
        gateway.setActivo(true);
        Map<String, Object> esperado = Map.of("advertencias", List.of());

        when(nodoFlujoRepository.findById(300)).thenReturn(Optional.of(gateway));
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(arcoRepository.findByOrigenIdOrDestinoId(300, 300)).thenReturn(List.of());
        when(mensajeRepository.findByOrigenId(300)).thenReturn(List.of());
        when(validacionService.resultadoEliminacion(proceso, 0, 0)).thenReturn(esperado);

        Map<String, Object> resultado = nodoFlujoService.eliminarGateway(300, usuario);

        assertThat(resultado).isSameAs(esperado);
        assertThat(gateway.isActivo()).isFalse();
        verify(nodoFlujoRepository).save(gateway);
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.ELIMINAR), eq("GATEWAY"),
                eq(300), anyString());
    }

    // =====================================================================
    // Eventos: crearEvento y copiarDatosDeMensaje (HU-11, HU-25, HU-26, HU-27)
    // =====================================================================

    @Test
    @DisplayName("HU-11: crearEvento guarda el evento en el pool con nombre sin espacios y registra historial")
    void crearEvento_datosValidos_creaYRegistraHistorial() {
        stubCrearEvento();
        Evento datos = datosEvento(TipoEvento.INICIO);
        datos.setNombre("  Inicio  ");

        Evento resultado = nodoFlujoService.crearEvento(30, usuario, datos);

        assertThat(resultado.getId()).isEqualTo(400);
        assertThat(resultado.getNombre()).isEqualTo("Inicio");
        assertThat(resultado.getTipoEvento()).isEqualTo(TipoEvento.INICIO);
        assertThat(resultado.getPool()).isSameAs(pool);
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.CREAR), eq("EVENTO"), eq(400),
                anyString());
    }

    @Test
    @DisplayName("HU-21: crearEvento en un pool externo (caja negra) se rechaza y no se guarda")
    void crearEvento_poolExterno_propagaReglaNegocioYNoGuarda() {
        when(poolService.buscarPorId(30, 1)).thenReturn(pool);
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        doThrow(new ReglaNegocioException("El pool externo es una caja negra"))
                .when(poolService).verificarInterno(pool);

        assertThatThrownBy(() -> nodoFlujoService.crearEvento(30, usuario, datosEvento(TipoEvento.INICIO)))
                .isInstanceOf(ReglaNegocioException.class);

        verify(nodoFlujoRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = TipoEvento.class, mode = EnumSource.Mode.EXCLUDE, names = { "MENSAJE_LANZAMIENTO",
            "MENSAJE_RECEPCION_INICIO", "MENSAJE_RECEPCION_INTERMEDIO" })
    @DisplayName("HU-25: eventos que NO son de mensaje descartan contenido, correlacion, origen externo y actividades")
    void crearEvento_tipoSinMensaje_descartaDatosDeMensaje(TipoEvento tipo) {
        stubCrearEvento();
        Evento datos = datosEvento(tipo);
        datos.setContenido("contenido");
        datos.setClaveCorrelacion("clave");
        datos.setOrigenExterno(true);
        datos.setActividadesUsuarias(Set.of(500));

        Evento resultado = nodoFlujoService.crearEvento(30, usuario, datos);

        assertThat(resultado.getContenido()).isNull();
        assertThat(resultado.getClaveCorrelacion()).isNull();
        assertThat(resultado.getOrigenExterno()).isNull();
        assertThat(resultado.getActividadesUsuarias()).isEmpty();
        verify(nodoFlujoRepository, never()).findById(anyInt());
    }

    @Test
    @DisplayName("HU-25: MENSAJE_LANZAMIENTO conserva contenido y clave, sin origen externo ni actividades")
    void crearEvento_mensajeLanzamiento_copiaContenidoYClaveSinOrigenNiActividades() {
        stubCrearEvento();
        Evento datos = datosEvento(TipoEvento.MENSAJE_LANZAMIENTO);
        datos.setContenido("Factura enviada");
        datos.setClaveCorrelacion("nroFactura");
        datos.setOrigenExterno(true);
        datos.setActividadesUsuarias(Set.of(500));

        Evento resultado = nodoFlujoService.crearEvento(30, usuario, datos);

        assertThat(resultado.getContenido()).isEqualTo("Factura enviada");
        assertThat(resultado.getClaveCorrelacion()).isEqualTo("nroFactura");
        assertThat(resultado.getOrigenExterno()).isNull();
        assertThat(resultado.getActividadesUsuarias()).isEmpty();
        verify(nodoFlujoRepository, never()).findById(anyInt());
    }

    @Test
    @DisplayName("HU-27: MENSAJE_RECEPCION_INICIO conserva origen externo=true y contenido")
    void crearEvento_recepcionInicioOrigenExterno_copiaOrigenExternoTrue() {
        stubCrearEvento();
        Evento datos = datosEvento(TipoEvento.MENSAJE_RECEPCION_INICIO);
        datos.setContenido("Solicitud");
        datos.setClaveCorrelacion("nroCaso");
        datos.setOrigenExterno(true);

        Evento resultado = nodoFlujoService.crearEvento(30, usuario, datos);

        assertThat(resultado.getContenido()).isEqualTo("Solicitud");
        assertThat(resultado.getClaveCorrelacion()).isEqualTo("nroCaso");
        assertThat(resultado.getOrigenExterno()).isTrue();
    }

    @Test
    @DisplayName("HU-26: recepcion con origenExterno nulo se normaliza a FALSE (no queda nulo)")
    void crearEvento_recepcionSinOrigenExterno_normalizaAFalse() {
        stubCrearEvento();
        Evento datos = datosEvento(TipoEvento.MENSAJE_RECEPCION_INTERMEDIO);
        datos.setOrigenExterno(null);

        Evento resultado = nodoFlujoService.crearEvento(30, usuario, datos);

        assertThat(resultado.getOrigenExterno()).isFalse();
    }

    @Test
    @DisplayName("HU-26: recepcion sin lista de actividades usuarias deja el conjunto vacio")
    void crearEvento_recepcionConActividadesNulas_dejaConjuntoVacio() {
        stubCrearEvento();
        Evento datos = datosEvento(TipoEvento.MENSAJE_RECEPCION_INTERMEDIO);
        datos.setActividadesUsuarias(null);

        Evento resultado = nodoFlujoService.crearEvento(30, usuario, datos);

        assertThat(resultado.getActividadesUsuarias()).isEmpty();
    }

    @Test
    @DisplayName("HU-26: recepcion con actividades del mismo proceso las asocia al evento")
    void crearEvento_recepcionConActividadesDelProceso_lasAsocia() {
        stubCrearEvento();
        Actividad a1 = enMiPool(new Actividad(), 500);
        Actividad a2 = enMiPool(new Actividad(), 501);
        when(nodoFlujoRepository.findById(500)).thenReturn(Optional.of(a1));
        when(nodoFlujoRepository.findById(501)).thenReturn(Optional.of(a2));

        Evento datos = datosEvento(TipoEvento.MENSAJE_RECEPCION_INTERMEDIO);
        datos.setActividadesUsuarias(Set.of(500, 501));

        Evento resultado = nodoFlujoService.crearEvento(30, usuario, datos);

        assertThat(resultado.getActividadesUsuarias()).containsExactlyInAnyOrder(500, 501);
    }

    @Test
    @DisplayName("HU-26: actividad usuaria de OTRO proceso -> ReglaNegocioException y no guarda")
    void crearEvento_actividadDeOtroProceso_lanzaReglaNegocio() {
        stubCrearEvento();
        when(nodoFlujoRepository.findById(500)).thenReturn(Optional.of(actividadDeOtroProceso(500)));

        Evento datos = datosEvento(TipoEvento.MENSAJE_RECEPCION_INTERMEDIO);
        datos.setActividadesUsuarias(Set.of(500));

        assertThatThrownBy(() -> nodoFlujoService.crearEvento(30, usuario, datos))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La actividad 500 no es de este proceso");

        verify(nodoFlujoRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-26: actividad usuaria de OTRA empresa -> RecursoNoEncontradoException (aislamiento)")
    void crearEvento_actividadDeOtraEmpresa_lanzaNoEncontrado() {
        stubCrearEvento();
        when(nodoFlujoRepository.findById(500)).thenReturn(Optional.of(deOtraEmpresa(new Actividad(), 500)));

        Evento datos = datosEvento(TipoEvento.MENSAJE_RECEPCION_INTERMEDIO);
        datos.setActividadesUsuarias(Set.of(500));

        assertThatThrownBy(() -> nodoFlujoService.crearEvento(30, usuario, datos))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(nodoFlujoRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-26: actividad usuaria inexistente -> RecursoNoEncontradoException")
    void crearEvento_actividadInexistente_lanzaNoEncontrado() {
        stubCrearEvento();
        when(nodoFlujoRepository.findById(500)).thenReturn(Optional.empty());

        Evento datos = datosEvento(TipoEvento.MENSAJE_RECEPCION_INTERMEDIO);
        datos.setActividadesUsuarias(Set.of(500));

        assertThatThrownBy(() -> nodoFlujoService.crearEvento(30, usuario, datos))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(nodoFlujoRepository, never()).save(any());
    }

    // =====================================================================
    // Eventos: actualizarEvento (HU-25, HU-27)
    // =====================================================================

    @Test
    @DisplayName("HU-27 CA5: Un Message Catch de inicio no puede tener arcos entrantes")
    void actualizarEvento_mensajeCatchInicioConArcosEntrantes_lanzaConflicto() {
        Evento evento = eventoExistente(TipoEvento.INICIO);
        evento.setNombre("Evento Inicio");
        when(nodoFlujoRepository.findById(400)).thenReturn(Optional.of(evento));
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(arcoRepository.existsByDestinoId(400)).thenReturn(true);

        Evento datos = datosEvento(TipoEvento.MENSAJE_RECEPCION_INICIO);
        datos.setNombre("Evento Recepción Inicio");

        assertThatThrownBy(() -> nodoFlujoService.actualizarEvento(400, usuario, datos))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("Un Message Catch de inicio no puede tener arcos entrantes");

        assertThat(evento.getTipoEvento()).isEqualTo(TipoEvento.INICIO);
        verify(nodoFlujoRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-27: evento puede pasar a Message Catch de inicio si no tiene arcos entrantes")
    void actualizarEvento_aRecepcionInicioSinArcosEntrantes_cambiaTipo() {
        Evento existente = eventoExistente(TipoEvento.INICIO);
        stubActualizarEvento(existente);
        when(arcoRepository.existsByDestinoId(400)).thenReturn(false);

        Evento datos = datosEvento(TipoEvento.MENSAJE_RECEPCION_INICIO);
        datos.setNombre("  Recibir solicitud  ");
        datos.setOrigenExterno(true);

        Evento resultado = nodoFlujoService.actualizarEvento(400, usuario, datos);

        assertThat(resultado.getTipoEvento()).isEqualTo(TipoEvento.MENSAJE_RECEPCION_INICIO);
        assertThat(resultado.getNombre()).isEqualTo("Recibir solicitud");
        assertThat(resultado.getOrigenExterno()).isTrue();
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.EDITAR), eq("EVENTO"), eq(400),
                anyString());
    }

    @Test
    @DisplayName("HU-25: evento que lanza mensajes no puede dejar de ser MENSAJE_LANZAMIENTO")
    void actualizarEvento_cambioDeTipoConMensajesSalientes_lanzaConflicto() {
        Evento existente = eventoExistente(TipoEvento.MENSAJE_LANZAMIENTO);
        stubActualizarEvento(existente);
        when(mensajeRepository.findByOrigenId(400)).thenReturn(List.of(new Mensaje()));

        assertThatThrownBy(() -> nodoFlujoService.actualizarEvento(400, usuario, datosEvento(TipoEvento.INICIO)))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("debe seguir siendo de tipo MENSAJE_LANZAMIENTO");

        assertThat(existente.getTipoEvento()).isEqualTo(TipoEvento.MENSAJE_LANZAMIENTO);
        verify(nodoFlujoRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-25: evento que lanza mensajes puede seguir siendo MENSAJE_LANZAMIENTO (no consulta mensajes)")
    void actualizarEvento_siguesiendoLanzamiento_permitido() {
        Evento existente = eventoExistente(TipoEvento.MENSAJE_LANZAMIENTO);
        stubActualizarEvento(existente);
        Evento datos = datosEvento(TipoEvento.MENSAJE_LANZAMIENTO);
        datos.setContenido("Nuevo contenido");

        Evento resultado = nodoFlujoService.actualizarEvento(400, usuario, datos);

        assertThat(resultado.getContenido()).isEqualTo("Nuevo contenido");
        verify(mensajeRepository, never()).findByOrigenId(anyInt());
    }

    @Test
    @DisplayName("HU-25: evento sin mensajes salientes puede cambiar libremente de tipo")
    void actualizarEvento_sinMensajesSalientes_cambiaTipo() {
        Evento existente = eventoExistente(TipoEvento.MENSAJE_LANZAMIENTO);
        stubActualizarEvento(existente);
        when(mensajeRepository.findByOrigenId(400)).thenReturn(List.of());

        Evento resultado = nodoFlujoService.actualizarEvento(400, usuario, datosEvento(TipoEvento.INICIO));

        assertThat(resultado.getTipoEvento()).isEqualTo(TipoEvento.INICIO);
        assertThat(resultado.getContenido()).isNull();
    }

    @Test
    @DisplayName("Aislamiento: actualizarEvento de OTRA empresa -> RecursoNoEncontradoException")
    void actualizarEvento_deOtraEmpresa_lanzaNoEncontrado() {
        Evento ajeno = deOtraEmpresa(new Evento(), 400);
        when(nodoFlujoRepository.findById(400)).thenReturn(Optional.of(ajeno));

        assertThatThrownBy(() -> nodoFlujoService.actualizarEvento(400, usuario, datosEvento(TipoEvento.INICIO)))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(nodoFlujoRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarEvento: el id corresponde a una Actividad -> 'Evento no encontrado'")
    void actualizarEvento_idDeActividad_lanzaNoEncontrado() {
        when(nodoFlujoRepository.findById(100)).thenReturn(Optional.of(enMiPool(new Actividad(), 100)));

        assertThatThrownBy(() -> nodoFlujoService.actualizarEvento(100, usuario, datosEvento(TipoEvento.INICIO)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Evento no encontrado");
    }

    // =====================================================================
    // Eventos: eliminarEvento y eliminarNodo con mensajes (HU-10, HU-16)
    // =====================================================================

    @Test
    @DisplayName("HU-10/16: eliminarEvento desactiva el evento, sus arcos y sus mensajes, y registra cada baja")
    void eliminarEvento_desactivaEventoArcosYMensajes() {
        Evento evento = eventoExistente(TipoEvento.MENSAJE_LANZAMIENTO);
        evento.setActivo(true);

        Arco arco = new Arco();
        arco.setId(201);
        arco.setActivo(true);

        Mensaje mensaje = new Mensaje();
        mensaje.setId(601);
        mensaje.setNombre("Factura");
        mensaje.setActivo(true);

        Map<String, Object> esperado = Map.of("advertencias", List.of("aviso"));

        when(nodoFlujoRepository.findById(400)).thenReturn(Optional.of(evento));
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(arcoRepository.findByOrigenIdOrDestinoId(400, 400)).thenReturn(List.of(arco));
        when(mensajeRepository.findByOrigenId(400)).thenReturn(List.of(mensaje));
        when(validacionService.resultadoEliminacion(proceso, 1, 1)).thenReturn(esperado);

        Map<String, Object> resultado = nodoFlujoService.eliminarEvento(400, usuario);

        assertThat(resultado).isSameAs(esperado);
        assertThat(evento.isActivo()).isFalse();
        assertThat(arco.isActivo()).isFalse();
        assertThat(mensaje.isActivo()).isFalse();
        verify(arcoRepository).save(arco);
        verify(mensajeRepository).save(mensaje);
        verify(nodoFlujoRepository).save(evento);
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.ELIMINAR), eq("ARCO"), eq(201),
                anyString());
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.ELIMINAR), eq("MENSAJE"),
                eq(601), anyString());
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.ELIMINAR), eq("EVENTO"),
                eq(400), anyString());
    }

    @Test
    @DisplayName("Aislamiento: eliminarEvento de OTRA empresa -> RecursoNoEncontradoException y no elimina nada")
    void eliminarEvento_deOtraEmpresa_lanzaNoEncontradoYNoElimina() {
        Evento ajeno = deOtraEmpresa(new Evento(), 400);
        ajeno.setActivo(true);
        when(nodoFlujoRepository.findById(400)).thenReturn(Optional.of(ajeno));

        assertThatThrownBy(() -> nodoFlujoService.eliminarEvento(400, usuario))
                .isInstanceOf(RecursoNoEncontradoException.class);

        assertThat(ajeno.isActivo()).isTrue();
        verify(nodoFlujoRepository, never()).save(any());
        verify(arcoRepository, never()).save(any());
    }
}