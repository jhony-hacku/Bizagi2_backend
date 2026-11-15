package desarrollo.web.Bizagi2.dto.nodo;

import desarrollo.web.Bizagi2.entities.TipoActividad;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActividadResponse {
    private Integer id;
    private String nombre;
    private TipoActividad tipo;
    private String descripcion;
    private double posicionX;
    private double posicionY;
    private Integer poolId;
    private Integer laneId;
}
