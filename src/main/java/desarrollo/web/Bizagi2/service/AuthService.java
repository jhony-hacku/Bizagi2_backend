package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.Validaciones.requerido;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public record Sesion(Map<String, Object> cuerpo, String refreshToken) {
    }

    // HU-01: registra una empresa nueva junto con su usuario administrador inicial.
    // Primero se validan todos los campos obligatorios y luego se crea todo; si algo falla
    // (NIT o email repetido...) no queda nada a medias.
    public Sesion registrar(Map<String, String> datos) {
        requerido(datos.get("nombreEmpresa"), "nombreEmpresa");
        requerido(datos.get("nit"), "nit");
        requerido(datos.get("emailContacto"), "emailContacto");
        requerido(datos.get("nombre"), "nombre");
        requerido(datos.get("email"), "email");
        requerido(datos.get("password"), "password");

        Empresa empresa = empresaService.crear(datos.get("nombreEmpresa"), datos.get("nit"),
                datos.get("emailContacto"));

        Usuario admin = new Usuario();
        admin.setNombre(datos.get("nombre"));
        admin.setEmail(datos.get("email"));
        admin.setPassword(datos.get("password"));
        admin.setRolAcceso(RolAcceso.ADMINISTRADOR);

        return abrirSesion(usuarioService.crear(empresa.getId(), admin));
    }

    // Login: access token corto + refresh token largo (familia nueva)
    public Sesion login(String email, String password) {
        return abrirSesion(usuarioService.autenticar(email, password));
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

    private Map<String, Object> cuerpo(Usuario usuario) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("token", jwtService.generarAccessToken(usuario));
        respuesta.put("tokenType", "Bearer");
        respuesta.put("expiresIn", jwtService.getAccessExpirationMs() / 1000);
        respuesta.put("usuarioId", usuario.getId());
        respuesta.put("nombre", usuario.getNombre());
        respuesta.put("email", usuario.getEmail());
        respuesta.put("rolAcceso", usuario.getRolAcceso());
        respuesta.put("empresaId", usuario.getEmpresa().getId());
        respuesta.put("empresa", usuario.getEmpresa().getNombre());
        return respuesta;
    }
}
