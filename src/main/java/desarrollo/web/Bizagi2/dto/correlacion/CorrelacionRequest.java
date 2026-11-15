package desarrollo.web.Bizagi2.dto.correlacion;

import desarrollo.web.Bizagi2.entities.AccionSinCaso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// HU-28: clave de correlacion del mensaje
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CorrelacionRequest {
    @NotBlank(message = "El criterio es obligatorio")
    private String criterio;

    @NotNull(message = "La accion sin caso es obligatoria")
    private AccionSinCaso accionSinCaso;
}
