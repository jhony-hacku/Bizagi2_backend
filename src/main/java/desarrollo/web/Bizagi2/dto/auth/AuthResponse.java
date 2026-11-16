package desarrollo.web.Bizagi2.dto.auth;

import desarrollo.web.Bizagi2.entities.RolAcceso;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Respuesta de registro, login y refresh: el access token y los datos basicos de la sesion.
// Nunca incluye la contrasena ni el refresh token (ese viaja solo en la cookie HttpOnly)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType;
    // Segundos de vida del access token
    private long expiresIn;
    private Integer usuarioId;
    private String nombre;
    private String email;
    private RolAcceso rolAcceso;
    private Integer empresaId;
    private String empresa;
}
