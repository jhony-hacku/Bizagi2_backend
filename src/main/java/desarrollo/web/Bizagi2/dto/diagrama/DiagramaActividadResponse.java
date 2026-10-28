package desarrollo.web.Bizagi2.dto.diagrama;

import com.fasterxml.jackson.annotation.JsonInclude;

import desarrollo.web.Bizagi2.entities.TipoActividad;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// rolProcesoId se omite (null) cuando el proceso es compartido
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiagramaActividadResponse {
    private Integer id;
    private String nombre;
    private TipoActividad tipo;
    private String descripcion;
    private double posicionX;
    private double posicionY;
    private Integer laneId;
    private Integer rolProcesoId;
}
