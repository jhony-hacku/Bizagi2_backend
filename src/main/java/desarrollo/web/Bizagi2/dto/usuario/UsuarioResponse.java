package desarrollo.web.Bizagi2.dto.usuario;

import desarrollo.web.Bizagi2.entities.RolAcceso;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Lo que se devuelve de un usuario: sin contrasena, sin versionSesion y sin la empresa completa.
// ModelMapper llena empresaId a partir de usuario.getEmpresa().getId()
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {
    private Integer id;
    private String nombre;
    private String email;
    private RolAcceso rolAcceso;
    private boolean activo;
    private Integer empresaId;
}
