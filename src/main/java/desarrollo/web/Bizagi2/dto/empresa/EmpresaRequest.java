package desarrollo.web.Bizagi2.dto.empresa;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Datos editables de la empresa. El id no viaja: se toma del usuario autenticado
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El NIT es obligatorio")
    private String nit;

    @NotBlank(message = "El correo de contacto es obligatorio")
    @Email(message = "El correo de contacto no tiene un formato valido")
    private String emailContacto;

    // Opcional (HU-24): si no viene, se conserva el valor actual
    private Boolean editorModificaEstructura;
}
