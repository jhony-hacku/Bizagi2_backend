package desarrollo.web.Bizagi2.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import desarrollo.web.Bizagi2.dto.proceso.AdvertenciaResponse;
import desarrollo.web.Bizagi2.dto.proceso.EliminacionResponse;
import desarrollo.web.Bizagi2.entities.AccionFallo;
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

    public List<AdvertenciaResponse> validar(Proceso proceso) {
        List<AdvertenciaResponse> items = new ArrayList<>();
        List<Evento> recepciones = new ArrayList<>();
        for (Pool pool : poolRepository.findByProcesoId(proceso.getId())) {
            // Los participantes externos son cajas negras: solo se valida el flujo de los pools internos
            if (pool.getTipoParticipante() == TipoParticipante.EMPRESA_PROPIETARIA) {
                validarPool(items, pool, recepciones);
            }
        }
        validarMensajes(items, proceso, recepciones);
        return items;
    }

    // Respuesta de las eliminaciones de nodos y arcos: que se elimino y que advierte ahora el editor
    public EliminacionResponse resultadoEliminacion(Proceso proceso, int arcosEliminados, int mensajesEliminados) {
        return new EliminacionResponse(arcosEliminados, mensajesEliminados, validar(proceso));
    }

    // Solo los textos de los errores: lo usa el proceso al pasar a PUBLICADO
    public List<String> errores(Proceso proceso) {
        return validar(proceso).stream()
                .filter(i -> ERROR.equals(i.getNivel()))
                .map(AdvertenciaResponse::getMensaje)
                .toList();
    }

    // ---------- Flujo dentro de un pool ----------

    private void validarPool(List<AdvertenciaResponse> items, Pool pool, List<Evento> recepciones) {
        Map<Integer, List<Arco>> salientes = new HashMap<>();
        Map<Integer, Integer> entrantes = new HashMap<>();
        for (Arco arco : arcoRepository.findByPoolId(pool.getId())) {
            salientes.computeIfAbsent(arco.getOrigen().getId(), k -> new ArrayList<>()).add(arco);
            entrantes.merge(arco.getDestino().getId(), 1, Integer::sum);
        }
        for (NodoFlujo nodo : nodoFlujoRepository.findByPoolId(pool.getId())) {
            int nEntrantes = entrantes.getOrDefault(nodo.getId(), 0);
            List<Arco> misSalientes = salientes.getOrDefault(nodo.getId(), List.of());
            validarNodo(items, nodo, nEntrantes, misSalientes, recepciones);
        }
    }

    private void validarNodo(List<AdvertenciaResponse> items, NodoFlujo nodo, int nEntrantes, List<Arco> salientes,
            List<Evento> recepciones) {
        String tipo = tipoDe(nodo);
        validarConexiones(items, nodo, tipo, nEntrantes, salientes);
        if (nodo instanceof Gateway gateway) {
            validarGateway(items, gateway, nEntrantes, salientes);
        } else if (salientes.size() >= 2) {
            // HU-16: una ramificacion necesita un gateway como punto de decision
            items.add(item(ADVERTENCIA, tipo, nodo.getId(), "'" + nodo.getNombre() + "' se divide en "
                    + salientes.size() + " caminos sin un gateway que funcione como punto de decision"));
        }
        if (nodo instanceof Evento evento) {
            validarEventoDeRecepcion(items, evento, nEntrantes, recepciones);
        }
    }

    // HU-10 y HU-13: elementos desconectados
    private void validarConexiones(List<AdvertenciaResponse> items, NodoFlujo nodo, String tipo, int nEntrantes,
            List<Arco> salientes) {
        boolean esInicio = esEvento(nodo, TipoEvento.INICIO) || esEvento(nodo, TipoEvento.MENSAJE_RECEPCION_INICIO);
        if (nEntrantes == 0 && !esInicio) {
            items.add(item(ADVERTENCIA, tipo, nodo.getId(), "'" + nodo.getNombre() + "' no tiene camino de entrada"));
        }
        if (salientes.isEmpty() && !esEvento(nodo, TipoEvento.FIN)) {
            items.add(item(ADVERTENCIA, tipo, nodo.getId(), "'" + nodo.getNombre() + "' no tiene camino de salida"));
        }
    }

    // HU-27: los Message Catch se revisan despues contra los mensajes; el de inicio no tiene arcos entrantes
    private void validarEventoDeRecepcion(List<AdvertenciaResponse> items, Evento evento, int nEntrantes,
            List<Evento> recepciones) {
        boolean inicio = evento.getTipoEvento() == TipoEvento.MENSAJE_RECEPCION_INICIO;
        if (inicio || evento.getTipoEvento() == TipoEvento.MENSAJE_RECEPCION_INTERMEDIO) {
            recepciones.add(evento);
        }
        if (inicio && nEntrantes > 0) {
            items.add(item(ERROR, "EVENTO", evento.getId(),
                    "El Message Catch de inicio '" + evento.getNombre() + "' no puede tener arcos entrantes"));
        }
    }

    // ---------- Gateways ----------

    private void validarGateway(List<AdvertenciaResponse> items, Gateway gateway, int nEntrantes, List<Arco> salientes) {
        // HU-14: un gateway de divergencia tiene al menos dos arcos salientes (o es de convergencia)
        boolean convergencia = nEntrantes >= 2 && salientes.size() == 1;
        if (salientes.size() < 2 && !convergencia) {
            items.add(item(ERROR, "GATEWAY", gateway.getId(),
                    "El gateway '" + gateway.getNombre() + "' debe tener al menos dos arcos salientes"));
        }
        boolean condicional = gateway.getTipoGateway() == TipoGateway.EXCLUSIVA
                || gateway.getTipoGateway() == TipoGateway.INCLUSIVA;
        if (condicional && salientes.size() >= 2) {
            validarCondiciones(items, gateway, salientes);
        }
        if (gateway.getTipoGateway() == TipoGateway.EXCLUSIVA) {
            validarCondicionesRepetidas(items, gateway, salientes);
        }
    }

    // HU-14: en un gateway exclusivo o inclusivo cada arco saliente lleva una condicion
    private void validarCondiciones(List<AdvertenciaResponse> items, Gateway gateway, List<Arco> salientes) {
        for (Arco arco : salientes) {
            if (vacio(arco.getCondicion())) {
                items.add(item(ERROR, "ARCO", arco.getId(), "El arco " + arco.getId() + " que sale del gateway '"
                        + gateway.getNombre() + "' debe tener una condicion"));
            }
        }
    }

    // HU-15: en un exclusivo las condiciones deben excluirse entre si (aqui se detectan las repetidas)
    private void validarCondicionesRepetidas(List<AdvertenciaResponse> items, Gateway gateway, List<Arco> salientes) {
        Map<String, Integer> vistas = new HashMap<>();
        salientes.stream()
                .filter(arco -> !vacio(arco.getCondicion()))
                .forEach(arco -> vistas.merge(arco.getCondicion().trim().toLowerCase(), 1, Integer::sum));
        if (vistas.values().stream().anyMatch(n -> n > 1)) {
            items.add(item(ADVERTENCIA, "GATEWAY", gateway.getId(), "El gateway exclusivo '" + gateway.getNombre()
                    + "' tiene condiciones repetidas: no son mutuamente excluyentes"));
        }
    }

    // ---------- Mensajes entre pools ----------

    private void validarMensajes(List<AdvertenciaResponse> items, Proceso proceso, List<Evento> recepciones) {
        List<Mensaje> mensajes = mensajeRepository.findByProcesoId(proceso.getId());
        Map<Integer, Correlacion> correlaciones = new HashMap<>();
        Map<String, Integer> nombreYClave = new HashMap<>();
        for (Mensaje m : mensajes) {
            correlacionRepository.findByMensajeId(m.getId()).ifPresent(c -> {
                correlaciones.put(m.getId(), c);
                nombreYClave.merge(clave(m.getNombre(), c.getCriterio()), 1, Integer::sum);
            });
        }
        for (Mensaje m : mensajes) {
            validarMensaje(items, m, correlaciones.get(m.getId()), nombreYClave, recepciones);
        }
        for (Evento e : recepciones) {
            validarRecepcion(items, e, mensajes, correlaciones);
        }
    }

    private void validarMensaje(List<AdvertenciaResponse> items, Mensaje m, Correlacion c,
            Map<String, Integer> nombreYClave, List<Evento> recepciones) {
        // HU-25: el nombre del mensaje debe corresponder a un Message Catch del pool destino
        boolean destinoInterno = m.getDestinoPool().getTipoParticipante() == TipoParticipante.EMPRESA_PROPIETARIA;
        if (destinoInterno && !hayReceptor(m, recepciones)) {
            items.add(item(ADVERTENCIA, "MENSAJE", m.getId(), "El mensaje '" + m.getNombre()
                    + "' queda sin receptor en el pool '" + m.getDestinoPool().getNombre() + "'"));
        }
        // HU-26: si deriva a manejo de error, la actividad de error debe seguir existiendo
        if (m.getAccionFallo() == AccionFallo.DERIVAR_A_MANEJO_ERROR && m.getActividadError() == null) {
            items.add(item(ADVERTENCIA, "MENSAJE", m.getId(),
                    "El mensaje '" + m.getNombre() + "' deriva a manejo de error pero no tiene actividad de error"));
        }
        // HU-28: cada mensaje declara una clave de correlacion, y no la comparte con otro mensaje del mismo nombre
        if (c == null) {
            items.add(item(ADVERTENCIA, "MENSAJE", m.getId(),
                    "El mensaje '" + m.getNombre() + "' no tiene clave de correlacion"));
        } else if (nombreYClave.get(clave(m.getNombre(), c.getCriterio())) > 1) {
            items.add(item(ADVERTENCIA, "MENSAJE", m.getId(), "El mensaje '" + m.getNombre()
                    + "' es ambiguo: otro mensaje comparte su nombre y su clave de correlacion"));
        }
    }

    private boolean hayReceptor(Mensaje m, List<Evento> recepciones) {
        return recepciones.stream().anyMatch(e -> e.getPool().getId().equals(m.getDestinoPool().getId())
                && mismoNombre(e.getNombre(), m.getNombre()));
    }

    private void validarRecepcion(List<AdvertenciaResponse> items, Evento e, List<Mensaje> mensajes,
            Map<Integer, Correlacion> correlaciones) {
        List<Mensaje> lanzados = mensajes.stream().filter(m -> mismoNombre(m.getNombre(), e.getNombre())).toList();
        // HU-27: debe existir un Message Throw con ese nombre o estar marcado como de origen externo
        if (lanzados.isEmpty() && !Boolean.TRUE.equals(e.getOrigenExterno())) {
            items.add(item(ADVERTENCIA, "EVENTO", e.getId(), "No existe un mensaje lanzado con el nombre '"
                    + e.getNombre() + "' ni se marco el evento como de origen externo"));
        }
        // HU-28: un Message Catch intermedio sin clave de correlacion
        if (vacio(e.getClaveCorrelacion())) {
            if (e.getTipoEvento() == TipoEvento.MENSAJE_RECEPCION_INTERMEDIO) {
                items.add(item(ADVERTENCIA, "EVENTO", e.getId(),
                        "El Message Catch intermedio '" + e.getNombre() + "' no tiene clave de correlacion"));
            }
            return;
        }
        // HU-28: la clave debe ser la misma en el lanzamiento y en la recepcion
        for (Mensaje m : lanzados) {
            Correlacion c = correlaciones.get(m.getId());
            if (c != null && c.getCriterio() != null
                    && !c.getCriterio().trim().equalsIgnoreCase(e.getClaveCorrelacion().trim())) {
                items.add(item(ADVERTENCIA, "EVENTO", e.getId(), "La clave de correlacion de '" + e.getNombre()
                        + "' (" + e.getClaveCorrelacion() + ") no coincide con la del mensaje lanzado ("
                        + c.getCriterio() + ")"));
            }
        }
    }

    // ---------- Apoyo ----------

    private boolean esEvento(NodoFlujo nodo, TipoEvento tipo) {
        return nodo instanceof Evento evento && evento.getTipoEvento() == tipo;
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
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

    private AdvertenciaResponse item(String nivel, String elemento, Integer id, String mensaje) {
        return new AdvertenciaResponse(nivel, elemento, id, mensaje);
    }
}
