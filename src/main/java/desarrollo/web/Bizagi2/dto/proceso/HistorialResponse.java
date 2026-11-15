package desarrollo.web.Bizagi2.dto.proceso;

import java.time.Instant;

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Un cambio registrado: quien, cuando y que
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialResponse {
    private Integer id;
    private AccionHistorial accion;
    private String entidad;
    private Integer entidadId;
    private Instant fecha;
    private String detalle;
    private Integer usuarioId;
    private String usuarioNombre;
}
