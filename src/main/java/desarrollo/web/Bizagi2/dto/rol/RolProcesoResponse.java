package desarrollo.web.Bizagi2.dto.rol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolProcesoResponse {
    private Integer id;
    private String nombre;
    private String descripcion;
}
