package desarrollo.web.Bizagi2.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Historial;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.repository.HistorialRepository;
import lombok.RequiredArgsConstructor;

// Trazabilidad: cada cambio queda con el usuario que lo hizo, la fecha y un detalle
@Service
@RequiredArgsConstructor
@Transactional
public class HistorialService {

    private final HistorialRepository historialRepository;

    // proceso puede ser null (los roles son de la empresa y no de un proceso)
    public void registrar(Usuario usuario, Proceso proceso, AccionHistorial accion, String entidad,
            Integer entidadId, String detalle) {
        Historial registro = new Historial();
        registro.setProceso(proceso);
        registro.setUsuario(usuario);
        registro.setAccion(accion);
        registro.setEntidad(entidad);
        registro.setEntidadId(entidadId);
        registro.setFecha(Instant.now());
        registro.setDetalle(detalle);
        historialRepository.save(registro);
    }

    // Todo lo que cambio en el proceso y en su diagrama, del mas reciente al mas antiguo
    @Transactional(readOnly = true)
    public List<Historial> delProceso(Integer procesoId) {
        return historialRepository.findByProcesoIdOrderByFechaDescIdDesc(procesoId);
    }

    @Transactional(readOnly = true)
    public List<Historial> deEntidad(String entidad, Integer entidadId) {
        return historialRepository.findByEntidadAndEntidadIdOrderByFechaDescIdDesc(entidad, entidadId);
    }
}
