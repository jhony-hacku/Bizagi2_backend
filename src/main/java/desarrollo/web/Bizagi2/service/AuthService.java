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
import desarrollo.web.Bizagi2.exception.SesionInvalidaException;
import desarrollo.web.Bizagi2.security.JwtService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final EmpresaService empresaService;
    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    // Lo que devuelve cada operacion de sesion: el cuerpo JSON (con el access token) y el refresh token,
    // que el controlador NO pone en el JSON sino en una cookie HttpOnly.
    public record Sesion(AuthResponse cuerpo, String refreshToken) {
    }

    // HU-01: registra una empresa nueva junto con su usuario administrador inicial.
    // Primero se validan todos los campos obligatorios y luego se crea todo.
    // si algo falla
    // (NIT o email repetido...) no queda nada a medias.
    public Sesion registrar(RegistroRequest datos) {
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

        return abrirSesion(usuarioService.crear(empresa.getId(), admin));
    }

    // Login: access token corto + refresh token largo (familia nueva).
    // No es readOnly: guarda el refresh token y bloquea la fila del usuario.
    public Sesion login(LoginRequest datos) {
        return abrirSesion(usuarioService.autenticar(datos.getEmail(), datos.getPassword()));
    }

    // Renovacion: valida y rota el refresh token; entrega un access nuevo y un refresh nuevo.
    // noRollbackFor vive en RefreshTokenService.rotar, que confirma la revocacion ante una reutilizacion.
    @Transactional(noRollbackFor = SesionInvalidaException.class)
    public Sesion refrescar(String refreshToken) {
        RefreshTokenService.Rotacion rotacion = refreshTokenService.rotar(refreshToken);
        return new Sesion(cuerpo(rotacion.usuario()), rotacion.refreshToken());
    }

    // Cierra la sesion: sube la version (mata los access tokens vivos) y borra los refresh tokens
    public void logout(Usuario usuario, String refreshToken) {
        if (usuario != null) {
            usuarioService.cerrarSesion(usuario.getId());
            refreshTokenService.cerrarSesiones(usuario.getId());
        } else {
            refreshTokenService.cerrarPorRefresh(refreshToken);
        }
    }

    private Sesion abrirSesion(Usuario usuario) {
        return new Sesion(cuerpo(usuario), refreshTokenService.iniciarFamilia(usuario));
    }

    // El token no es parte de la entidad, por eso esta respuesta se arma a mano
    private AuthResponse cuerpo(Usuario usuario) {
        return new AuthResponse(
                jwtService.generarAccessToken(usuario),
                "Bearer",
                jwtService.getAccessExpirationMs() / 1000,
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRolAcceso(),
                usuario.getEmpresa().getId(),
                usuario.getEmpresa().getNombre());
    }
}
