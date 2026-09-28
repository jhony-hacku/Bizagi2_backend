package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.Validaciones.requerido;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Historial;
import desarrollo.web.Bizagi2.entities.Lane;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.RolProceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.LaneRepository;
import desarrollo.web.Bizagi2.repository.RolProcesoRepository;
import lombok.RequiredArgsConstructor;

// HU-17 a HU-20: los roles de proceso son de la empresa y se usan en las lanes de cualquiera de sus procesos
@Service
@RequiredArgsConstructor
@Transactional
public class RolProcesoService {

    private static final int TAMANO_MAXIMO = 100;

    private final RolProcesoRepository rolProcesoRepository;
    private final LaneRepository laneRepository;
    private final EmpresaService empresaService;
    private final HistorialService historialService;

    // HU-20: lista paginada con busqueda por nombre, procesos donde se usa y si se puede eliminar
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> listar(Integer empresaId, String nombre, int pagina, int tamano) {
        if (pagina < 0 || tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new ReglaNegocioException(
                    "Paginacion invalida: page debe ser >= 0 y size entre 1 y " + TAMANO_MAXIMO);
        }
        return rolProcesoRepository.buscar(empresaId, nombre == null ? "" : nombre.trim(),
                PageRequest.of(pagina, tamano, Sort.by("nombre"))).map(this::conUso);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detalle(Integer id, Integer empresaId) {
        return conUso(buscarPorId(id, empresaId));
    }

    // Los roles que se pueden asignar a las lanes de los pools de la empresa (HU-24)
    @Transactional(readOnly = true)
    public List<RolProceso> disponibles(Integer empresaId) {
        return rolProcesoRepository.findByEmpresaIdOrderByNombre(empresaId);
    }

    @Transactional(readOnly = true)
    public RolProceso buscarPorId(Integer id, Integer empresaId) {
        RolProceso rol = rolProcesoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol de proceso no encontrado"));
        if (!rol.getEmpresa().getId().equals(empresaId)) {
            throw new RecursoNoEncontradoException("Rol de proceso no encontrado");
        }
        return rol;
    }

    // HU-17: nombre y descripcion; el nombre es unico dentro de la empresa
    public RolProceso crear(Usuario usuario, RolProceso datos) {
        requerido(datos.getNombre(), "nombre");
        Integer empresaId = usuario.getEmpresa().getId();
        String nombre = datos.getNombre().trim();
        if (rolProcesoRepository.existsByEmpresaIdAndNombreIgnoreCase(empresaId, nombre)) {
            throw new ConflictoDominioException("Ya existe un rol con ese nombre en la empresa");
        }
        RolProceso rol = new RolProceso();
        rol.setEmpresa(empresaService.buscarPorId(empresaId));
        rol.setNombre(nombre);
        rol.setDescripcion(datos.getDescripcion());
        rol.setActivo(true);
        rol = rolProcesoRepository.save(rol);
        historialService.registrar(usuario, null, AccionHistorial.CREAR, "ROL", rol.getId(),
                "Rol '" + nombre + "' creado");
        return rol;
    }

    // HU-18: los procesos que lo usan siguen apuntando al mismo rol y ven el nombre nuevo
    public RolProceso actualizar(Integer id, Usuario usuario, RolProceso datos) {
        requerido(datos.getNombre(), "nombre");
        Integer empresaId = usuario.getEmpresa().getId();
        RolProceso rol = buscarPorId(id, empresaId);
        String nombre = datos.getNombre().trim();
        if (rolProcesoRepository.existsByEmpresaIdAndNombreIgnoreCaseAndIdNot(empresaId, nombre, id)) {
            throw new ConflictoDominioException("Ya existe un rol con ese nombre en la empresa");
        }
        String detalle = "Rol '" + rol.getNombre() + "' modificado a '" + nombre + "'";
        rol.setNombre(nombre);
        rol.setDescripcion(datos.getDescripcion());
        rol = rolProcesoRepository.save(rol);
        historialService.registrar(usuario, null, AccionHistorial.EDITAR, "ROL", id, detalle);
        return rol;
    }

    // HU-19: si alguna lane lo usa no se elimina (y se dice en que procesos); si no, queda inactivo
    public void eliminar(Integer id, Usuario usuario) {
        RolProceso rol = buscarPorId(id, usuario.getEmpresa().getId());
        List<String> procesos = nombresDeProcesos(rol);
        if (!procesos.isEmpty()) {
            throw new ConflictoDominioException(
                    "El rol esta en uso y no se puede eliminar. Procesos donde se usa: " + String.join(", ", procesos));
        }
        rol.setActivo(false);
        rolProcesoRepository.save(rol);
        historialService.registrar(usuario, null, AccionHistorial.ELIMINAR, "ROL", id,
                "Rol '" + rol.getNombre() + "' eliminado (pasa a inactivo)");
    }

    @Transactional(readOnly = true)
    public List<Historial> historial(Integer id, Integer empresaId) {
        buscarPorId(id, empresaId);
        return historialService.deEntidad("ROL", id);
    }

    private Map<String, Object> conUso(RolProceso rol) {
        Map<Integer, String> procesos = procesosDondeSeUsa(rol);
        List<Map<String, Object>> lista = new ArrayList<>();
        procesos.forEach((pid, nombre) -> {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("id", pid);
            p.put("nombre", nombre);
            lista.add(p);
        });
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("id", rol.getId());
        fila.put("nombre", rol.getNombre());
        fila.put("descripcion", rol.getDescripcion());
        fila.put("enUso", !procesos.isEmpty());
        fila.put("eliminable", procesos.isEmpty());
        fila.put("procesos", lista);
        return fila;
    }

    private List<String> nombresDeProcesos(RolProceso rol) {
        return new ArrayList<>(procesosDondeSeUsa(rol).values());
    }

    private Map<Integer, String> procesosDondeSeUsa(RolProceso rol) {
        Map<Integer, String> procesos = new LinkedHashMap<>();
        for (Lane lane : laneRepository.findByRolProcesoId(rol.getId())) {
            Proceso proceso = lane.getPool().getProceso();
            procesos.putIfAbsent(proceso.getId(), proceso.getNombre());
        }
        return procesos;
    }
}
