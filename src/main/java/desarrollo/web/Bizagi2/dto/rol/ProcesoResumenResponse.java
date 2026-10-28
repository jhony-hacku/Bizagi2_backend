package desarrollo.web.Bizagi2.dto.rol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Referencia corta a un proceso (id y nombre)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcesoResumenResponse {
    private Integer id;
    private String nombre;
}
