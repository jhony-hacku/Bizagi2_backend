package desarrollo.web.Bizagi2.dto.proceso;

import java.time.Instant;

import desarrollo.web.Bizagi2.entities.EstadoProceso;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// HU-23: un proceso que otra empresa compartio con la del usuario
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcesoCompartidoResponse {
    private Integer procesoId;
    private String nombre;
    private String categoria;
    private EstadoProceso estado;
    private String empresaPropietaria;
    private Instant compartidoDesde;
}
