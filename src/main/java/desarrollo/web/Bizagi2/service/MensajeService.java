package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.Validaciones.requerido;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public List<Mensaje> listarPorProceso(Integer procesoId, Integer empresaId) {
        procesoService.buscarPorId(procesoId, empresaId);
        return mensajeRepository.findByProcesoId(procesoId);
    }

    @Transactional(readOnly = true)
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
        copiarDatosDeEnvio(mensaje, datos, destino);
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
        copiarDatosDeEnvio(mensaje, datos, destino);
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

    private void copiarDatosDeEnvio(Mensaje mensaje, Mensaje datos, Pool destino) {
        mensaje.setContenido(datos.getContenido());
        boolean externo = destino.getTipoParticipante() == TipoParticipante.SISTEMA_EXTERNO;
        mensaje.setTipoDestino(externo ? datos.getTipoDestino() : null);
        mensaje.setMomento(externo ? datos.getMomento() : null);
        mensaje.setAccionFallo(externo ? datos.getAccionFallo() : null);
    }
}
