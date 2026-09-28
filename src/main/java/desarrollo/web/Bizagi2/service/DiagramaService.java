package desarrollo.web.Bizagi2.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import desarrollo.web.Bizagi2.entities.Actividad;
import desarrollo.web.Bizagi2.entities.Arco;
import desarrollo.web.Bizagi2.entities.Evento;
import desarrollo.web.Bizagi2.entities.Gateway;
import desarrollo.web.Bizagi2.entities.Lane;
import desarrollo.web.Bizagi2.entities.Mensaje;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.repository.ArcoRepository;
import desarrollo.web.Bizagi2.repository.CorrelacionRepository;
import desarrollo.web.Bizagi2.repository.LaneRepository;
import desarrollo.web.Bizagi2.repository.MensajeRepository;
import desarrollo.web.Bizagi2.repository.NodoFlujoRepository;
import desarrollo.web.Bizagi2.repository.PoolRepository;
import lombok.RequiredArgsConstructor;

// HU-07: al abrir un proceso se entrega su diagrama completo en una sola respuesta:
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

    public Map<String, Object> obtener(Integer procesoId, Integer empresaId) {
        Proceso proceso = procesoService.buscarParaLectura(procesoId, empresaId);
        boolean propio = proceso.getEmpresa().getId().equals(empresaId);

        List<Object> pools = new ArrayList<>();
        for (Pool pool : poolRepository.findByProcesoId(procesoId)) {
            pools.add(mapa(
                    "id", pool.getId(),
                    "nombre", pool.getNombre(),
                    "tipoParticipante", pool.getTipoParticipante(),
                    "lanes", laneRepository.findByPoolIdOrderByOrdenAscIdAsc(pool.getId()).stream().map(l -> lane(l, propio)).toList(),
                    "actividades", nodoFlujoRepository.findActividadesByPool(pool.getId()).stream().map(a -> actividad(a, propio)).toList(),
                    "gateways", nodoFlujoRepository.findGatewaysByPool(pool.getId()).stream().map(this::gateway).toList(),
                    "eventos", nodoFlujoRepository.findEventosByPool(pool.getId()).stream().map(this::evento).toList(),
                    "arcos", arcoRepository.findByPoolId(pool.getId()).stream().map(this::arco).toList()));
        }

        Map<String, Object> diagrama = mapa(
                "proceso", mapa(
                        "id", proceso.getId(),
                        "nombre", proceso.getNombre(),
                        "descripcion", proceso.getDescripcion(),
                        "categoria", proceso.getCategoria(),
                        "estado", proceso.getEstado(),
                        "activo", proceso.isActivo(),
                        "propio", propio),
                "pools", pools,
                "mensajes", mensajeRepository.findByProcesoId(procesoId).stream().map(this::mensaje).toList());
        return diagrama;
    }

    private Map<String, Object> lane(Lane lane, boolean propio) {
        Map<String, Object> m = mapa("id", lane.getId(), "nombre", lane.getNombre(), "orden", lane.getOrden());
        if (propio) {
            m.put("rolProcesoId", lane.getRolProceso().getId());
            m.put("rolProceso", lane.getRolProceso().getNombre());
        }
        return m;
    }

    private Map<String, Object> actividad(Actividad a, boolean propio) {
        Map<String, Object> m = mapa("id", a.getId(), "nombre", a.getNombre(), "tipo", a.getTipo(),
                "descripcion", a.getDescripcion(), "posicionX", a.getPosicionX(), "posicionY", a.getPosicionY(),
                "laneId", a.getLane().getId());
        if (propio) {
            m.put("rolProcesoId", a.getLane().getRolProceso().getId());
        }
        return m;
    }

    private Map<String, Object> gateway(Gateway g) {
        return mapa("id", g.getId(), "nombre", g.getNombre(), "tipoGateway", g.getTipoGateway(),
                "posicionX", g.getPosicionX(), "posicionY", g.getPosicionY());
    }

    private Map<String, Object> evento(Evento e) {
        return mapa("id", e.getId(), "nombre", e.getNombre(), "tipoEvento", e.getTipoEvento(),
                "contenido", e.getContenido(), "origenExterno", e.getOrigenExterno(),
                "claveCorrelacion", e.getClaveCorrelacion(), "actividadesUsuarias", e.getActividadesUsuarias(),
                "posicionX", e.getPosicionX(), "posicionY", e.getPosicionY());
    }

    private Map<String, Object> arco(Arco a) {
        return mapa("id", a.getId(), "origenId", a.getOrigen().getId(), "destinoId", a.getDestino().getId(),
                "etiqueta", a.getEtiqueta(), "condicion", a.getCondicion());
    }

    private Map<String, Object> mensaje(Mensaje m) {
        Map<String, Object> correlacion = correlacionRepository.findByMensajeId(m.getId())
                .map(c -> mapa("criterio", c.getCriterio(), "accionSinCaso", c.getAccionSinCaso())).orElse(null);
        return mapa("id", m.getId(), "nombre", m.getNombre(), "origenId", m.getOrigen().getId(),
                "origenPoolId", m.getOrigen().getPool().getId(), "destinoPoolId", m.getDestinoPool().getId(),
                "contenido", m.getContenido(), "tipoDestino", m.getTipoDestino(), "momento", m.getMomento(),
                "accionFallo", m.getAccionFallo(), "correlacion", correlacion);
    }

    // Arma un JSON ordenado a partir de pares clave, valor
    private Map<String, Object> mapa(Object... pares) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            mapa.put((String) pares[i], pares[i + 1]);
        }
        return mapa;
    }
}
