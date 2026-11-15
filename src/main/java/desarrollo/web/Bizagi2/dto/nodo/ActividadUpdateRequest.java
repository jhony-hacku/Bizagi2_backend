package desarrollo.web.Bizagi2.dto.nodo;

import desarrollo.web.Bizagi2.entities.TipoActividad;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// tipo y laneId son opcionales; la lane debe ser del mismo pool
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActividadUpdateRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    private TipoActividad tipo;
    private String descripcion;
    // Opcional: si no viene, la posicion queda en 0
    private Double posicionX;
    private Double posicionY;
    private Integer laneId;
}
