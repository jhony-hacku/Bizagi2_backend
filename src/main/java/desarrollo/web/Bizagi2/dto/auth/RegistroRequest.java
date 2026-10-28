package desarrollo.web.Bizagi2.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// HU-01: lo que envia el cliente para registrar una empresa y su administrador inicial
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroRequest {

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(max = 150, message = "El nombre de la empresa no puede superar 150 caracteres")
    private String nombreEmpresa;

    @NotBlank(message = "El NIT es obligatorio")
    private String nit;

    @NotBlank(message = "El correo de contacto es obligatorio")
    @Email(message = "El correo de contacto no tiene un formato valido")
    private String emailContacto;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato valido")
    private String email;

    @NotBlank(message = "La contrasena es obligatoria")
    private String password;
}
