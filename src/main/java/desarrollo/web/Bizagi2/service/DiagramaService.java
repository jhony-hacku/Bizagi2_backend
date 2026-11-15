package desarrollo.web.Bizagi2.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import desarrollo.web.Bizagi2.dto.diagrama.DiagramaActividadResponse;
import desarrollo.web.Bizagi2.dto.diagrama.DiagramaArcoResponse;
import desarrollo.web.Bizagi2.dto.diagrama.DiagramaCorrelacionResponse;
import desarrollo.web.Bizagi2.dto.diagrama.DiagramaLaneResponse;
import desarrollo.web.Bizagi2.dto.diagrama.DiagramaMensajeResponse;
import desarrollo.web.Bizagi2.dto.diagrama.DiagramaPoolResponse;
import desarrollo.web.Bizagi2.dto.diagrama.DiagramaProcesoResponse;
import desarrollo.web.Bizagi2.dto.diagrama.DiagramaResponse;
import desarrollo.web.Bizagi2.dto.nodo.EventoResponse;
import desarrollo.web.Bizagi2.dto.nodo.GatewayResponse;
import desarrollo.web.Bizagi2.entities.Actividad;
import desarrollo.web.Bizagi2.entities.Arco;
import desarrollo.web.Bizagi2.entities.Evento;
import desarrollo.web.Bizagi2.entities.Gateway;
import desarrollo.web.Bizagi2.entities.Lane;
import desarrollo.web.Bizagi2.entities.Mensaje;
import desarrollo.web.Bizagi2.entities.NodoFlujo;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.repository.ArcoRepository;
import desarrollo.web.Bizagi2.repository.CorrelacionRepository;
import desarrollo.web.Bizagi2.repository.LaneRepository;
import desarrollo.web.Bizagi2.repository.MensajeRepository;
import desarrollo.web.Bizagi2.repository.NodoFlujoRepository;
import desarrollo.web.Bizagi2.repository.PoolRepository;
import lombok.RequiredArgsConstructor;

// HU-07 (DTOs en dto/diagrama): al abrir un proceso se entrega su diagrama completo en una sola respuesta:
// pools con sus lanes, actividades, gateways, eventos y arcos, mas los mensajes entre pools.
// Si el proceso se consulta como compartido (HU-23) no se incluyen los roles de la empresa propietaria.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiagramaService {

    private final ProcesoService procesoService;
    private final PoolRepository poolRepository;
    private final LaneRepository laneRepository;
    private final NodoFlujoRepository nodoFlujoRepository;
    private final ArcoRepository arcoRepository;
    private final MensajeRepository mensajeRepository;
    private final CorrelacionRepository correlacionRepository;

    public DiagramaResponse obtener(Integer procesoId, Integer empresaId) {
        Proceso proceso = procesoService.buscarParaLectura(procesoId, empresaId);
        boolean propio = proceso.getEmpresa().getId().equals(empresaId);

        List<DiagramaPoolResponse> pools = new ArrayList<>();
        for (Pool pool : poolRepository.findByProcesoId(procesoId)) {
            pools.add(new DiagramaPoolResponse(
                    pool.getId(),
                    pool.getNombre(),
                    pool.getTipoParticipante(),
                    laneRepository.findByPoolIdOrderByOrdenAscIdAsc(pool.getId()).stream().map(l -> lane(l, propio)).toList(),
                    nodoFlujoRepository.findActividadesByPool(pool.getId()).stream().map(a -> actividad(a, propio)).toList(),
                    nodoFlujoRepository.findGatewaysByPool(pool.getId()).stream().map(g -> gateway(g, pool.getId())).toList(),
                    nodoFlujoRepository.findEventosByPool(pool.getId()).stream().map(e -> evento(e, pool.getId())).toList(),
                    arcoRepository.findByPoolId(pool.getId()).stream().map(this::arco).toList()));
        }

        DiagramaProcesoResponse datosProceso = new DiagramaProcesoResponse(
                proceso.getId(),
                proceso.getNombre(),
                proceso.getDescripcion(),
                proceso.getCategoria(),
                proceso.getEstado(),
                proceso.isActivo(),
                propio);

        return new DiagramaResponse(datosProceso, pools,
                mensajeRepository.findByProcesoId(procesoId).stream().map(this::mensaje).toList());
    }

    // Si el proceso es compartido, los datos del rol quedan en null y no se envian (@JsonInclude NON_NULL)
    private DiagramaLaneResponse lane(Lane lane, boolean propio) {
        return new DiagramaLaneResponse(lane.getId(), lane.getNombre(), lane.getOrden(),
                propio ? lane.getRolProceso().getId() : null,
                propio ? lane.getRolProceso().getNombre() : null);
    }

    private DiagramaActividadResponse actividad(Actividad a, boolean propio) {
        return new DiagramaActividadResponse(a.getId(), a.getNombre(), a.getTipo(), a.getDescripcion(),
                a.getPosicionX(), a.getPosicionY(), a.getLane().getId(),
                propio ? a.getLane().getRolProceso().getId() : null);
    }

    // El pool se recibe porque los nodos se consultaron por pool
    private GatewayResponse gateway(Gateway g, Integer poolId) {
        return new GatewayResponse(g.getId(), g.getNombre(), g.getTipoGateway(), g.getPosicionX(), g.getPosicionY(),
                poolId);
    }

    private Set<Integer> idsDe(Set<Actividad> actividades) {
        return actividades.stream().map(NodoFlujo::getId).collect(Collectors.toSet());
    }

    private EventoResponse evento(Evento e, Integer poolId) {
        return new EventoResponse(e.getId(), e.getNombre(), e.getTipoEvento(), e.getPosicionX(), e.getPosicionY(),
                e.getContenido(), e.getOrigenExterno(), e.getClaveCorrelacion(), idsDe(e.getActividadesUsuarias()),
                poolId);
    }

    private DiagramaArcoResponse arco(Arco a) {
        return new DiagramaArcoResponse(a.getId(), a.getOrigen().getId(), a.getDestino().getId(),
                a.getEtiqueta(), a.getCondicion());
    }

    private DiagramaMensajeResponse mensaje(Mensaje m) {
        DiagramaCorrelacionResponse correlacion = correlacionRepository.findByMensajeId(m.getId())
                .map(c -> new DiagramaCorrelacionResponse(c.getCriterio(), c.getAccionSinCaso()))
                .orElse(null);
        return new DiagramaMensajeResponse(m.getId(), m.getNombre(), m.getOrigen().getId(),
                m.getOrigen().getPool().getId(), m.getDestinoPool().getId(), m.getContenido(),
                m.getTipoDestino(), m.getMomento(), m.getAccionFallo(),
                m.getActividadError() != null ? m.getActividadError().getId() : null, correlacion);
    }
}
