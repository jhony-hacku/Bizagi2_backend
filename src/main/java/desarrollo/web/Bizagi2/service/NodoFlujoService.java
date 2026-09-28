package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.Validaciones.requerido;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Actividad;
import desarrollo.web.Bizagi2.entities.Arco;
import desarrollo.web.Bizagi2.entities.Evento;
import desarrollo.web.Bizagi2.entities.Gateway;
import desarrollo.web.Bizagi2.entities.Lane;
import desarrollo.web.Bizagi2.entities.Mensaje;
import desarrollo.web.Bizagi2.entities.NodoFlujo;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
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
import lombok.RequiredArgsConstructor;

// Los tres tipos de nodo del diagrama: Actividad, Gateway y Evento (comparten la tabla nodos_flujo)
@Service
@RequiredArgsConstructor
@Transactional
public class NodoFlujoService {

    private final NodoFlujoRepository nodoFlujoRepository;
    private final ArcoRepository arcoRepository;
    private final MensajeRepository mensajeRepository;
    private final PoolService poolService;
    private final LaneService laneService;
    private final ProcesoService procesoService;
    private final HistorialService historialService;
    private final ValidacionService validacionService;

    // ---------- Consultas ----------

    @Transactional(readOnly = true)
    public List<Actividad> listarActividades(Integer poolId, Integer empresaId) {
        poolService.buscarPorId(poolId, empresaId);
        return nodoFlujoRepository.findActividadesByPool(poolId);
    }

    @Transactional(readOnly = true)
    public List<Gateway> listarGateways(Integer poolId, Integer empresaId) {
        poolService.buscarPorId(poolId, empresaId);
        return nodoFlujoRepository.findGatewaysByPool(poolId);
    }

    @Transactional(readOnly = true)
    public List<Evento> listarEventos(Integer poolId, Integer empresaId) {
        poolService.buscarPorId(poolId, empresaId);
        return nodoFlujoRepository.findEventosByPool(poolId);
    }

    // Cualquier nodo de la empresa; lo usan los arcos y los mensajes
    @Transactional(readOnly = true)
    public NodoFlujo buscarNodo(Integer id, Integer empresaId) {
        NodoFlujo nodo = nodoFlujoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Elemento no encontrado"));
        if (!nodo.getPool().getProceso().getEmpresa().getId().equals(empresaId)) {
            throw new RecursoNoEncontradoException("Elemento no encontrado");
        }
        return nodo;
    }

    @Transactional(readOnly = true)
    public Actividad buscarActividad(Integer id, Integer empresaId) {
        if (buscarNodo(id, empresaId) instanceof Actividad actividad) {
            return actividad;
        }
        throw new RecursoNoEncontradoException("Actividad no encontrada");
    }

    @Transactional(readOnly = true)
    public Gateway buscarGateway(Integer id, Integer empresaId) {
        if (buscarNodo(id, empresaId) instanceof Gateway gateway) {
            return gateway;
        }
        throw new RecursoNoEncontradoException("Gateway no encontrado");
    }

    @Transactional(readOnly = true)
    public Evento buscarEvento(Integer id, Integer empresaId) {
        if (buscarNodo(id, empresaId) instanceof Evento evento) {
            return evento;
        }
        throw new RecursoNoEncontradoException("Evento no encontrado");
    }

    // ---------- Actividades (HU-08, 09, 10) ----------

    // Se crea dentro de una lane: su pool es el de la lane y su responsable es el rol de la lane
    public Actividad crearActividad(Integer laneId, Usuario usuario, Actividad datos) {
        requerido(datos.getNombre(), "nombre");
        requerido(datos.getTipo(), "tipo");
        Integer empresaId = usuario.getEmpresa().getId();
        Lane lane = laneService.buscarPorId(laneId, empresaId);
        Proceso proceso = procesoService.buscarActivo(lane.getPool().getProceso().getId(), empresaId);
        verificarNombreUnico(proceso.getId(), datos.getNombre(), -1);

        Actividad actividad = new Actividad();
        actividad.setPool(lane.getPool());
        actividad.setLane(lane);
        actividad.setTipo(datos.getTipo());
        actividad.setDescripcion(datos.getDescripcion());
        copiarDatosBasicos(actividad, datos);
        actividad = nodoFlujoRepository.save(actividad);
        historialService.registrar(usuario, proceso, AccionHistorial.CREAR, "ACTIVIDAD", actividad.getId(),
                "Actividad '" + actividad.getNombre() + "' (" + actividad.getTipo() + ") creada en la lane '"
                        + lane.getNombre() + "'");
        return actividad;
    }

    public Actividad actualizarActividad(Integer id, Usuario usuario, Actividad datos) {
        requerido(datos.getNombre(), "nombre");
        Integer empresaId = usuario.getEmpresa().getId();
        Actividad actividad = buscarActividad(id, empresaId);
        Proceso proceso = procesoService.buscarActivo(actividad.getPool().getProceso().getId(), empresaId);
        verificarNombreUnico(proceso.getId(), datos.getNombre(), id);

        if (datos.getTipo() != null) {
            if (datos.getTipo() != TipoActividad.ENVIO && !mensajeRepository.findByOrigenId(id).isEmpty()) {
                throw new ConflictoDominioException("La actividad envia mensajes: debe seguir siendo de tipo ENVIO");
            }
            actividad.setTipo(datos.getTipo());
        }
        if (datos.getLane() != null && datos.getLane().getId() != null) {
            Lane lane = laneService.buscarPorId(datos.getLane().getId(), empresaId);
            if (!lane.getPool().getId().equals(actividad.getPool().getId())) {
                throw new ReglaNegocioException("La lane debe pertenecer al mismo pool de la actividad");
            }
            actividad.setLane(lane);
        }
        actividad.setDescripcion(datos.getDescripcion());
        copiarDatosBasicos(actividad, datos);
        actividad = nodoFlujoRepository.save(actividad);
        historialService.registrar(usuario, proceso, AccionHistorial.EDITAR, "ACTIVIDAD", id,
                "Actividad '" + actividad.getNombre() + "' modificada");
        return actividad;
    }

    // Se eliminan (logicamente) tambien los arcos y mensajes conectados a la actividad
    public Map<String, Object> eliminarActividad(Integer id, Usuario usuario) {
        return eliminarNodo(buscarActividad(id, usuario.getEmpresa().getId()), usuario, "ACTIVIDAD");
    }

    // ---------- Gateways (HU-14, 15, 16) ----------

    public Gateway crearGateway(Integer poolId, Usuario usuario, Gateway datos) {
        requerido(datos.getNombre(), "nombre");
        requerido(datos.getTipoGateway(), "tipoGateway");
        Integer empresaId = usuario.getEmpresa().getId();
        Pool pool = poolService.buscarPorId(poolId, empresaId);
        Proceso proceso = procesoService.buscarActivo(pool.getProceso().getId(), empresaId);
        poolService.verificarInterno(pool);

        Gateway gateway = new Gateway();
        gateway.setPool(pool);
        gateway.setTipoGateway(datos.getTipoGateway());
        copiarDatosBasicos(gateway, datos);
        gateway = nodoFlujoRepository.save(gateway);
        historialService.registrar(usuario, proceso, AccionHistorial.CREAR, "GATEWAY", gateway.getId(),
                "Gateway '" + gateway.getNombre() + "' (" + gateway.getTipoGateway() + ") creado");
        return gateway;
    }

    public Gateway actualizarGateway(Integer id, Usuario usuario, Gateway datos) {
        requerido(datos.getNombre(), "nombre");
        requerido(datos.getTipoGateway(), "tipoGateway");
        Integer empresaId = usuario.getEmpresa().getId();
        Gateway gateway = buscarGateway(id, empresaId);
        Proceso proceso = procesoService.buscarActivo(gateway.getPool().getProceso().getId(), empresaId);

        String detalle = "Gateway '" + datos.getNombre().trim() + "' modificado";
        // HU-15: al pasar a paralelo se eliminan las condiciones, porque todos los caminos se siguen
        if (datos.getTipoGateway() == TipoGateway.PARALELA && gateway.getTipoGateway() != TipoGateway.PARALELA) {
            for (Arco arco : arcoRepository.findByOrigenId(id)) {
                arco.setCondicion(null);
                arcoRepository.save(arco);
            }
            detalle += " (paso a PARALELA: se eliminaron las condiciones de sus arcos salientes)";
        }
        gateway.setTipoGateway(datos.getTipoGateway());
        copiarDatosBasicos(gateway, datos);
        gateway = nodoFlujoRepository.save(gateway);
        historialService.registrar(usuario, proceso, AccionHistorial.EDITAR, "GATEWAY", id, detalle);
        return gateway;
    }

    public Map<String, Object> eliminarGateway(Integer id, Usuario usuario) {
        return eliminarNodo(buscarGateway(id, usuario.getEmpresa().getId()), usuario, "GATEWAY");
    }

    // ---------- Eventos (HU-25 y HU-27: mensajes; HU-11: inicio y fin) ----------

    public Evento crearEvento(Integer poolId, Usuario usuario, Evento datos) {
        requerido(datos.getNombre(), "nombre");
        requerido(datos.getTipoEvento(), "tipoEvento");
        Integer empresaId = usuario.getEmpresa().getId();
        Pool pool = poolService.buscarPorId(poolId, empresaId);
        Proceso proceso = procesoService.buscarActivo(pool.getProceso().getId(), empresaId);
        poolService.verificarInterno(pool);

        Evento evento = new Evento();
        evento.setPool(pool);
        evento.setTipoEvento(datos.getTipoEvento());
        copiarDatosBasicos(evento, datos);
        copiarDatosDeMensaje(evento, datos, proceso.getId(), empresaId);
        evento = nodoFlujoRepository.save(evento);
        historialService.registrar(usuario, proceso, AccionHistorial.CREAR, "EVENTO", evento.getId(),
                "Evento '" + evento.getNombre() + "' (" + evento.getTipoEvento() + ") creado");
        return evento;
    }

    public Evento actualizarEvento(Integer id, Usuario usuario, Evento datos) {
        requerido(datos.getNombre(), "nombre");
        requerido(datos.getTipoEvento(), "tipoEvento");
        Integer empresaId = usuario.getEmpresa().getId();
        Evento evento = buscarEvento(id, empresaId);
        Proceso proceso = procesoService.buscarActivo(evento.getPool().getProceso().getId(), empresaId);

        // HU-27: un Message Catch de inicio no puede tener arcos entrantes
        if (datos.getTipoEvento() == TipoEvento.MENSAJE_RECEPCION_INICIO && arcoRepository.existsByDestinoId(id)) {
            throw new ConflictoDominioException("Un Message Catch de inicio no puede tener arcos entrantes: elimina los arcos que llegan al evento");
        }
        if (datos.getTipoEvento() != TipoEvento.MENSAJE_LANZAMIENTO && !mensajeRepository.findByOrigenId(id).isEmpty()) {
            throw new ConflictoDominioException("El evento lanza mensajes: debe seguir siendo de tipo MENSAJE_LANZAMIENTO");
        }
        evento.setTipoEvento(datos.getTipoEvento());
        copiarDatosBasicos(evento, datos);
        copiarDatosDeMensaje(evento, datos, proceso.getId(), empresaId);
        evento = nodoFlujoRepository.save(evento);
        historialService.registrar(usuario, proceso, AccionHistorial.EDITAR, "EVENTO", id,
                "Evento '" + evento.getNombre() + "' modificado");
        return evento;
    }

    public Map<String, Object> eliminarEvento(Integer id, Usuario usuario) {
        return eliminarNodo(buscarEvento(id, usuario.getEmpresa().getId()), usuario, "EVENTO");
    }

    // ---------- Apoyo ----------

    private void copiarDatosBasicos(NodoFlujo destino, NodoFlujo datos) {
        destino.setNombre(datos.getNombre().trim());
        destino.setPosicionX(datos.getPosicionX());
        destino.setPosicionY(datos.getPosicionY());
    }

    // Datos de los eventos de mensaje: solo aplican a lanzamiento y recepcion
    private void copiarDatosDeMensaje(Evento evento, Evento datos, Integer procesoId, Integer empresaId) {
        boolean recepcion = datos.getTipoEvento() == TipoEvento.MENSAJE_RECEPCION_INICIO
                || datos.getTipoEvento() == TipoEvento.MENSAJE_RECEPCION_INTERMEDIO;
        boolean mensaje = recepcion || datos.getTipoEvento() == TipoEvento.MENSAJE_LANZAMIENTO;
        evento.setContenido(mensaje ? datos.getContenido() : null);
        evento.setClaveCorrelacion(mensaje ? datos.getClaveCorrelacion() : null);
        evento.setOrigenExterno(recepcion ? Boolean.TRUE.equals(datos.getOrigenExterno()) : null);

        Set<Integer> actividades = new HashSet<>();
        if (recepcion && datos.getActividadesUsuarias() != null) {
            for (Integer actividadId : datos.getActividadesUsuarias()) {
                Actividad actividad = buscarActividad(actividadId, empresaId);
                if (!actividad.getPool().getProceso().getId().equals(procesoId)) {
                    throw new ReglaNegocioException("La actividad " + actividadId + " no es de este proceso");
                }
                actividades.add(actividadId);
            }
        }
        evento.setActividadesUsuarias(actividades);
    }

    // HU-08: el nombre de una actividad es unico dentro del proceso
    private void verificarNombreUnico(Integer procesoId, String nombre, Integer excluirId) {
        if (nodoFlujoRepository.contarActividadesConNombre(procesoId, nombre.trim(), excluirId) > 0) {
            throw new ConflictoDominioException("Ya existe una actividad con ese nombre en el proceso");
        }
    }

    // HU-10 y HU-16: eliminacion logica; tambien se eliminan los arcos (y los mensajes que salen) del nodo.
    // Devuelve que se elimino y las advertencias que deja el diagrama.
    private Map<String, Object> eliminarNodo(NodoFlujo nodo, Usuario usuario, String tipo) {
        Proceso proceso = procesoService.buscarActivo(nodo.getPool().getProceso().getId(), usuario.getEmpresa().getId());
        Integer id = nodo.getId();

        List<Arco> arcos = arcoRepository.findByOrigenIdOrDestinoId(id, id);
        for (Arco arco : arcos) {
            arco.setActivo(false);
            arcoRepository.save(arco);
            historialService.registrar(usuario, proceso, AccionHistorial.ELIMINAR, "ARCO", arco.getId(),
                    "Arco eliminado junto con '" + nodo.getNombre() + "'");
        }
        List<Mensaje> mensajes = mensajeRepository.findByOrigenId(id);
        for (Mensaje mensaje : mensajes) {
            mensaje.setActivo(false);
            mensajeRepository.save(mensaje);
            historialService.registrar(usuario, proceso, AccionHistorial.ELIMINAR, "MENSAJE", mensaje.getId(),
                    "Mensaje '" + mensaje.getNombre() + "' eliminado junto con '" + nodo.getNombre() + "'");
        }
        nodo.setActivo(false);
        nodoFlujoRepository.save(nodo);
        historialService.registrar(usuario, proceso, AccionHistorial.ELIMINAR, tipo, id,
                "'" + nodo.getNombre() + "' eliminado");
        return validacionService.resultadoEliminacion(proceso, arcos.size(), mensajes.size());
    }
}
