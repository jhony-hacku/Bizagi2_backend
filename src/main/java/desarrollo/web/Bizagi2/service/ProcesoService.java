package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.Validaciones.requerido;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.EstadoProceso;
import desarrollo.web.Bizagi2.entities.Historial;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.TipoParticipante;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.PoolRepository;
import desarrollo.web.Bizagi2.repository.ProcesoCompartidoRepository;
import desarrollo.web.Bizagi2.repository.ProcesoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProcesoService {

    private static final int TAMANO_MAXIMO = 100;

    private final ProcesoRepository procesoRepository;
    private final PoolRepository poolRepository;
    private final ProcesoCompartidoRepository procesoCompartidoRepository;
    private final HistorialService historialService;
    private final ValidacionService validacionService;

    // HU-07: listado paginado con busqueda por nombre y filtros por estado y categoria.
    // Por defecto solo los procesos activos; con activo=false se consultan los eliminados (HU-06).
    @Transactional(readOnly = true)
    public Page<Proceso> listar(Integer empresaId, String nombre, EstadoProceso estado, String categoria,
            boolean activo, int pagina, int tamano) {
        if (pagina < 0 || tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new ReglaNegocioException(
                    "Paginacion invalida: page debe ser >= 0 y size entre 1 y " + TAMANO_MAXIMO);
        }
        return procesoRepository.buscar(empresaId, activo,
                nombre == null ? "" : nombre.trim(),
                estado != null,
                estado != null ? estado : EstadoProceso.BORRADOR,
                categoria == null ? "" : categoria.trim(),
                PageRequest.of(pagina, tamano, Sort.by("id")));
    }

    // Para consultar: devuelve el proceso aunque este inactivo (siempre que sea de la empresa)
    @Transactional(readOnly = true)
    public Proceso buscarPorId(Integer id, Integer empresaId) {
        Proceso proceso = procesoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado"));
        if (!proceso.getEmpresa().getId().equals(empresaId)) {
            throw new RecursoNoEncontradoException("Proceso no encontrado");
        }
        return proceso;
    }

    // HU-23: un proceso compartido tambien lo puede consultar la empresa invitada (solo lectura)
    @Transactional(readOnly = true)
    public Proceso buscarParaLectura(Integer id, Integer empresaId) {
        Proceso proceso = procesoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado"));
        boolean propio = proceso.getEmpresa().getId().equals(empresaId);
        boolean compartido = proceso.isActivo()
                && procesoCompartidoRepository.existsByProcesoIdAndEmpresaIdAndActivoTrue(id, empresaId);
        if (!propio && !compartido) {
            throw new RecursoNoEncontradoException("Proceso no encontrado");
        }
        return proceso;
    }

    // Para modificar o agregarle elementos: un proceso eliminado ya no admite cambios
    @Transactional(readOnly = true)
    public Proceso buscarActivo(Integer id, Integer empresaId) {
        Proceso proceso = buscarPorId(id, empresaId);
        if (!proceso.isActivo()) {
            throw new ConflictoDominioException("El proceso esta eliminado (inactivo) y no admite cambios");
        }
        return proceso;
    }

    // HU-04: nace en BORRADOR, con nombre unico en la empresa y con el pool de la empresa ya creado
    public Proceso crear(Usuario usuario, Proceso datos) {
        requerido(datos.getNombre(), "nombre");
        Integer empresaId = usuario.getEmpresa().getId();
        String nombre = datos.getNombre().trim();
        if (procesoRepository.existsByEmpresaIdAndActivoTrueAndNombreIgnoreCase(empresaId, nombre)) {
            throw new ConflictoDominioException("Ya existe un proceso con ese nombre en la empresa");
        }

        Proceso proceso = new Proceso();
        proceso.setEmpresa(usuario.getEmpresa());
        proceso.setNombre(nombre);
        proceso.setDescripcion(texto(datos.getDescripcion()));
        proceso.setCategoria(texto(datos.getCategoria()));
        proceso.setEstado(EstadoProceso.BORRADOR);
        proceso.setActivo(true);
        proceso = procesoRepository.save(proceso);

        Pool poolEmpresa = new Pool();
        poolEmpresa.setProceso(proceso);
        poolEmpresa.setNombre(usuario.getEmpresa().getNombre());
        poolEmpresa.setTipoParticipante(TipoParticipante.EMPRESA_PROPIETARIA);
        poolRepository.save(poolEmpresa);

        historialService.registrar(usuario, proceso, AccionHistorial.CREAR, "PROCESO", proceso.getId(),
                "Proceso creado en estado BORRADOR con el nombre '" + nombre + "'");
        return proceso;
    }

    // HU-05: mismas validaciones que al crear, y cada cambio queda en el historial.
    // Al pasar a PUBLICADO el diagrama debe ser coherente (en borrador se puede trabajar incompleto).
    public Proceso actualizar(Integer id, Usuario usuario, Proceso datos) {
        requerido(datos.getNombre(), "nombre");
        Integer empresaId = usuario.getEmpresa().getId();
        Proceso proceso = buscarActivo(id, empresaId);

        String nombre = datos.getNombre().trim();
        if (procesoRepository.existsByEmpresaIdAndActivoTrueAndNombreIgnoreCaseAndIdNot(empresaId, nombre, id)) {
            throw new ConflictoDominioException("Ya existe un proceso con ese nombre en la empresa");
        }
        EstadoProceso estado = datos.getEstado() != null ? datos.getEstado() : proceso.getEstado();

        if (estado == EstadoProceso.PUBLICADO && proceso.getEstado() != EstadoProceso.PUBLICADO) {
            List<String> errores = validacionService.errores(proceso);
            if (!errores.isEmpty()) {
                throw new ReglaNegocioException("No se puede publicar el proceso: " + String.join("; ", errores));
            }
        }

        List<String> cambios = new ArrayList<>();
        anotarCambio(cambios, "nombre", proceso.getNombre(), nombre);
        anotarCambio(cambios, "descripcion", proceso.getDescripcion(), texto(datos.getDescripcion()));
        anotarCambio(cambios, "categoria", proceso.getCategoria(), texto(datos.getCategoria()));
        anotarCambio(cambios, "estado", proceso.getEstado(), estado);

        proceso.setNombre(nombre);
        proceso.setDescripcion(texto(datos.getDescripcion()));
        proceso.setCategoria(texto(datos.getCategoria()));
        proceso.setEstado(estado);
        proceso = procesoRepository.save(proceso);

        if (!cambios.isEmpty()) {
            historialService.registrar(usuario, proceso, AccionHistorial.EDITAR, "PROCESO", id,
                    String.join("; ", cambios));
        }
        return proceso;
    }

    // HU-06: eliminacion logica. El proceso pasa a inactivo y se conserva todo, incluido su historial.
    public void eliminar(Integer id, Usuario usuario) {
        Proceso proceso = buscarActivo(id, usuario.getEmpresa().getId());
        proceso.setActivo(false);
        procesoRepository.save(proceso);
        historialService.registrar(usuario, proceso, AccionHistorial.ELIMINAR, "PROCESO", id,
                "Proceso eliminado (pasa a inactivo)");
    }

    // HU-07: historial de cambios del proceso y de su diagrama, del mas reciente al mas antiguo
    @Transactional(readOnly = true)
    public List<Historial> historial(Integer id, Integer empresaId) {
        return historialService.delProceso(buscarPorId(id, empresaId).getId());
    }

    // Advertencias y errores del diagrama (lo que el editor le avisa al usuario)
    @Transactional(readOnly = true)
    public List<Map<String, Object>> validar(Integer id, Integer empresaId) {
        return validacionService.validar(buscarPorId(id, empresaId));
    }

    private void anotarCambio(List<String> cambios, String campo, Object antes, Object despues) {
        if (!Objects.equals(antes, despues)) {
            cambios.add(campo + ": '" + (antes == null ? "" : antes) + "' -> '" + (despues == null ? "" : despues) + "'");
        }
    }

    // Recorta espacios y trata el texto vacio como "no enviado"
    private String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
