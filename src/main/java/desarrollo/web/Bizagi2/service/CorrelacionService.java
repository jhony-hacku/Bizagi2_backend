package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.Validaciones.requerido;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import desarrollo.web.Bizagi2.dto.correlacion.CorrelacionRequest;
import desarrollo.web.Bizagi2.dto.correlacion.CorrelacionResponse;
import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Correlacion;
import desarrollo.web.Bizagi2.entities.Mensaje;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.repository.CorrelacionRepository;
import lombok.RequiredArgsConstructor;

// HU-28: cada mensaje tiene como maximo una clave de correlacion, por eso todo se identifica por el id del mensaje
@Service
@RequiredArgsConstructor
@Transactional
public class CorrelacionService {

    private final CorrelacionRepository correlacionRepository;
    private final MensajeService mensajeService;
    private final ProcesoService procesoService;
    private final HistorialService historialService;
    private final ModelMapper modelMapper;

    public Correlacion obtener(Integer mensajeId, Integer empresaId) {
        mensajeService.buscarPorId(mensajeId, empresaId);
        return correlacionRepository.findByMensajeId(mensajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El mensaje no tiene clave de correlacion"));
    }

    // Body: criterio (la clave) y accionSinCaso (DESCARTAR o INICIAR_CASO_NUEVO)
    public Correlacion crear(Integer mensajeId, Usuario usuario, Correlacion datos) {
        requerido(datos.getCriterio(), "criterio");
        requerido(datos.getAccionSinCaso(), "accionSinCaso");
        Integer empresaId = usuario.getEmpresa().getId();
        Mensaje mensaje = mensajeService.buscarPorId(mensajeId, empresaId);
        Proceso proceso = procesoService.buscarActivo(mensaje.getProceso().getId(), empresaId);
        if (correlacionRepository.findByMensajeId(mensajeId).isPresent()) {
            throw new ConflictoDominioException("El mensaje ya tiene una clave de correlacion; actualizala");
        }
        Correlacion correlacion = new Correlacion();
        correlacion.setMensaje(mensaje);
        correlacion.setCriterio(datos.getCriterio().trim());
        correlacion.setAccionSinCaso(datos.getAccionSinCaso());
        correlacion = correlacionRepository.save(correlacion);
        historialService.registrar(usuario, proceso, AccionHistorial.CREAR, "CORRELACION", correlacion.getId(),
                "Clave de correlacion '" + correlacion.getCriterio() + "' definida para el mensaje '" + mensaje.getNombre() + "'");
        return correlacion;
    }

    public Correlacion actualizar(Integer mensajeId, Usuario usuario, Correlacion datos) {
        requerido(datos.getCriterio(), "criterio");
        requerido(datos.getAccionSinCaso(), "accionSinCaso");
        Correlacion correlacion = obtener(mensajeId, usuario.getEmpresa().getId());
        Proceso proceso = procesoService.buscarActivo(correlacion.getMensaje().getProceso().getId(), usuario.getEmpresa().getId());
        correlacion.setCriterio(datos.getCriterio().trim());
        correlacion.setAccionSinCaso(datos.getAccionSinCaso());
        correlacion = correlacionRepository.save(correlacion);
        historialService.registrar(usuario, proceso, AccionHistorial.EDITAR, "CORRELACION", correlacion.getId(),
                "Clave de correlacion del mensaje '" + correlacion.getMensaje().getNombre() + "' modificada");
        return correlacion;
    }

    public void eliminar(Integer mensajeId, Usuario usuario) {
        Correlacion correlacion = obtener(mensajeId, usuario.getEmpresa().getId());
        Proceso proceso = procesoService.buscarActivo(correlacion.getMensaje().getProceso().getId(), usuario.getEmpresa().getId());
        correlacionRepository.delete(correlacion);
        historialService.registrar(usuario, proceso, AccionHistorial.ELIMINAR, "CORRELACION", correlacion.getId(),
                "Clave de correlacion del mensaje '" + correlacion.getMensaje().getNombre() + "' eliminada");
    }

    // ---------- Frontera con el controlador: entra y sale DTO, nunca la entidad ----------

    public CorrelacionResponse aResponse(Correlacion correlacion) {
        return modelMapper.map(correlacion, CorrelacionResponse.class);
    }

    @Transactional(readOnly = true)
    public CorrelacionResponse obtenerDto(Integer mensajeId, Integer empresaId) {
        return aResponse(obtener(mensajeId, empresaId));
    }

    public CorrelacionResponse crear(Integer mensajeId, Usuario usuario, CorrelacionRequest datos) {
        return aResponse(crear(mensajeId, usuario, modelMapper.map(datos, Correlacion.class)));
    }

    public CorrelacionResponse actualizar(Integer mensajeId, Usuario usuario, CorrelacionRequest datos) {
        return aResponse(actualizar(mensajeId, usuario, modelMapper.map(datos, Correlacion.class)));
    }
}
