package desarrollo.web.Bizagi2.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import desarrollo.web.Bizagi2.entities.Arco;
import desarrollo.web.Bizagi2.entities.Correlacion;
import desarrollo.web.Bizagi2.entities.Evento;
import desarrollo.web.Bizagi2.entities.Gateway;
import desarrollo.web.Bizagi2.entities.Mensaje;
import desarrollo.web.Bizagi2.entities.NodoFlujo;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.TipoEvento;
import desarrollo.web.Bizagi2.entities.TipoGateway;
import desarrollo.web.Bizagi2.entities.TipoParticipante;
import desarrollo.web.Bizagi2.repository.ArcoRepository;
import desarrollo.web.Bizagi2.repository.CorrelacionRepository;
import desarrollo.web.Bizagi2.repository.MensajeRepository;
import desarrollo.web.Bizagi2.repository.NodoFlujoRepository;
import desarrollo.web.Bizagi2.repository.PoolRepository;
import lombok.RequiredArgsConstructor;

// Revisa la coherencia del diagrama. Son las "advertencias del editor" de las historias de usuario.
// nivel ERROR: incumple una regla que se exige al PUBLICAR (en borrador se puede trabajar incompleto).
// nivel ADVERTENCIA: algo dudoso que el editor avisa pero no bloquea.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ValidacionService {

    public static final String ERROR = "ERROR";
    public static final String ADVERTENCIA = "ADVERTENCIA";

    private final PoolRepository poolRepository;
    private final NodoFlujoRepository nodoFlujoRepository;
    private final ArcoRepository arcoRepository;
    private final MensajeRepository mensajeRepository;
    private final CorrelacionRepository correlacionRepository;

    public List<Map<String, Object>> validar(Proceso proceso) {
        List<Map<String, Object>> items = new ArrayList<>();
        List<Pool> pools = poolRepository.findByProcesoId(proceso.getId());
        List<Evento> recepciones = new ArrayList<>();
        Map<Integer, Integer> entrantesPorNodo = new HashMap<>();

        for (Pool pool : pools) {
            if (pool.getTipoParticipante() != TipoParticipante.EMPRESA_PROPIETARIA) {
                continue; // los participantes externos son cajas negras
            }
            List<NodoFlujo> nodos = nodoFlujoRepository.findByPoolId(pool.getId());
            List<Arco> arcos = arcoRepository.findByPoolId(pool.getId());
            Map<Integer, List<Arco>> salientes = new HashMap<>();
            Map<Integer, Integer> entrantes = new HashMap<>();
            for (Arco arco : arcos) {
                salientes.computeIfAbsent(arco.getOrigen().getId(), k -> new ArrayList<>()).add(arco);
                entrantes.merge(arco.getDestino().getId(), 1, Integer::sum);
            }
            entrantesPorNodo.putAll(entrantes);

            for (NodoFlujo nodo : nodos) {
                int nEntrantes = entrantes.getOrDefault(nodo.getId(), 0);
                List<Arco> misSalientes = salientes.getOrDefault(nodo.getId(), List.of());
                String tipo = tipoDe(nodo);

                // HU-10 y HU-13: elementos desconectados
                boolean esInicio = nodo instanceof Evento e
                        && (e.getTipoEvento() == TipoEvento.INICIO || e.getTipoEvento() == TipoEvento.MENSAJE_RECEPCION_INICIO);
                boolean esFin = nodo instanceof Evento e && e.getTipoEvento() == TipoEvento.FIN;
                if (nEntrantes == 0 && !esInicio) {
                    items.add(item(ADVERTENCIA, tipo, nodo.getId(),
                            "'" + nodo.getNombre() + "' no tiene camino de entrada"));
                }
                if (misSalientes.isEmpty() && !esFin) {
                    items.add(item(ADVERTENCIA, tipo, nodo.getId(),
                            "'" + nodo.getNombre() + "' no tiene camino de salida"));
                }

                if (nodo instanceof Gateway gateway) {
                    validarGateway(items, gateway, nEntrantes, misSalientes);
                }
                if (nodo instanceof Evento evento) {
                    if (evento.getTipoEvento() == TipoEvento.MENSAJE_RECEPCION_INICIO
                            || evento.getTipoEvento() == TipoEvento.MENSAJE_RECEPCION_INTERMEDIO) {
                        recepciones.add(evento);
                    }
                    if (evento.getTipoEvento() == TipoEvento.MENSAJE_RECEPCION_INICIO && nEntrantes > 0) {
                        items.add(item(ERROR, "EVENTO", evento.getId(),
                                "El Message Catch de inicio '" + evento.getNombre() + "' no puede tener arcos entrantes"));
                    }
                }
            }
        }
        validarMensajes(items, proceso, pools, recepciones);
        return items;
    }

    // Respuesta de las eliminaciones de nodos y arcos: que se elimino y que advierte ahora el editor
    public Map<String, Object> resultadoEliminacion(Proceso proceso, int arcosEliminados, int mensajesEliminados) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("arcosEliminados", arcosEliminados);
        resultado.put("mensajesEliminados", mensajesEliminados);
        resultado.put("advertencias", validar(proceso));
        return resultado;
    }

    // Solo los textos de los errores: lo usa el proceso al pasar a PUBLICADO
    public List<String> errores(Proceso proceso) {
        return validar(proceso).stream()
                .filter(i -> ERROR.equals(i.get("nivel")))
                .map(i -> (String) i.get("mensaje"))
                .toList();
    }

    private void validarGateway(List<Map<String, Object>> items, Gateway gateway, int nEntrantes, List<Arco> salientes) {
        // HU-14: un gateway de divergencia tiene al menos dos arcos salientes (o es de convergencia)
        boolean convergencia = nEntrantes >= 2 && salientes.size() == 1;
        if (salientes.size() < 2 && !convergencia) {
            items.add(item(ERROR, "GATEWAY", gateway.getId(),
                    "El gateway '" + gateway.getNombre() + "' debe tener al menos dos arcos salientes"));
        }
        // HU-14: en un gateway exclusivo o inclusivo cada arco saliente lleva una condicion
        boolean condicional = gateway.getTipoGateway() == TipoGateway.EXCLUSIVA
                || gateway.getTipoGateway() == TipoGateway.INCLUSIVA;
        if (condicional && salientes.size() >= 2) {
            for (Arco arco : salientes) {
                if (arco.getCondicion() == null || arco.getCondicion().isBlank()) {
                    items.add(item(ERROR, "ARCO", arco.getId(),
                            "El arco " + arco.getId() + " que sale del gateway '" + gateway.getNombre()
                                    + "' debe tener una condicion"));
                }
            }
        }
        // HU-15: en un exclusivo las condiciones deben excluirse entre si (aqui se detectan las repetidas)
        if (gateway.getTipoGateway() == TipoGateway.EXCLUSIVA) {
            Map<String, Integer> vistas = new HashMap<>();
            for (Arco arco : salientes) {
                if (arco.getCondicion() != null && !arco.getCondicion().isBlank()) {
                    vistas.merge(arco.getCondicion().trim().toLowerCase(), 1, Integer::sum);
                }
            }
            if (vistas.values().stream().anyMatch(n -> n > 1)) {
                items.add(item(ADVERTENCIA, "GATEWAY", gateway.getId(),
                        "El gateway exclusivo '" + gateway.getNombre() + "' tiene condiciones repetidas: no son mutuamente excluyentes"));
            }
        }
    }

    private void validarMensajes(List<Map<String, Object>> items, Proceso proceso, List<Pool> pools, List<Evento> recepciones) {
        List<Mensaje> mensajes = mensajeRepository.findByProcesoId(proceso.getId());
        Map<Integer, Correlacion> correlaciones = new HashMap<>();
        Map<String, Integer> nombreYClave = new HashMap<>();
        for (Mensaje m : mensajes) {
            Correlacion c = correlacionRepository.findByMensajeId(m.getId()).orElse(null);
            if (c != null) {
                correlaciones.put(m.getId(), c);
                nombreYClave.merge(clave(m.getNombre(), c.getCriterio()), 1, Integer::sum);
            }
        }

        for (Mensaje m : mensajes) {
            Correlacion c = correlaciones.get(m.getId());
            // HU-25: el nombre del mensaje debe corresponder a un Message Catch del pool destino
            boolean destinoInterno = m.getDestinoPool().getTipoParticipante() == TipoParticipante.EMPRESA_PROPIETARIA;
            boolean hayReceptor = recepciones.stream().anyMatch(e -> e.getPool().getId().equals(m.getDestinoPool().getId())
                    && mismoNombre(e.getNombre(), m.getNombre()));
            if (destinoInterno && !hayReceptor) {
                items.add(item(ADVERTENCIA, "MENSAJE", m.getId(),
                        "El mensaje '" + m.getNombre() + "' queda sin receptor en el pool '" + m.getDestinoPool().getNombre() + "'"));
            }
            // HU-28: cada mensaje declara una clave de correlacion
            if (c == null) {
                items.add(item(ADVERTENCIA, "MENSAJE", m.getId(),
                        "El mensaje '" + m.getNombre() + "' no tiene clave de correlacion"));
            } else if (nombreYClave.get(clave(m.getNombre(), c.getCriterio())) > 1) {
                // HU-28: dos mensajes con el mismo nombre y la misma clave son ambiguos
                items.add(item(ADVERTENCIA, "MENSAJE", m.getId(),
                        "El mensaje '" + m.getNombre() + "' es ambiguo: otro mensaje comparte su nombre y su clave de correlacion"));
            }
        }

        for (Evento e : recepciones) {
            List<Mensaje> lanzados = mensajes.stream().filter(m -> mismoNombre(m.getNombre(), e.getNombre())).toList();
            // HU-27: debe existir un Message Throw con ese nombre o estar marcado como de origen externo
            if (lanzados.isEmpty() && !Boolean.TRUE.equals(e.getOrigenExterno())) {
                items.add(item(ADVERTENCIA, "EVENTO", e.getId(),
                        "No existe un mensaje lanzado con el nombre '" + e.getNombre()
                                + "' ni se marco el evento como de origen externo"));
            }
            boolean sinClave = e.getClaveCorrelacion() == null || e.getClaveCorrelacion().isBlank();
            // HU-28: un Message Catch intermedio sin clave de correlacion
            if (e.getTipoEvento() == TipoEvento.MENSAJE_RECEPCION_INTERMEDIO && sinClave) {
                items.add(item(ADVERTENCIA, "EVENTO", e.getId(),
                        "El Message Catch intermedio '" + e.getNombre() + "' no tiene clave de correlacion"));
            }
            // HU-28: la clave debe ser la misma en el lanzamiento y en la recepcion
            for (Mensaje m : lanzados) {
                Correlacion c = correlaciones.get(m.getId());
                if (c != null && !sinClave && c.getCriterio() != null && !c.getCriterio().trim().equalsIgnoreCase(e.getClaveCorrelacion().trim())) {
                    items.add(item(ADVERTENCIA, "EVENTO", e.getId(),
                            "La clave de correlacion de '" + e.getNombre() + "' (" + e.getClaveCorrelacion()
                                    + ") no coincide con la del mensaje lanzado (" + c.getCriterio() + ")"));
                }
            }
        }
    }

    private String tipoDe(NodoFlujo nodo) {
        if (nodo instanceof Gateway) {
            return "GATEWAY";
        }
        return nodo instanceof Evento ? "EVENTO" : "ACTIVIDAD";
    }

    private boolean mismoNombre(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    private String clave(String nombre, String criterio) {
        return Objects.toString(nombre, "").trim().toLowerCase() + "|" + Objects.toString(criterio, "").trim().toLowerCase();
    }

    private Map<String, Object> item(String nivel, String elemento, Integer id, String mensaje) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("nivel", nivel);
        item.put("elemento", elemento);
        item.put("elementoId", id);
        item.put("mensaje", mensaje);
        return item;
    }
}
