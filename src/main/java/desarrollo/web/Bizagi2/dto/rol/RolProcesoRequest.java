package desarrollo.web.Bizagi2.dto.rol;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// HU-17 / HU-18: el nombre es unico dentro de la empresa
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolProcesoRequest {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombre;
    private String descripcion;
}
