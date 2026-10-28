package desarrollo.web.Bizagi2.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import desarrollo.web.Bizagi2.config.ModelMapperConfig;
import desarrollo.web.Bizagi2.dto.arco.ArcoRequest;
import desarrollo.web.Bizagi2.dto.arco.ArcoResponse;
import desarrollo.web.Bizagi2.dto.correlacion.CorrelacionRequest;
import desarrollo.web.Bizagi2.dto.correlacion.CorrelacionResponse;
import desarrollo.web.Bizagi2.dto.empresa.EmpresaRequest;
import desarrollo.web.Bizagi2.dto.empresa.EmpresaResponse;
import desarrollo.web.Bizagi2.dto.lane.LaneCreateRequest;
import desarrollo.web.Bizagi2.dto.lane.LaneResponse;
import desarrollo.web.Bizagi2.dto.lane.LaneUpdateRequest;
import desarrollo.web.Bizagi2.dto.mensaje.MensajeRequest;
import desarrollo.web.Bizagi2.dto.mensaje.MensajeResponse;
import desarrollo.web.Bizagi2.dto.nodo.ActividadCreateRequest;
import desarrollo.web.Bizagi2.dto.nodo.ActividadResponse;
import desarrollo.web.Bizagi2.dto.nodo.ActividadUpdateRequest;
import desarrollo.web.Bizagi2.dto.nodo.EventoRequest;
import desarrollo.web.Bizagi2.dto.nodo.EventoResponse;
import desarrollo.web.Bizagi2.dto.nodo.GatewayRequest;
import desarrollo.web.Bizagi2.dto.nodo.GatewayResponse;
import desarrollo.web.Bizagi2.dto.pool.PoolRequest;
import desarrollo.web.Bizagi2.dto.pool.PoolResponse;
import desarrollo.web.Bizagi2.dto.proceso.HistorialResponse;
import desarrollo.web.Bizagi2.dto.proceso.ProcesoRequest;
import desarrollo.web.Bizagi2.dto.proceso.ProcesoResponse;
import desarrollo.web.Bizagi2.dto.rol.RolProcesoRequest;
import desarrollo.web.Bizagi2.dto.rol.RolProcesoResponse;
import desarrollo.web.Bizagi2.dto.usuario.UsuarioCreateRequest;
import desarrollo.web.Bizagi2.dto.usuario.UsuarioResponse;
import desarrollo.web.Bizagi2.dto.usuario.UsuarioUpdateRequest;
import desarrollo.web.Bizagi2.entities.AccionFallo;
import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.AccionSinCaso;
import desarrollo.web.Bizagi2.entities.Actividad;
import desarrollo.web.Bizagi2.entities.Arco;
import desarrollo.web.Bizagi2.entities.Correlacion;
import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.EstadoProceso;
import desarrollo.web.Bizagi2.entities.Evento;
import desarrollo.web.Bizagi2.entities.Gateway;
import desarrollo.web.Bizagi2.entities.Historial;
import desarrollo.web.Bizagi2.entities.Lane;
import desarrollo.web.Bizagi2.entities.Mensaje;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.RolProceso;
import desarrollo.web.Bizagi2.entities.TipoActividad;
import desarrollo.web.Bizagi2.entities.TipoDestino;
import desarrollo.web.Bizagi2.entities.TipoEvento;
import desarrollo.web.Bizagi2.entities.TipoGateway;
import desarrollo.web.Bizagi2.entities.TipoParticipante;
import desarrollo.web.Bizagi2.entities.Usuario;

// Verifica cada conversion entidad <-> DTO con el mismo ModelMapper (STRICT) que usa la aplicacion.
// Lo importante: los ids de referencia del request (rolProcesoId, origenId...) llegan a la relacion
// correcta y nunca al id de la entidad, y las respuestas no exponen datos sensibles.
@DisplayName("Mapeo entidad <-> DTO con ModelMapper")
class DtoMappingTest {

    private final ModelMapper modelMapper = new ModelMapperConfig().modelMapper();

    // ---------- datos de apoyo ----------

    private Empresa empresa() {
        Empresa empresa = new Empresa();
        empresa.setId(7);
        empresa.setNombre("Acme SAS");
        empresa.setNit("900123456");
        empresa.setEmailContacto("contacto@acme.com");
        empresa.setEditorModificaEstructura(false);
        return empresa;
    }

    private Proceso proceso() {
        Proceso proceso = new Proceso();
        proceso.setId(10);
        proceso.setEmpresa(empresa());
        proceso.setNombre("Credito");
        proceso.setDescripcion("Aprobacion de creditos");
        proceso.setCategoria("Finanzas");
        proceso.setEstado(EstadoProceso.BORRADOR);
        proceso.setActivo(true);
        return proceso;
    }

    private Pool pool(Integer id, String nombre) {
        Pool pool = new Pool();
        pool.setId(id);
        pool.setNombre(nombre);
        pool.setProceso(proceso());
        pool.setTipoParticipante(TipoParticipante.EMPRESA_PROPIETARIA);
        return pool;
    }

    private RolProceso rol() {
        RolProceso rol = new RolProceso();
        rol.setId(30);
        rol.setNombre("Analista");
        rol.setDescripcion("Revisa solicitudes");
        rol.setEmpresa(empresa());
        return rol;
    }

    private Lane lane() {
        Lane lane = new Lane();
        lane.setId(40);
        lane.setNombre("Analistas");
        lane.setOrden(2);
        lane.setPool(pool(20, "Acme"));
        lane.setRolProceso(rol());
        return lane;
    }

    // ---------- Usuario y Empresa ----------

    @Test
    @DisplayName("Usuario -> UsuarioResponse: aplana empresaId y no expone la contrasena")
    void usuarioAResponse() {
        Usuario usuario = new Usuario();
        usuario.setId(3);
        usuario.setNombre("Ana Perez");
        usuario.setEmail("ana@acme.com");
        usuario.setPassword("$2a$10$hash");
        usuario.setRolAcceso(RolAcceso.EDITOR);
        usuario.setActivo(true);
        usuario.setEmpresa(empresa());

        UsuarioResponse dto = modelMapper.map(usuario, UsuarioResponse.class);

        assertThat(dto.getId()).isEqualTo(3);
        assertThat(dto.getNombre()).isEqualTo("Ana Perez");
        assertThat(dto.getEmail()).isEqualTo("ana@acme.com");
        assertThat(dto.getRolAcceso()).isEqualTo(RolAcceso.EDITOR);
        assertThat(dto.isActivo()).isTrue();
        assertThat(dto.getEmpresaId()).isEqualTo(7);
        assertThat(UsuarioResponse.class.getDeclaredFields())
                .extracting("name")
                .doesNotContain("password", "versionSesion", "empresa");
    }

    @Test
    @DisplayName("Requests de usuario -> Usuario: no inventan empresa ni id")
    void usuarioRequestsAEntidad() {
        Usuario creado = modelMapper.map(
                new UsuarioCreateRequest("Luis", "luis@acme.com", "Clave12345", RolAcceso.LECTOR), Usuario.class);
        assertThat(creado.getNombre()).isEqualTo("Luis");
        assertThat(creado.getEmail()).isEqualTo("luis@acme.com");
        assertThat(creado.getPassword()).isEqualTo("Clave12345");
        assertThat(creado.getRolAcceso()).isEqualTo(RolAcceso.LECTOR);
        assertThat(creado.getId()).isNull();
        assertThat(creado.getEmpresa()).isNull();

        Usuario cambios = modelMapper.map(new UsuarioUpdateRequest("Luis M", null, null, null), Usuario.class);
        assertThat(cambios.getNombre()).isEqualTo("Luis M");
        assertThat(cambios.getEmail()).isNull();
        assertThat(cambios.getRolAcceso()).isNull();
    }

    @Test
    @DisplayName("Empresa <-> DTOs")
    void empresaIdaYVuelta() {
        EmpresaResponse dto = modelMapper.map(empresa(), EmpresaResponse.class);
        assertThat(dto.getId()).isEqualTo(7);
        assertThat(dto.getNit()).isEqualTo("900123456");
        assertThat(dto.getEditorModificaEstructura()).isFalse();

        Empresa entidad = modelMapper.map(new EmpresaRequest("Nueva", "800", "n@acme.com", null), Empresa.class);
        assertThat(entidad.getNombre()).isEqualTo("Nueva");
        assertThat(entidad.getId()).isNull();
    }

    // ---------- Proceso, Pool, Lane, Rol ----------

    @Test
    @DisplayName("Proceso <-> DTOs: empresa.id sale como empresaId")
    void procesoIdaYVuelta() {
        ProcesoResponse dto = modelMapper.map(proceso(), ProcesoResponse.class);
        assertThat(dto.getId()).isEqualTo(10);
        assertThat(dto.getNombre()).isEqualTo("Credito");
        assertThat(dto.getCategoria()).isEqualTo("Finanzas");
        assertThat(dto.getEstado()).isEqualTo(EstadoProceso.BORRADOR);
        assertThat(dto.isActivo()).isTrue();
        assertThat(dto.getEmpresaId()).isEqualTo(7);

        Proceso entidad = modelMapper.map(
                new ProcesoRequest("Compras", "desc", "Logistica", EstadoProceso.PUBLICADO), Proceso.class);
        assertThat(entidad.getNombre()).isEqualTo("Compras");
        assertThat(entidad.getEstado()).isEqualTo(EstadoProceso.PUBLICADO);
        assertThat(entidad.getId()).isNull();
        assertThat(entidad.getEmpresa()).isNull();
    }

    @Test
    @DisplayName("Pool <-> DTOs: proceso.id sale como procesoId")
    void poolIdaYVuelta() {
        PoolResponse dto = modelMapper.map(pool(20, "Acme"), PoolResponse.class);
        assertThat(dto.getId()).isEqualTo(20);
        assertThat(dto.getProcesoId()).isEqualTo(10);
        assertThat(dto.getTipoParticipante()).isEqualTo(TipoParticipante.EMPRESA_PROPIETARIA);

        Pool entidad = modelMapper.map(new PoolRequest("Banco", TipoParticipante.CLIENTE), Pool.class);
        assertThat(entidad.getNombre()).isEqualTo("Banco");
        assertThat(entidad.getTipoParticipante()).isEqualTo(TipoParticipante.CLIENTE);
        assertThat(entidad.getId()).isNull();
        assertThat(entidad.getProceso()).isNull();
    }

    @Test
    @DisplayName("Lane <-> DTOs: rolProcesoId llega a lane.rolProceso.id y nunca al id de la lane")
    void laneIdaYVuelta() {
        LaneResponse dto = modelMapper.map(lane(), LaneResponse.class);
        assertThat(dto.getId()).isEqualTo(40);
        assertThat(dto.getOrden()).isEqualTo(2);
        assertThat(dto.getPoolId()).isEqualTo(20);
        assertThat(dto.getRolProcesoId()).isEqualTo(30);
        assertThat(dto.getRolProcesoNombre()).isEqualTo("Analista");

        Lane creada = modelMapper.map(new LaneCreateRequest(30, "Analistas"), Lane.class);
        assertThat(creada.getId()).isNull();
        assertThat(creada.getNombre()).isEqualTo("Analistas");
        assertThat(creada.getRolProceso()).isNotNull();
        assertThat(creada.getRolProceso().getId()).isEqualTo(30);
        assertThat(creada.getPool()).isNull();

        Lane sinRol = modelMapper.map(new LaneUpdateRequest(null, "Solo nombre"), Lane.class);
        assertThat(sinRol.getNombre()).isEqualTo("Solo nombre");
        assertThat(sinRol.getRolProceso() == null || sinRol.getRolProceso().getId() == null).isTrue();
    }

    @Test
    @DisplayName("RolProceso <-> DTOs")
    void rolProceso() {
        RolProcesoResponse dto = modelMapper.map(rol(), RolProcesoResponse.class);
        assertThat(dto.getId()).isEqualTo(30);
        assertThat(dto.getNombre()).isEqualTo("Analista");
        assertThat(dto.getDescripcion()).isEqualTo("Revisa solicitudes");

        RolProceso entidad = modelMapper.map(new RolProcesoRequest("Auditor", "Audita"), RolProceso.class);
        assertThat(entidad.getNombre()).isEqualTo("Auditor");
        assertThat(entidad.getId()).isNull();
        assertThat(entidad.getEmpresa()).isNull();
    }

    // ---------- Nodos del diagrama ----------

    @Test
    @DisplayName("Actividad <-> DTOs: laneId llega a actividad.lane.id")
    void actividad() {
        Actividad actividad = new Actividad();
        actividad.setId(100);
        actividad.setNombre("Revisar");
        actividad.setTipo(TipoActividad.USUARIO);
        actividad.setDescripcion("Revisa la solicitud");
        actividad.setPosicionX(10.5);
        actividad.setPosicionY(20);
        actividad.setPool(pool(20, "Acme"));
        actividad.setLane(lane());

        ActividadResponse dto = modelMapper.map(actividad, ActividadResponse.class);
        assertThat(dto.getId()).isEqualTo(100);
        assertThat(dto.getTipo()).isEqualTo(TipoActividad.USUARIO);
        assertThat(dto.getPosicionX()).isEqualTo(10.5);
        assertThat(dto.getPoolId()).isEqualTo(20);
        assertThat(dto.getLaneId()).isEqualTo(40);

        Actividad creada = modelMapper.map(
                new ActividadCreateRequest("Aprobar", TipoActividad.TAREA, null, 1.0, 2.0), Actividad.class);
        assertThat(creada.getNombre()).isEqualTo("Aprobar");
        assertThat(creada.getTipo()).isEqualTo(TipoActividad.TAREA);
        assertThat(creada.getPosicionY()).isEqualTo(2);
        assertThat(creada.getId()).isNull();

        Actividad movida = modelMapper.map(
                new ActividadUpdateRequest("Aprobar", null, null, null, null, 41), Actividad.class);
        assertThat(movida.getId()).isNull();
        assertThat(movida.getLane()).isNotNull();
        assertThat(movida.getLane().getId()).isEqualTo(41);
        assertThat(movida.getTipo()).isNull();
        assertThat(movida.getPosicionX()).isZero();
    }

    @Test
    @DisplayName("Gateway <-> DTOs")
    void gateway() {
        Gateway gateway = new Gateway();
        gateway.setId(300);
        gateway.setNombre("Monto?");
        gateway.setTipoGateway(TipoGateway.EXCLUSIVA);
        gateway.setPool(pool(20, "Acme"));

        GatewayResponse dto = modelMapper.map(gateway, GatewayResponse.class);
        assertThat(dto.getId()).isEqualTo(300);
        assertThat(dto.getTipoGateway()).isEqualTo(TipoGateway.EXCLUSIVA);
        assertThat(dto.getPoolId()).isEqualTo(20);

        Gateway entidad = modelMapper.map(new GatewayRequest("Paralelo", TipoGateway.PARALELA, 5.0, 6.0), Gateway.class);
        assertThat(entidad.getTipoGateway()).isEqualTo(TipoGateway.PARALELA);
        assertThat(entidad.getPosicionX()).isEqualTo(5);
        assertThat(entidad.getId()).isNull();
    }

    @Test
    @DisplayName("Evento <-> DTOs: conserva actividadesUsuarias")
    void evento() {
        Evento evento = new Evento();
        evento.setId(400);
        evento.setNombre("Pago recibido");
        evento.setTipoEvento(TipoEvento.MENSAJE_RECEPCION_INTERMEDIO);
        evento.setContenido("monto:numero");
        evento.setOrigenExterno(true);
        evento.setClaveCorrelacion("radicado");
        evento.setActividadesUsuarias(Set.of(100, 101));
        evento.setPool(pool(20, "Acme"));

        EventoResponse dto = modelMapper.map(evento, EventoResponse.class);
        assertThat(dto.getId()).isEqualTo(400);
        assertThat(dto.getTipoEvento()).isEqualTo(TipoEvento.MENSAJE_RECEPCION_INTERMEDIO);
        assertThat(dto.getOrigenExterno()).isTrue();
        assertThat(dto.getActividadesUsuarias()).containsExactlyInAnyOrder(100, 101);
        assertThat(dto.getPoolId()).isEqualTo(20);

        Evento entidad = modelMapper.map(new EventoRequest("Inicio", TipoEvento.INICIO, null, null, null, null, null,
                Set.of(7)), Evento.class);
        assertThat(entidad.getTipoEvento()).isEqualTo(TipoEvento.INICIO);
        assertThat(entidad.getActividadesUsuarias()).containsExactly(7);
        assertThat(entidad.getId()).isNull();
    }

    // ---------- Arcos, mensajes y correlaciones ----------

    @Test
    @DisplayName("Arco <-> DTOs: origenId/destinoId llegan a arco.origen.id/arco.destino.id")
    void arco() {
        Actividad origen = new Actividad();
        origen.setId(100);
        origen.setNombre("Revisar");
        Gateway destino = new Gateway();
        destino.setId(300);
        destino.setNombre("Monto?");
        Arco arco = new Arco();
        arco.setId(500);
        arco.setPool(pool(20, "Acme"));
        arco.setOrigen(origen);
        arco.setDestino(destino);
        arco.setEtiqueta("ok");

        ArcoResponse dto = modelMapper.map(arco, ArcoResponse.class);
        assertThat(dto.getId()).isEqualTo(500);
        assertThat(dto.getPoolId()).isEqualTo(20);
        assertThat(dto.getOrigenId()).isEqualTo(100);
        assertThat(dto.getOrigenNombre()).isEqualTo("Revisar");
        assertThat(dto.getDestinoId()).isEqualTo(300);
        assertThat(dto.getDestinoNombre()).isEqualTo("Monto?");
        assertThat(dto.getEtiqueta()).isEqualTo("ok");

        Arco entidad = modelMapper.map(new ArcoRequest(100, 300, "si", "monto > 10"), Arco.class);
        assertThat(entidad.getId()).isNull();
        assertThat(entidad.getOrigen().getId()).isEqualTo(100);
        assertThat(entidad.getDestino().getId()).isEqualTo(300);
        assertThat(entidad.getEtiqueta()).isEqualTo("si");
        assertThat(entidad.getCondicion()).isEqualTo("monto > 10");
        assertThat(entidad.getPool()).isNull();
    }

    @Test
    @DisplayName("Mensaje <-> DTOs: destinoPoolId llega a mensaje.destinoPool.id")
    void mensaje() {
        Evento origen = new Evento();
        origen.setId(400);
        origen.setNombre("Enviar factura");
        Mensaje mensaje = new Mensaje();
        mensaje.setId(600);
        mensaje.setProceso(proceso());
        mensaje.setNombre("Factura");
        mensaje.setOrigen(origen);
        mensaje.setDestinoPool(pool(21, "Banco"));
        mensaje.setTipoDestino(TipoDestino.CORREO);
        mensaje.setAccionFallo(AccionFallo.CONTINUAR_FLUJO);

        MensajeResponse dto = modelMapper.map(mensaje, MensajeResponse.class);
        assertThat(dto.getId()).isEqualTo(600);
        assertThat(dto.getProcesoId()).isEqualTo(10);
        assertThat(dto.getOrigenId()).isEqualTo(400);
        assertThat(dto.getOrigenNombre()).isEqualTo("Enviar factura");
        assertThat(dto.getDestinoPoolId()).isEqualTo(21);
        assertThat(dto.getDestinoPoolNombre()).isEqualTo("Banco");
        assertThat(dto.getTipoDestino()).isEqualTo(TipoDestino.CORREO);

        Mensaje entidad = modelMapper.map(new MensajeRequest("Factura", 400, 21, "total:numero",
                TipoDestino.COLA, "al aprobar", AccionFallo.FINALIZAR_PROCESO, null), Mensaje.class);
        assertThat(entidad.getId()).isNull();
        assertThat(entidad.getOrigen().getId()).isEqualTo(400);
        assertThat(entidad.getDestinoPool().getId()).isEqualTo(21);
        assertThat(entidad.getProceso()).isNull();
        assertThat(entidad.getMomento()).isEqualTo("al aprobar");
        assertThat(entidad.getAccionFallo()).isEqualTo(AccionFallo.FINALIZAR_PROCESO);

        Actividad manejoError = new Actividad();
        manejoError.setId(105);
        manejoError.setNombre("Gestionar fallo");
        mensaje.setActividadError(manejoError);
        MensajeResponse conError = modelMapper.map(mensaje, MensajeResponse.class);
        assertThat(conError.getActividadErrorId()).isEqualTo(105);
        assertThat(conError.getActividadErrorNombre()).isEqualTo("Gestionar fallo");

        MensajeRequest deriva = new MensajeRequest("Cobro", 400, 21, null, TipoDestino.SERVICIO_WEB, null,
                AccionFallo.DERIVAR_A_MANEJO_ERROR, 105);
        assertThat(modelMapper.map(deriva, Mensaje.class).getActividadError().getId()).isEqualTo(105);
        assertThat(entidad.getActividadError()).isNull();
    }

    @Test
    @DisplayName("Correlacion <-> DTOs: mensaje.id sale como mensajeId")
    void correlacion() {
        Mensaje mensaje = new Mensaje();
        mensaje.setId(600);
        Correlacion correlacion = new Correlacion();
        correlacion.setId(700);
        correlacion.setMensaje(mensaje);
        correlacion.setCriterio("radicado");
        correlacion.setAccionSinCaso(AccionSinCaso.DESCARTAR);

        CorrelacionResponse dto = modelMapper.map(correlacion, CorrelacionResponse.class);
        assertThat(dto.getId()).isEqualTo(700);
        assertThat(dto.getMensajeId()).isEqualTo(600);
        assertThat(dto.getCriterio()).isEqualTo("radicado");

        Correlacion entidad = modelMapper.map(
                new CorrelacionRequest("factura", AccionSinCaso.INICIAR_CASO_NUEVO), Correlacion.class);
        assertThat(entidad.getId()).isNull();
        assertThat(entidad.getMensaje()).isNull();
        assertThat(entidad.getAccionSinCaso()).isEqualTo(AccionSinCaso.INICIAR_CASO_NUEVO);
    }

    @Test
    @DisplayName("Historial -> HistorialResponse: solo id y nombre del usuario")
    void historial() {
        Usuario usuario = new Usuario();
        usuario.setId(3);
        usuario.setNombre("Ana");
        usuario.setPassword("secreta");
        Historial historial = new Historial();
        historial.setId(900);
        historial.setProceso(proceso());
        historial.setUsuario(usuario);
        historial.setAccion(AccionHistorial.EDITAR);
        historial.setEntidad("PROCESO");
        historial.setEntidadId(10);
        historial.setFecha(Instant.parse("2026-10-27T12:00:00Z"));
        historial.setDetalle("nombre cambiado");

        HistorialResponse dto = modelMapper.map(historial, HistorialResponse.class);
        assertThat(dto.getId()).isEqualTo(900);
        assertThat(dto.getAccion()).isEqualTo(AccionHistorial.EDITAR);
        assertThat(dto.getEntidad()).isEqualTo("PROCESO");
        assertThat(dto.getEntidadId()).isEqualTo(10);
        assertThat(dto.getFecha()).isEqualTo(Instant.parse("2026-10-27T12:00:00Z"));
        assertThat(dto.getUsuarioId()).isEqualTo(3);
        assertThat(dto.getUsuarioNombre()).isEqualTo("Ana");
    }
}
