package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.Validaciones.requerido;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import desarrollo.web.Bizagi2.dto.proceso.EmpresaInvitadaResponse;
import desarrollo.web.Bizagi2.dto.proceso.ProcesoCompartidoResponse;
import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.ProcesoCompartido;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.EmpresaRepository;
import desarrollo.web.Bizagi2.repository.ProcesoCompartidoRepository;
import lombok.RequiredArgsConstructor;

// HU-23: por defecto los procesos no se comparten. El administrador de la empresa propietaria puede
// compartirlos en SOLO LECTURA con otra empresa (identificada por su NIT). Se comparte el modelo,
// nunca los usuarios ni los roles.
@Service
@RequiredArgsConstructor
@Transactional
public class ProcesoCompartidoService {

    private final ProcesoCompartidoRepository procesoCompartidoRepository;
    private final EmpresaRepository empresaRepository;
    private final ProcesoService procesoService;
    private final HistorialService historialService;

    public EmpresaInvitadaResponse compartir(Integer procesoId, Usuario usuario, String nit) {
        requerido(nit, "nit");
        Proceso proceso = procesoService.buscarActivo(procesoId, usuario.getEmpresa().getId());
        Empresa invitada = empresaRepository.findByNit(nit.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una empresa con ese NIT"));
        if (invitada.getId().equals(usuario.getEmpresa().getId())) {
            throw new ReglaNegocioException("El proceso ya es de tu empresa: comparte con otra empresa");
        }

        ProcesoCompartido compartido = procesoCompartidoRepository
                .findByProcesoIdAndEmpresaId(procesoId, invitada.getId()).orElse(new ProcesoCompartido());
        if (compartido.getId() != null && compartido.isActivo()) {
            throw new ConflictoDominioException("El proceso ya esta compartido con esa empresa");
        }
        compartido.setProceso(proceso);
        compartido.setEmpresa(invitada);
        compartido.setFecha(Instant.now());
        compartido.setActivo(true);
        compartido = procesoCompartidoRepository.save(compartido);
        historialService.registrar(usuario, proceso, AccionHistorial.COMPARTIR, "PROCESO", procesoId,
                "Proceso compartido en solo lectura con la empresa '" + invitada.getNombre() + "' (NIT " + invitada.getNit() + ")");
        return fila(compartido);
    }

    public void dejarDeCompartir(Integer procesoId, Usuario usuario, Integer empresaInvitadaId) {
        Proceso proceso = procesoService.buscarActivo(procesoId, usuario.getEmpresa().getId());
        ProcesoCompartido compartido = procesoCompartidoRepository
                .findByProcesoIdAndEmpresaId(procesoId, empresaInvitadaId)
                .filter(ProcesoCompartido::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("El proceso no esta compartido con esa empresa"));
        compartido.setActivo(false);
        procesoCompartidoRepository.save(compartido);
        historialService.registrar(usuario, proceso, AccionHistorial.DEJAR_DE_COMPARTIR, "PROCESO", procesoId,
                "Se dejo de compartir el proceso con la empresa '" + compartido.getEmpresa().getNombre() + "'");
    }

    // Empresas con las que la empresa propietaria comparte el proceso
    @Transactional(readOnly = true)
    public List<EmpresaInvitadaResponse> invitadas(Integer procesoId, Integer empresaId) {
        procesoService.buscarPorId(procesoId, empresaId);
        return procesoCompartidoRepository.findByProcesoIdAndActivoTrue(procesoId).stream().map(this::fila).toList();
    }

    // Procesos que otras empresas compartieron con la empresa del usuario
    @Transactional(readOnly = true)
    public List<ProcesoCompartidoResponse> compartidosConmigo(Integer empresaId) {
        return procesoCompartidoRepository.findByEmpresaIdAndActivoTrue(empresaId).stream()
                .filter(c -> c.getProceso().isActivo())
                .map(c -> new ProcesoCompartidoResponse(
                        c.getProceso().getId(),
                        c.getProceso().getNombre(),
                        c.getProceso().getCategoria(),
                        c.getProceso().getEstado(),
                        c.getProceso().getEmpresa().getNombre(),
                        c.getFecha()))
                .toList();
    }

    // Los nombres del DTO no coinciden con la entidad, por eso esta conversion es manual
    private EmpresaInvitadaResponse fila(ProcesoCompartido c) {
        return new EmpresaInvitadaResponse(c.getEmpresa().getId(), c.getEmpresa().getNombre(),
                c.getEmpresa().getNit(), c.getFecha());
    }
}
