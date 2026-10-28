package desarrollo.web.Bizagi2.dto.lane;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// HU-22: la lane se asocia a un rol de proceso de la empresa
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LaneCreateRequest {
    @NotNull(message = "El rol de proceso es obligatorio")
    private Integer rolProcesoId;

    // Opcional: por defecto el nombre del rol
    private String nombre;
}
