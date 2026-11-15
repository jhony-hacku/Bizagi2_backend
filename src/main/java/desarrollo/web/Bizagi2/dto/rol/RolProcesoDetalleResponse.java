package desarrollo.web.Bizagi2.dto.rol;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// HU-20: el rol con los procesos donde se usa y si se puede eliminar
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolProcesoDetalleResponse {
    private Integer id;
    private String nombre;
    private String descripcion;
    private boolean enUso;
    private boolean eliminable;
    private List<ProcesoResumenResponse> procesos;
}
