package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.Validaciones.requerido;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    // Primero se validan todos los campos obligatorios y luego se crea todo; si algo falla
    // (NIT o email repetido...) no queda nada a medias.
    public Map<String, Object> registrar(Map<String, String> datos) {
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

        return respuesta(usuarioService.crear(empresa.getId(), admin));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> login(String email, String password) {
        return respuesta(usuarioService.autenticar(email, password));
    }

    public void logout(Usuario usuario) {
        usuarioService.cerrarSesion(usuario.getId());
    }

    private Map<String, Object> respuesta(Usuario usuario) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("token", jwtService.generarToken(usuario));
        respuesta.put("usuarioId", usuario.getId());
        respuesta.put("nombre", usuario.getNombre());
        respuesta.put("email", usuario.getEmail());
        respuesta.put("rolAcceso", usuario.getRolAcceso());
        respuesta.put("empresaId", usuario.getEmpresa().getId());
        respuesta.put("empresa", usuario.getEmpresa().getNombre());
        return respuesta;
    }
}
