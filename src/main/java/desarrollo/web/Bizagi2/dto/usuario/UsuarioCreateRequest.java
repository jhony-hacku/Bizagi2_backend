package desarrollo.web.Bizagi2.dto.usuario;

import desarrollo.web.Bizagi2.entities.RolAcceso;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// HU-02: el administrador crea un colaborador. La empresa no viaja: es la del administrador
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioCreateRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato valido")
    private String email;

    @NotBlank(message = "La contrasena es obligatoria")
    private String password;

    @NotNull(message = "El rol de acceso es obligatorio")
    private RolAcceso rolAcceso;
}
