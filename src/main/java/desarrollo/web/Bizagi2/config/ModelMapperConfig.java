package desarrollo.web.Bizagi2.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import desarrollo.web.Bizagi2.dto.arco.ArcoRequest;
import desarrollo.web.Bizagi2.dto.arco.ArcoResponse;
import desarrollo.web.Bizagi2.dto.correlacion.CorrelacionResponse;
import desarrollo.web.Bizagi2.dto.lane.LaneCreateRequest;
import desarrollo.web.Bizagi2.dto.lane.LaneResponse;
import desarrollo.web.Bizagi2.dto.lane.LaneUpdateRequest;
import desarrollo.web.Bizagi2.dto.mensaje.MensajeRequest;
import desarrollo.web.Bizagi2.dto.mensaje.MensajeResponse;
import desarrollo.web.Bizagi2.dto.nodo.ActividadResponse;
import desarrollo.web.Bizagi2.dto.nodo.ActividadUpdateRequest;
import desarrollo.web.Bizagi2.dto.nodo.EventoResponse;
import desarrollo.web.Bizagi2.dto.nodo.GatewayResponse;
import desarrollo.web.Bizagi2.dto.pool.PoolResponse;
import desarrollo.web.Bizagi2.dto.proceso.HistorialResponse;
import desarrollo.web.Bizagi2.dto.proceso.ProcesoResponse;
import desarrollo.web.Bizagi2.dto.usuario.UsuarioResponse;
import desarrollo.web.Bizagi2.entities.Actividad;
import desarrollo.web.Bizagi2.entities.Arco;
import desarrollo.web.Bizagi2.entities.Correlacion;
import desarrollo.web.Bizagi2.entities.Evento;
import desarrollo.web.Bizagi2.entities.Gateway;
import desarrollo.web.Bizagi2.entities.Historial;
import desarrollo.web.Bizagi2.entities.Lane;
import desarrollo.web.Bizagi2.entities.Mensaje;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.Usuario;

// Un solo ModelMapper para toda la aplicacion. Los servicios lo usan para convertir
// entidades JPA en DTOs (y DTOs de entrada en entidades) en la frontera del Service.
@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();

        // STRICT: solo se copian las propiedades con el mismo nombre (nombre -> nombre, tipo -> tipo...).
        // Evita mapeos accidentales, por ejemplo que "rolProcesoId" termine en el id de la lane.
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        // Los campos que no vienen en el request (null) no se copian: la entidad conserva su valor
        // por defecto (por ejemplo posicionX = 0). Ver "Ignorar nulos" en el material del curso.
        mapper.getConfiguration().setSkipNullEnabled(true);

        // Los campos que en el DTO viajan "planos" (empresaId, poolId, origenId...) se declaran
        // explicitamente con TypeMap (ver Contenido > Capas > DTO > Mapeos personalizados).
        respuestas(mapper);
        solicitudes(mapper);
        return mapper;
    }

    // Entidad -> DTO de respuesta: de la relacion se envia solo el id (y el nombre cuando ayuda)
    private void respuestas(ModelMapper mapper) {
        mapper.typeMap(Usuario.class, UsuarioResponse.class).addMappings(m ->
                m.map(src -> src.getEmpresa().getId(), UsuarioResponse::setEmpresaId));

        mapper.typeMap(Proceso.class, ProcesoResponse.class).addMappings(m ->
                m.map(src -> src.getEmpresa().getId(), ProcesoResponse::setEmpresaId));

        mapper.typeMap(Pool.class, PoolResponse.class).addMappings(m ->
                m.map(src -> src.getProceso().getId(), PoolResponse::setProcesoId));

        mapper.typeMap(Lane.class, LaneResponse.class).addMappings(m -> {
            m.map(src -> src.getPool().getId(), LaneResponse::setPoolId);
            m.map(src -> src.getRolProceso().getId(), LaneResponse::setRolProcesoId);
            m.map(src -> src.getRolProceso().getNombre(), LaneResponse::setRolProcesoNombre);
        });

        mapper.typeMap(Actividad.class, ActividadResponse.class).addMappings(m -> {
            m.map(src -> src.getPool().getId(), ActividadResponse::setPoolId);
            m.map(src -> src.getLane().getId(), ActividadResponse::setLaneId);
        });

        mapper.typeMap(Gateway.class, GatewayResponse.class).addMappings(m ->
                m.map(src -> src.getPool().getId(), GatewayResponse::setPoolId));

        mapper.typeMap(Evento.class, EventoResponse.class).addMappings(m ->
                m.map(src -> src.getPool().getId(), EventoResponse::setPoolId));

        mapper.typeMap(Arco.class, ArcoResponse.class).addMappings(m -> {
            m.map(src -> src.getPool().getId(), ArcoResponse::setPoolId);
            m.map(src -> src.getOrigen().getId(), ArcoResponse::setOrigenId);
            m.map(src -> src.getOrigen().getNombre(), ArcoResponse::setOrigenNombre);
            m.map(src -> src.getDestino().getId(), ArcoResponse::setDestinoId);
            m.map(src -> src.getDestino().getNombre(), ArcoResponse::setDestinoNombre);
        });

        mapper.typeMap(Mensaje.class, MensajeResponse.class).addMappings(m -> {
            m.map(src -> src.getProceso().getId(), MensajeResponse::setProcesoId);
            m.map(src -> src.getOrigen().getId(), MensajeResponse::setOrigenId);
            m.map(src -> src.getOrigen().getNombre(), MensajeResponse::setOrigenNombre);
            m.map(src -> src.getDestinoPool().getId(), MensajeResponse::setDestinoPoolId);
            m.map(src -> src.getDestinoPool().getNombre(), MensajeResponse::setDestinoPoolNombre);
            m.map(src -> src.getActividadError().getId(), MensajeResponse::setActividadErrorId);
            m.map(src -> src.getActividadError().getNombre(), MensajeResponse::setActividadErrorNombre);
        });

        mapper.typeMap(Correlacion.class, CorrelacionResponse.class).addMappings(m ->
                m.map(src -> src.getMensaje().getId(), CorrelacionResponse::setMensajeId));

        mapper.typeMap(Historial.class, HistorialResponse.class).addMappings(m -> {
            m.map(src -> src.getUsuario().getId(), HistorialResponse::setUsuarioId);
            m.map(src -> src.getUsuario().getNombre(), HistorialResponse::setUsuarioNombre);
        });
    }

    // DTO de solicitud -> entidad: el id de referencia llega a la relacion (lane.rolProceso.id...).
    // El servicio despues busca la entidad real por ese id y valida que sea de la empresa.
    private void solicitudes(ModelMapper mapper) {
        mapper.typeMap(LaneCreateRequest.class, Lane.class).addMappings(m ->
                m.<Integer>map(LaneCreateRequest::getRolProcesoId, (dest, v) -> dest.getRolProceso().setId(v)));

        mapper.typeMap(LaneUpdateRequest.class, Lane.class).addMappings(m ->
                m.<Integer>map(LaneUpdateRequest::getRolProcesoId, (dest, v) -> dest.getRolProceso().setId(v)));

        mapper.typeMap(ActividadUpdateRequest.class, Actividad.class).addMappings(m ->
                m.<Integer>map(ActividadUpdateRequest::getLaneId, (dest, v) -> dest.getLane().setId(v)));

        mapper.typeMap(ArcoRequest.class, Arco.class).addMappings(m -> {
            m.<Integer>map(ArcoRequest::getOrigenId, (dest, v) -> dest.getOrigen().setId(v));
            m.<Integer>map(ArcoRequest::getDestinoId, (dest, v) -> dest.getDestino().setId(v));
        });

        mapper.typeMap(MensajeRequest.class, Mensaje.class).addMappings(m -> {
            m.<Integer>map(MensajeRequest::getOrigenId, (dest, v) -> dest.getOrigen().setId(v));
            m.<Integer>map(MensajeRequest::getDestinoPoolId, (dest, v) -> dest.getDestinoPool().setId(v));
            m.<Integer>map(MensajeRequest::getActividadErrorId, (dest, v) -> dest.getActividadError().setId(v));
        });
    }
}
