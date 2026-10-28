package desarrollo.web.Bizagi2.dto.auth;

import desarrollo.web.Bizagi2.entities.RolAcceso;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Respuesta de registro y login: el token y los datos basicos de la sesion (nunca la contrasena)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Integer usuarioId;
    private String nombre;
    private String email;
    private RolAcceso rolAcceso;
    private Integer empresaId;
    private String empresa;
}
