package desarrollo.web.Bizagi2.dto.lane;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LaneResponse {
    private Integer id;
    private String nombre;
    private int orden;
    private Integer poolId;
    private Integer rolProcesoId;
    private String rolProcesoNombre;
}
