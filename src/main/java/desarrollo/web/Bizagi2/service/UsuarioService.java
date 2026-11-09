package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.Validaciones.emailValido;
import static desarrollo.web.Bizagi2.service.Validaciones.passwordValido;
import static desarrollo.web.Bizagi2.service.Validaciones.requerido;

import java.util.List;
import java.util.Locale;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.repository.UsuarioRepository;
import desarrollo.web.Bizagi2.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaService empresaService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    private static final String HASH_FICTICIO = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
            .encode("comparacion-para-usuario-inexistente");

    // Incluye a los desactivados, para que el administrador pueda volver a activarlos
    @Transactional(readOnly = true)
    public List<Usuario> listar(Integer empresaId) {
        return usuarioRepository.findByEmpresaId(empresaId);
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorId(Integer id, Integer empresaId) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        if (!usuario.getEmpresa().getId().equals(empresaId)) {
            throw new RecursoNoEncontradoException("Usuario no encontrado");
        }
        return usuario;
    }

    // Crea un usuario dentro de la empresa indicada (la empresa nunca viene del cliente)
    public Usuario crear(Integer empresaId, Usuario datos) {
        requerido(datos.getNombre(), "nombre");
        requerido(datos.getEmail(), "email");
        requerido(datos.getPassword(), "password");
        requerido(datos.getRolAcceso(), "rolAcceso");
        emailValido(datos.getEmail().trim());
        passwordValido(datos.getPassword());

        String email = datos.getEmail().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictoDominioException("El email ya esta registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setEmpresa(empresaService.buscarPorId(empresaId));
        usuario.setNombre(datos.getNombre().trim());
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(datos.getPassword()));
        usuario.setRolAcceso(datos.getRolAcceso());
        usuario.setActivo(true);
        return usuarioRepository.save(usuario);
    }

    // Solo cambia los campos que vengan con valor; la contrasena es opcional
    public Usuario actualizar(Integer id, Integer empresaId, Usuario datos) {
        Usuario usuario = bloquearEnEmpresa(id, empresaId);

        if (datos.getNombre() != null && !datos.getNombre().isBlank()) {
            usuario.setNombre(datos.getNombre().trim());
        }
        if (datos.getEmail() != null && !datos.getEmail().isBlank()) {
            emailValido(datos.getEmail().trim());
            String email = datos.getEmail().trim().toLowerCase(Locale.ROOT);
            if (!email.equals(usuario.getEmail()) && usuarioRepository.existsByEmail(email)) {
                throw new ConflictoDominioException("El email ya esta registrado");
            }
            usuario.setEmail(email);
        }
        if (datos.getPassword() != null && !datos.getPassword().isBlank()) {
            passwordValido(datos.getPassword());
            usuario.setPassword(passwordEncoder.encode(datos.getPassword()));
            usuario.setVersionSesion(usuario.getVersionSesion() + 1);
            refreshTokenRepository.eliminarDeUsuario(id);
        }
        if (datos.getRolAcceso() != null && datos.getRolAcceso() != usuario.getRolAcceso()) {
            if (usuario.getRolAcceso() == RolAcceso.ADMINISTRADOR && usuario.isActivo()) {
                verificarQueQuedaOtroAdministrador(usuario, empresaId);
            }
            usuario.setRolAcceso(datos.getRolAcceso());
        }
        return usuarioRepository.save(usuario);
    }

    // HU-02: se desactiva, no se borra. Su historial y sus procesos siguen intactos.
    // Ademas se cierran sus sesiones abiertas.
    public void desactivar(Integer id, Integer empresaId) {
        Usuario usuario = bloquearEnEmpresa(id, empresaId);
        if (usuario.getRolAcceso() == RolAcceso.ADMINISTRADOR && usuario.isActivo()) {
            verificarQueQuedaOtroAdministrador(usuario, empresaId);
        }
        usuario.setActivo(false);
        usuario.setVersionSesion(usuario.getVersionSesion() + 1);
        refreshTokenRepository.eliminarDeUsuario(id);
        usuarioRepository.save(usuario);
    }

    public Usuario activar(Integer id, Integer empresaId) {
        Usuario usuario = bloquearEnEmpresa(id, empresaId);
        usuario.setActivo(true);
        return usuarioRepository.save(usuario);
    }

    // HU-03: verifica el email y la contrasena. El mensaje es siempre el mismo (email inexistente,
    // contrasena incorrecta o usuario desactivado) para no revelar que emails existen.
    public Usuario autenticar(String email, String password) {
        requerido(email, "email");
        requerido(password, "password");
        Usuario usuario = usuarioRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
        boolean coincide = passwordEncoder.matches(password, usuario == null ? HASH_FICTICIO : usuario.getPassword());
        if (usuario == null || !usuario.isActivo() || !coincide) {
            throw new BadCredentialsException("Credenciales invalidas");
        }
        return usuario;
    }

    // HU-03: cerrar sesion. Al subir el contador, los tokens emitidos antes dejan de ser validos.
    public void cerrarSesion(Integer usuarioId) {
        Usuario usuario = usuarioRepository.buscarParaActualizar(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        usuario.setVersionSesion(usuario.getVersionSesion() + 1);
        usuarioRepository.save(usuario);
    }

    private Usuario bloquearEnEmpresa(Integer id, Integer empresaId) {
        Usuario usuario = usuarioRepository.buscarParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        if (!usuario.getEmpresa().getId().equals(empresaId)) {
            throw new RecursoNoEncontradoException("Usuario no encontrado");
        }
        return usuario;
    }

    // La empresa siempre debe conservar un administrador activo
    private void verificarQueQuedaOtroAdministrador(Usuario usuario, Integer empresaId) {
        boolean hayOtro = usuarioRepository.findByEmpresaId(empresaId).stream()
                .anyMatch(u -> u.getRolAcceso() == RolAcceso.ADMINISTRADOR && u.isActivo()
                        && !u.getId().equals(usuario.getId()));
        if (!hayOtro) {
            throw new ConflictoDominioException("La empresa debe conservar al menos un administrador activo");
        }
    }
}
