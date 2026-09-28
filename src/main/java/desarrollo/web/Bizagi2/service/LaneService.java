package desarrollo.web.Bizagi2.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Lane;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.RolProceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.LaneRepository;
import desarrollo.web.Bizagi2.repository.NodoFlujoRepository;
import lombok.RequiredArgsConstructor;

// HU-22: las lanes dividen un pool y cada una esta asociada a un rol de proceso de la empresa
@Service
@RequiredArgsConstructor
@Transactional
public class LaneService {

    private final LaneRepository laneRepository;
    private final NodoFlujoRepository nodoFlujoRepository;
    private final PoolService poolService;
    private final RolProcesoService rolProcesoService;
    private final ProcesoService procesoService;
    private final HistorialService historialService;

    @Transactional(readOnly = true)
    public List<Lane> listarPorPool(Integer poolId, Integer empresaId) {
        poolService.buscarPorId(poolId, empresaId);
        return laneRepository.findByPoolIdOrderByOrdenAscIdAsc(poolId);
    }

    @Transactional(readOnly = true)
    public Lane buscarPorId(Integer id, Integer empresaId) {
        Lane lane = laneRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Lane no encontrada"));
        if (!lane.getPool().getProceso().getEmpresa().getId().equals(empresaId)) {
            throw new RecursoNoEncontradoException("Lane no encontrada");
        }
        return lane;
    }

    // Body: rolProceso: { id } (obligatorio) y nombre (opcional: por defecto el nombre del rol)
    public Lane crear(Integer poolId, Usuario usuario, Lane datos) {
        poolService.verificarPermisoEstructura(usuario);
        Integer empresaId = usuario.getEmpresa().getId();
        Pool pool = poolService.buscarPorId(poolId, empresaId);
        procesoService.buscarActivo(pool.getProceso().getId(), empresaId);
        poolService.verificarInterno(pool);
        RolProceso rol = rolDe(datos, empresaId);

        int orden = laneRepository.findByPoolIdOrderByOrdenAscIdAsc(poolId).stream()
                .mapToInt(Lane::getOrden).max().orElse(-1) + 1;
        Lane lane = new Lane();
        lane.setPool(pool);
        lane.setRolProceso(rol);
        lane.setNombre(nombreDe(datos, rol));
        lane.setOrden(orden);
        lane.setActivo(true);
        lane = laneRepository.save(lane);
        historialService.registrar(usuario, pool.getProceso(), AccionHistorial.CREAR, "LANE", lane.getId(),
                "Lane '" + lane.getNombre() + "' creada con el rol '" + rol.getNombre() + "'");
        return lane;
    }

    public Lane actualizar(Integer id, Usuario usuario, Lane datos) {
        poolService.verificarPermisoEstructura(usuario);
        Integer empresaId = usuario.getEmpresa().getId();
        Lane lane = buscarPorId(id, empresaId);
        procesoService.buscarActivo(lane.getPool().getProceso().getId(), empresaId);

        RolProceso rol = lane.getRolProceso();
        if (datos.getRolProceso() != null && datos.getRolProceso().getId() != null) {
            rol = rolProcesoService.buscarPorId(datos.getRolProceso().getId(), empresaId);
        }
        lane.setRolProceso(rol);
        lane.setNombre(nombreDe(datos, rol));
        lane = laneRepository.save(lane);
        historialService.registrar(usuario, lane.getPool().getProceso(), AccionHistorial.EDITAR, "LANE", id,
                "Lane '" + lane.getNombre() + "' modificada (rol '" + rol.getNombre() + "')");
        return lane;
    }

    // Recibe los ids de todas las lanes del pool en el orden deseado
    public List<Lane> reordenar(Integer poolId, Usuario usuario, List<Integer> idsEnOrden) {
        poolService.verificarPermisoEstructura(usuario);
        Integer empresaId = usuario.getEmpresa().getId();
        Pool pool = poolService.buscarPorId(poolId, empresaId);
        procesoService.buscarActivo(pool.getProceso().getId(), empresaId);

        List<Lane> lanes = laneRepository.findByPoolIdOrderByOrdenAscIdAsc(poolId);
        Set<Integer> existentes = new HashSet<>();
        lanes.forEach(l -> existentes.add(l.getId()));
        if (idsEnOrden == null || idsEnOrden.size() != existentes.size() || !existentes.equals(new HashSet<>(idsEnOrden))) {
            throw new ReglaNegocioException("Debes enviar los ids de todas las lanes del pool, sin repetir");
        }
        for (Lane lane : lanes) {
            lane.setOrden(idsEnOrden.indexOf(lane.getId()));
            laneRepository.save(lane);
        }
        historialService.registrar(usuario, pool.getProceso(), AccionHistorial.EDITAR, "POOL", poolId,
                "Lanes del pool '" + pool.getNombre() + "' reordenadas");
        return laneRepository.findByPoolIdOrderByOrdenAscIdAsc(poolId);
    }

    // No se elimina una lane con actividades: primero se reasignan a otra lane
    public void eliminar(Integer id, Usuario usuario) {
        poolService.verificarPermisoEstructura(usuario);
        Integer empresaId = usuario.getEmpresa().getId();
        Lane lane = buscarPorId(id, empresaId);
        procesoService.buscarActivo(lane.getPool().getProceso().getId(), empresaId);
        long actividades = nodoFlujoRepository.contarActividadesPorLane(id);
        if (actividades > 0) {
            throw new ConflictoDominioException(
                    "La lane tiene " + actividades + " actividad(es): reasignalas a otra lane primero");
        }
        lane.setActivo(false);
        laneRepository.save(lane);
        historialService.registrar(usuario, lane.getPool().getProceso(), AccionHistorial.ELIMINAR, "LANE", id,
                "Lane '" + lane.getNombre() + "' eliminada");
    }

    private RolProceso rolDe(Lane datos, Integer empresaId) {
        if (datos.getRolProceso() == null || datos.getRolProceso().getId() == null) {
            throw new ReglaNegocioException("La lane requiere un rol de proceso (rolProceso.id)");
        }
        return rolProcesoService.buscarPorId(datos.getRolProceso().getId(), empresaId);
    }

    private String nombreDe(Lane datos, RolProceso rol) {
        return datos.getNombre() == null || datos.getNombre().isBlank() ? rol.getNombre() : datos.getNombre().trim();
    }
}
