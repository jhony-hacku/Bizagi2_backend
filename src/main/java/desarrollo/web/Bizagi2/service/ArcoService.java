package desarrollo.web.Bizagi2.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Arco;
import desarrollo.web.Bizagi2.entities.Evento;
import desarrollo.web.Bizagi2.entities.Gateway;
import desarrollo.web.Bizagi2.entities.NodoFlujo;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.TipoEvento;
import desarrollo.web.Bizagi2.entities.TipoGateway;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.ArcoRepository;
import lombok.RequiredArgsConstructor;

// HU-11, 12 y 13: arcos entre actividades, gateways y eventos de un mismo pool
@Service
@RequiredArgsConstructor
@Transactional
public class ArcoService {

    private final ArcoRepository arcoRepository;
    private final PoolService poolService;
    private final NodoFlujoService nodoFlujoService;
    private final ProcesoService procesoService;
    private final HistorialService historialService;
    private final ValidacionService validacionService;

    @Transactional(readOnly = true)
    public List<Arco> listarPorPool(Integer poolId, Integer empresaId) {
        poolService.buscarPorId(poolId, empresaId);
        return arcoRepository.findByPoolId(poolId);
    }

    @Transactional(readOnly = true)
    public Arco buscarPorId(Integer id, Integer empresaId) {
        Arco arco = arcoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Arco no encontrado"));
        if (!arco.getPool().getProceso().getEmpresa().getId().equals(empresaId)) {
            throw new RecursoNoEncontradoException("Arco no encontrado");
        }
        return arco;
    }

    // Body: origen: { id }, destino: { id }, etiqueta (opcional), condicion (solo si sale de un gateway)
    public Arco crear(Integer poolId, Usuario usuario, Arco datos) {
        Integer empresaId = usuario.getEmpresa().getId();
        Pool pool = poolService.buscarPorId(poolId, empresaId);
        Proceso proceso = procesoService.buscarActivo(pool.getProceso().getId(), empresaId);
        NodoFlujo origen = nodo(datos.getOrigen(), "origen", empresaId);
        NodoFlujo destino = nodo(datos.getDestino(), "destino", empresaId);
        validar(pool, origen, destino);
        if (arcoRepository.existsByOrigenIdAndDestinoId(origen.getId(), destino.getId())) {
            throw new ConflictoDominioException("Ya existe un arco entre esos dos elementos");
        }

        Arco arco = new Arco();
        arco.setPool(pool);
        arco.setOrigen(origen);
        arco.setDestino(destino);
        aplicarEtiquetaYCondicion(arco, datos);
        arco = arcoRepository.save(arco);
        historialService.registrar(usuario, proceso, AccionHistorial.CREAR, "ARCO", arco.getId(),
                "Arco creado de '" + origen.getNombre() + "' a '" + destino.getNombre() + "'");
        return arco;
    }

    // HU-12: se puede cambiar origen, destino, etiqueta y condicion, con las mismas validaciones de la creacion
    public Arco actualizar(Integer id, Usuario usuario, Arco datos) {
        Integer empresaId = usuario.getEmpresa().getId();
        Arco arco = buscarPorId(id, empresaId);
        Proceso proceso = procesoService.buscarActivo(arco.getPool().getProceso().getId(), empresaId);
        NodoFlujo origen = nodo(datos.getOrigen(), "origen", empresaId);
        NodoFlujo destino = nodo(datos.getDestino(), "destino", empresaId);
        validar(arco.getPool(), origen, destino);

        boolean cambioElPar = !origen.getId().equals(arco.getOrigen().getId())
                || !destino.getId().equals(arco.getDestino().getId());
        if (cambioElPar && arcoRepository.existsByOrigenIdAndDestinoId(origen.getId(), destino.getId())) {
            throw new ConflictoDominioException("Ya existe un arco entre esos dos elementos");
        }
        arco.setOrigen(origen);
        arco.setDestino(destino);
        aplicarEtiquetaYCondicion(arco, datos);
        arco = arcoRepository.save(arco);
        historialService.registrar(usuario, proceso, AccionHistorial.EDITAR, "ARCO", id,
                "Arco modificado: ahora va de '" + origen.getNombre() + "' a '" + destino.getNombre() + "'");
        return arco;
    }

    // HU-13: eliminacion logica; se devuelven las advertencias que deja el diagrama
    public Map<String, Object> eliminar(Integer id, Usuario usuario) {
        Integer empresaId = usuario.getEmpresa().getId();
        Arco arco = buscarPorId(id, empresaId);
        Proceso proceso = procesoService.buscarActivo(arco.getPool().getProceso().getId(), empresaId);
        arco.setActivo(false);
        arcoRepository.save(arco);
        historialService.registrar(usuario, proceso, AccionHistorial.ELIMINAR, "ARCO", id,
                "Arco eliminado de '" + arco.getOrigen().getNombre() + "' a '" + arco.getDestino().getNombre() + "'");
        return validacionService.resultadoEliminacion(proceso, 1, 0);
    }

    private NodoFlujo nodo(NodoFlujo referencia, String campo, Integer empresaId) {
        if (referencia == null || referencia.getId() == null) {
            throw new ReglaNegocioException("El arco requiere el elemento de " + campo + " (" + campo + ".id)");
        }
        return nodoFlujoService.buscarNodo(referencia.getId(), empresaId);
    }

    private void validar(Pool pool, NodoFlujo origen, NodoFlujo destino) {
        // Los arcos no cruzan pools: esa comunicacion se modela como mensaje
        if (!origen.getPool().getId().equals(pool.getId()) || !destino.getPool().getId().equals(pool.getId())) {
            throw new ReglaNegocioException(
                    "Un arco no puede cruzar pools: origen y destino deben estar en el pool " + pool.getId()
                            + ". Para comunicar pools distintos use un mensaje");
        }
        if (origen.getId().equals(destino.getId())) {
            throw new ReglaNegocioException("Un arco no puede unir un elemento consigo mismo");
        }
        // HU-27: un Message Catch de inicio no puede tener arcos entrantes
        if (destino instanceof Evento evento && evento.getTipoEvento() == TipoEvento.MENSAJE_RECEPCION_INICIO) {
            throw new ReglaNegocioException("Un Message Catch de inicio no puede tener arcos entrantes");
        }
    }

    // La condicion solo existe en arcos que salen de un gateway exclusivo o inclusivo (en el paralelo no aplica)
    private void aplicarEtiquetaYCondicion(Arco arco, Arco datos) {
        arco.setEtiqueta(texto(datos.getEtiqueta()));
        String condicion = texto(datos.getCondicion());
        if (condicion != null && !(arco.getOrigen() instanceof Gateway)) {
            throw new ReglaNegocioException("Solo los arcos que salen de un gateway tienen condicion");
        }
        boolean paralelo = arco.getOrigen() instanceof Gateway g && g.getTipoGateway() == TipoGateway.PARALELA;
        arco.setCondicion(paralelo ? null : condicion);
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
