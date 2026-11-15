package desarrollo.web.Bizagi2.dto.diagrama;

import desarrollo.web.Bizagi2.entities.EstadoProceso;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// propio = false cuando el proceso se consulta como compartido (HU-23)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagramaProcesoResponse {
    private Integer id;
    private String nombre;
    private String descripcion;
    private String categoria;
    private EstadoProceso estado;
    private boolean activo;
    private boolean propio;
}
