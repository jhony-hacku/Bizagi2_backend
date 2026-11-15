package desarrollo.web.Bizagi2.dto.proceso;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// HU-23: NIT de la empresa con la que se comparte el proceso (solo lectura)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompartirRequest {
    @NotBlank(message = "El NIT es obligatorio")
    private String nit;
}
