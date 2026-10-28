package desarrollo.web.Bizagi2.dto.proceso;

import desarrollo.web.Bizagi2.entities.EstadoProceso;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcesoResponse {
    private Integer id;
    private String nombre;
    private String descripcion;
    private String categoria;
    private EstadoProceso estado;
    private boolean activo;
    private Integer empresaId;
}
