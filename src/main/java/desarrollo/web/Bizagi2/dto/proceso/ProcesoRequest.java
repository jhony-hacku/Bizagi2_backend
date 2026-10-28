package desarrollo.web.Bizagi2.dto.proceso;

import desarrollo.web.Bizagi2.entities.EstadoProceso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// HU-04 / HU-05: datos para crear o editar un proceso.
// La empresa no viaja: es la del usuario autenticado. estado solo se usa al editar.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcesoRequest {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;
    private String descripcion;
    private String categoria;

    // Opcional: BORRADOR | PUBLICADO. Si no viene se conserva el actual
    private EstadoProceso estado;
}
