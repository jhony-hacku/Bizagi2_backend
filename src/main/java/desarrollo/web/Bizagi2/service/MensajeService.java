package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.Validaciones.requerido;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import desarrollo.web.Bizagi2.dto.mensaje.MensajeRequest;
import desarrollo.web.Bizagi2.dto.mensaje.MensajeResponse;
import desarrollo.web.Bizagi2.entities.AccionFallo;
import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Actividad;
import desarrollo.web.Bizagi2.entities.Evento;
import desarrollo.web.Bizagi2.entities.Mensaje;
import desarrollo.web.Bizagi2.entities.NodoFlujo;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.TipoActividad;
import desarrollo.web.Bizagi2.entities.TipoEvento;
import desarrollo.web.Bizagi2.entities.TipoParticipante;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.MensajeRepository;
import lombok.RequiredArgsConstructor;

// HU-25 y HU-26: flujo de mensaje que sale de un evento de lanzamiento (o una actividad de envio)
// hacia el pool de otro participante
@Service
@RequiredArgsConstructor
@Transactional
public class MensajeService {

    private final MensajeRepository mensajeRepository;
    private final ProcesoService procesoService;
    private final PoolService poolService;
    private final NodoFlujoService nodoFlujoService;
    private final HistorialService historialService;
    private final ModelMapper modelMapper;

    public List<Mensaje> listarPorProceso(Integer procesoId, Integer empresaId) {
        procesoService.buscarPorId(procesoId, empresaId);
        return mensajeRepository.findByProcesoId(procesoId);
    }

    public Mensaje buscarPorId(Integer id, Integer empresaId) {
        Mensaje mensaje = mensajeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mensaje no encontrado"));
        if (!mensaje.getProceso().getEmpresa().getId().equals(empresaId)) {
            throw new RecursoNoEncontradoException("Mensaje no encontrado");
        }
        return mensaje;
    }

    // Body: nombre, origen: { id }, destinoPool: { id }, contenido, y si el destino es un sistema externo:
    // tipoDestino, momento y accionFallo
    public Mensaje crear(Integer procesoId, Usuario usuario, Mensaje datos) {
        requerido(datos.getNombre(), "nombre");
        Integer empresaId = usuario.getEmpresa().getId();
        Proceso proceso = procesoService.buscarActivo(procesoId, empresaId);
        NodoFlujo origen = origen(datos, empresaId);
        Pool destino = destino(datos, empresaId);
        validar(proceso, origen, destino, datos);

        Mensaje mensaje = new Mensaje();
        mensaje.setProceso(proceso);
        mensaje.setOrigen(origen);
        mensaje.setDestinoPool(destino);
        mensaje.setNombre(datos.getNombre().trim());
        copiarDatosDeEnvio(mensaje, datos, destino, proceso, empresaId);
        mensaje.setActivo(true);
        mensaje = mensajeRepository.save(mensaje);
        historialService.registrar(usuario, proceso, AccionHistorial.CREAR, "MENSAJE", mensaje.getId(),
                "Mensaje '" + mensaje.getNombre() + "' creado de '" + origen.getNombre() + "' al pool '" + destino.getNombre() + "'");
        return mensaje;
    }

    public Mensaje actualizar(Integer id, Usuario usuario, Mensaje datos) {
        requerido(datos.getNombre(), "nombre");
        Integer empresaId = usuario.getEmpresa().getId();
        Mensaje mensaje = buscarPorId(id, empresaId);
        Proceso proceso = procesoService.buscarActivo(mensaje.getProceso().getId(), empresaId);
        NodoFlujo origen = origen(datos, empresaId);
        Pool destino = destino(datos, empresaId);
        validar(proceso, origen, destino, datos);

        mensaje.setOrigen(origen);
        mensaje.setDestinoPool(destino);
        mensaje.setNombre(datos.getNombre().trim());
        copiarDatosDeEnvio(mensaje, datos, destino, proceso, empresaId);
        mensaje = mensajeRepository.save(mensaje);
        historialService.registrar(usuario, proceso, AccionHistorial.EDITAR, "MENSAJE", id,
                "Mensaje '" + mensaje.getNombre() + "' modificado");
        return mensaje;
    }

    // Eliminacion logica (su correlacion se conserva en la base junto con el mensaje)
    public void eliminar(Integer id, Usuario usuario) {
        Integer empresaId = usuario.getEmpresa().getId();
        Mensaje mensaje = buscarPorId(id, empresaId);
        Proceso proceso = procesoService.buscarActivo(mensaje.getProceso().getId(), empresaId);
        mensaje.setActivo(false);
        mensajeRepository.save(mensaje);
        historialService.registrar(usuario, proceso, AccionHistorial.ELIMINAR, "MENSAJE", id,
                "Mensaje '" + mensaje.getNombre() + "' eliminado");
    }

    private NodoFlujo origen(Mensaje datos, Integer empresaId) {
        if (datos.getOrigen() == null || datos.getOrigen().getId() == null) {
            throw new ReglaNegocioException("El mensaje requiere el elemento que lo envia (origen.id)");
        }
        return nodoFlujoService.buscarNodo(datos.getOrigen().getId(), empresaId);
    }

    private Pool destino(Mensaje datos, Integer empresaId) {
        if (datos.getDestinoPool() == null || datos.getDestinoPool().getId() == null) {
            throw new ReglaNegocioException("El mensaje requiere el pool destino (destinoPool.id)");
        }
        return poolService.buscarPorId(datos.getDestinoPool().getId(), empresaId);
    }

    private void validar(Proceso proceso, NodoFlujo origen, Pool destino, Mensaje datos) {
        // Solo envian mensajes un evento de lanzamiento o una actividad de envio
        boolean lanza = origen instanceof Evento e && e.getTipoEvento() == TipoEvento.MENSAJE_LANZAMIENTO;
        boolean envia = origen instanceof Actividad a && a.getTipo() == TipoActividad.ENVIO;
        if (!lanza && !envia) {
            throw new ReglaNegocioException(
                    "Un mensaje solo sale de un evento de lanzamiento de mensaje o de una actividad de tipo ENVIO");
        }
        boolean mismoProceso = origen.getPool().getProceso().getId().equals(proceso.getId())
                && destino.getProceso().getId().equals(proceso.getId());
        if (!mismoProceso) {
            throw new ReglaNegocioException("El origen y el pool destino deben pertenecer al proceso " + proceso.getId());
        }
        // Un flujo de mensaje solo es valido si cruza de un pool a otro
        if (origen.getPool().getId().equals(destino.getId())) {
            throw new ReglaNegocioException(
                    "Un mensaje debe cruzar de un pool a otro. Dentro de un mismo pool use un arco");
        }
        // HU-26: hacia un sistema externo se indica el tipo de destino (correo, servicio web o cola)
        if (destino.getTipoParticipante() == TipoParticipante.SISTEMA_EXTERNO && datos.getTipoDestino() == null) {
            throw new ReglaNegocioException(
                    "Un mensaje hacia un sistema externo requiere tipoDestino (CORREO, SERVICIO_WEB o COLA)");
        }
    }

    private void copiarDatosDeEnvio(Mensaje mensaje, Mensaje datos, Pool destino, Proceso proceso, Integer empresaId) {
        mensaje.setContenido(datos.getContenido());
        boolean externo = destino.getTipoParticipante() == TipoParticipante.SISTEMA_EXTERNO;
        mensaje.setTipoDestino(externo ? datos.getTipoDestino() : null);
        mensaje.setMomento(externo ? datos.getMomento() : null);
        mensaje.setAccionFallo(externo ? datos.getAccionFallo() : null);
        mensaje.setActividadError(externo && datos.getAccionFallo() == AccionFallo.DERIVAR_A_MANEJO_ERROR
                ? actividadDeError(datos, proceso, empresaId) : null);
    }

    // HU-26: si la notificacion falla y se deriva, se indica la actividad de manejo de error (del mismo proceso)
    private Actividad actividadDeError(Mensaje datos, Proceso proceso, Integer empresaId) {
        if (datos.getActividadError() == null || datos.getActividadError().getId() == null) {
            throw new ReglaNegocioException(
                    "Con accionFallo DERIVAR_A_MANEJO_ERROR se debe indicar la actividad de error (actividadErrorId)");
        }
        Actividad actividad = nodoFlujoService.buscarActividad(datos.getActividadError().getId(), empresaId);
        if (!actividad.getPool().getProceso().getId().equals(proceso.getId())) {
            throw new ReglaNegocioException("La actividad de error debe pertenecer al proceso " + proceso.getId());
        }
        return actividad;
    }

    // ---------- Frontera con el controlador: entra y sale DTO, nunca la entidad ----------
    // origenId / destinoPoolId del DTO llegan como mensaje.origen.id / mensaje.destinoPool.id

    public MensajeResponse aResponse(Mensaje mensaje) {
        return modelMapper.map(mensaje, MensajeResponse.class);
    }

    @Transactional(readOnly = true)
    public List<MensajeResponse> listarPorProcesoDto(Integer procesoId, Integer empresaId) {
        return listarPorProceso(procesoId, empresaId).stream().map(this::aResponse).toList();
    }

    @Transactional(readOnly = true)
    public MensajeResponse buscarPorIdDto(Integer id, Integer empresaId) {
        return aResponse(buscarPorId(id, empresaId));
    }

    public MensajeResponse crear(Integer procesoId, Usuario usuario, MensajeRequest datos) {
        return aResponse(crear(procesoId, usuario, modelMapper.map(datos, Mensaje.class)));
    }

    public MensajeResponse actualizar(Integer id, Usuario usuario, MensajeRequest datos) {
        return aResponse(actualizar(id, usuario, modelMapper.map(datos, Mensaje.class)));
    }
}
