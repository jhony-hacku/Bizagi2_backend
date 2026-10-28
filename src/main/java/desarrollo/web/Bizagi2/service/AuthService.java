package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.Validaciones.requerido;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import desarrollo.web.Bizagi2.dto.auth.AuthResponse;
import desarrollo.web.Bizagi2.dto.auth.LoginRequest;
import desarrollo.web.Bizagi2.dto.auth.RegistroRequest;
import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.security.JwtService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final EmpresaService empresaService;
    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    // HU-01: registra una empresa nueva junto con su usuario administrador inicial.
    // Primero se validan todos los campos obligatorios y luego se crea todo.
    // si algo falla
    // (NIT o email repetido...) no queda nada a medias.
    public AuthResponse registrar(RegistroRequest datos) {
        requerido(datos.getNombreEmpresa(), "nombreEmpresa");
        requerido(datos.getNit(), "nit");
        requerido(datos.getEmailContacto(), "emailContacto");
        requerido(datos.getNombre(), "nombre");
        requerido(datos.getEmail(), "email");
        requerido(datos.getPassword(), "password");

        Empresa empresa = empresaService.crear(datos.getNombreEmpresa(), datos.getNit(),
                datos.getEmailContacto());

        Usuario admin = new Usuario();
        admin.setNombre(datos.getNombre());
        admin.setEmail(datos.getEmail());
        admin.setPassword(datos.getPassword());
        admin.setRolAcceso(RolAcceso.ADMINISTRADOR);

        return respuesta(usuarioService.crear(empresa.getId(), admin));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest datos) {
        return respuesta(usuarioService.autenticar(datos.getEmail(), datos.getPassword()));
    }

    public void logout(Usuario usuario) {
        usuarioService.cerrarSesion(usuario.getId());
    }

    // El token no es parte de la entidad, por eso esta respuesta se arma a mano
    private AuthResponse respuesta(Usuario usuario) {
        return new AuthResponse(
                jwtService.generarToken(usuario),
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRolAcceso(),
                usuario.getEmpresa().getId(),
                usuario.getEmpresa().getNombre());
    }
}
