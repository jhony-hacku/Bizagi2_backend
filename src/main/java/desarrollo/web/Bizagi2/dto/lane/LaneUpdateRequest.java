package desarrollo.web.Bizagi2.dto.lane;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Ambos opcionales: si no viene el rol se conserva; si no viene el nombre se usa el del rol
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LaneUpdateRequest {
    private Integer rolProcesoId;
    private String nombre;
}
