package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.Validaciones.requerido;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.TipoParticipante;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.LaneRepository;
import desarrollo.web.Bizagi2.repository.MensajeRepository;
import desarrollo.web.Bizagi2.repository.NodoFlujoRepository;
import desarrollo.web.Bizagi2.repository.PoolRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PoolService {

    private final PoolRepository poolRepository;
    private final LaneRepository laneRepository;
    private final NodoFlujoRepository nodoFlujoRepository;
    private final MensajeRepository mensajeRepository;
    private final ProcesoService procesoService;
    private final HistorialService historialService;

    @Transactional(readOnly = true)
    public List<Pool> listarPorProceso(Integer procesoId, Integer empresaId) {
        procesoService.buscarPorId(procesoId, empresaId);
        return poolRepository.findByProcesoId(procesoId);
    }

    @Transactional(readOnly = true)
    public Pool buscarPorId(Integer id, Integer empresaId) {
        Pool pool = poolRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pool no encontrado"));
        if (!pool.getProceso().getEmpresa().getId().equals(empresaId)) {
            throw new RecursoNoEncontradoException("Pool no encontrado");
        }
        return pool;
    }

    // HU-24: el administrador siempre puede; el editor solo si la empresa lo permite; el lector nunca
    public void verificarPermisoEstructura(Usuario usuario) {
        boolean puede = usuario.getRolAcceso() == RolAcceso.ADMINISTRADOR
                || (usuario.getRolAcceso() == RolAcceso.EDITOR
                        && !Boolean.FALSE.equals(usuario.getEmpresa().getEditorModificaEstructura()));
        if (!puede) {
            throw new AccessDeniedException("No tienes permisos para modificar pools y lanes");
        }
    }

    // HU-21: un participante externo (cliente, proveedor, sistema externo) es una caja negra, sin elementos internos
    public void verificarInterno(Pool pool) {
        if (pool.getTipoParticipante() != TipoParticipante.EMPRESA_PROPIETARIA) {
            throw new ReglaNegocioException(
                    "El pool '" + pool.getNombre() + "' es un participante externo (caja negra) y no admite elementos internos");
        }
    }

    public Pool crear(Integer procesoId, Usuario usuario, Pool datos) {
        verificarPermisoEstructura(usuario);
        requerido(datos.getNombre(), "nombre");
        requerido(datos.getTipoParticipante(), "tipoParticipante");
        Proceso proceso = procesoService.buscarActivo(procesoId, usuario.getEmpresa().getId());

        Pool pool = new Pool();
        pool.setProceso(proceso);
        pool.setNombre(datos.getNombre().trim());
        pool.setTipoParticipante(datos.getTipoParticipante());
        pool.setActivo(true);
        pool = poolRepository.save(pool);
        historialService.registrar(usuario, proceso, AccionHistorial.CREAR, "POOL", pool.getId(),
                "Pool '" + pool.getNombre() + "' (" + pool.getTipoParticipante() + ") creado");
        return pool;
    }

    public Pool actualizar(Integer id, Usuario usuario, Pool datos) {
        verificarPermisoEstructura(usuario);
        requerido(datos.getNombre(), "nombre");
        requerido(datos.getTipoParticipante(), "tipoParticipante");
        Pool pool = buscarPorId(id, usuario.getEmpresa().getId());
        procesoService.buscarActivo(pool.getProceso().getId(), usuario.getEmpresa().getId());

        boolean pasaAExterno = datos.getTipoParticipante() != TipoParticipante.EMPRESA_PROPIETARIA;
        if (pasaAExterno && (laneRepository.existsByPoolId(id) || nodoFlujoRepository.existsByPoolId(id))) {
            throw new ConflictoDominioException(
                    "El pool tiene lanes o elementos y no puede pasar a ser un participante externo");
        }
        pool.setNombre(datos.getNombre().trim());
        pool.setTipoParticipante(datos.getTipoParticipante());
        pool = poolRepository.save(pool);
        historialService.registrar(usuario, pool.getProceso(), AccionHistorial.EDITAR, "POOL", id,
                "Pool '" + pool.getNombre() + "' modificado");
        return pool;
    }

    // Un pool solo se elimina (logicamente) cuando esta vacio
    public void eliminar(Integer id, Usuario usuario) {
        verificarPermisoEstructura(usuario);
        Pool pool = buscarPorId(id, usuario.getEmpresa().getId());
        procesoService.buscarActivo(pool.getProceso().getId(), usuario.getEmpresa().getId());
        if (laneRepository.existsByPoolId(id) || nodoFlujoRepository.existsByPoolId(id)
                || mensajeRepository.existsByDestinoPoolId(id)) {
            throw new ConflictoDominioException(
                    "El pool tiene lanes, elementos del flujo o mensajes dirigidos a el; eliminalos primero");
        }
        pool.setActivo(false);
        poolRepository.save(pool);
        historialService.registrar(usuario, pool.getProceso(), AccionHistorial.ELIMINAR, "POOL", id,
                "Pool '" + pool.getNombre() + "' eliminado");
    }
}
