package desarrollo.web.Bizagi2.dto.nodo;

import desarrollo.web.Bizagi2.entities.TipoActividad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Se crea dentro de una lane; el responsable es el rol de la lane
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActividadCreateRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotNull(message = "El tipo de actividad es obligatorio")
    private TipoActividad tipo;
    private String descripcion;
    // Opcionales: si no vienen, la posicion queda en 0
    private Double posicionX;
    private Double posicionY;
}
