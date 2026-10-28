package desarrollo.web.Bizagi2.dto.usuario;

import desarrollo.web.Bizagi2.entities.RolAcceso;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Actualizacion parcial: solo se cambian los campos que vengan con valor
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioUpdateRequest {

    private String nombre;

    @Email(message = "El email no tiene un formato valido")
    private String email;

    private String password;

    private RolAcceso rolAcceso;
}
