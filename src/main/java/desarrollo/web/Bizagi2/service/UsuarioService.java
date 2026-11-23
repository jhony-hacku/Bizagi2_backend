package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.Validaciones.emailValido;
import static desarrollo.web.Bizagi2.service.Validaciones.passwordValido;
import static desarrollo.web.Bizagi2.service.Validaciones.requerido;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import desarrollo.web.Bizagi2.dto.usuario.UsuarioCreateRequest;
import desarrollo.web.Bizagi2.dto.usuario.UsuarioResponse;
import desarrollo.web.Bizagi2.dto.usuario.UsuarioUpdateRequest;
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
    private final ModelMapper modelMapper;

    private static final String USUARIO_NO_ENCONTRADO = "Usuario no encontrado";

    // Hash de un valor aleatorio que nadie conoce: si el email no existe se compara contra el,
    // para que el login tarde lo mismo exista o no la cuenta (no revela que emails estan registrados)
    private static final String HASH_FICTICIO = new BCryptPasswordEncoder().encode(UUID.randomUUID().toString());

    // Incluye a los desactivados, para que el administrador pueda volver a activarlos
    public List<Usuario> listar(Integer empresaId) {
        return usuarioRepository.findByEmpresaId(empresaId);
    }

    public Usuario buscarPorId(Integer id, Integer empresaId) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(USUARIO_NO_ENCONTRADO));
        if (!usuario.getEmpresa().getId().equals(empresaId)) {
            throw new RecursoNoEncontradoException(USUARIO_NO_ENCONTRADO);
        }
        return usuario;
    }

    // Crea un usuario dentro de la empresa indicada (la empresa nunca viene del cliente)
    public Usuario crear(Integer empresaId, Usuario datos) {
        verificarRolAsignable(datos.getRolAcceso());
        return persistirNuevo(empresaId, datos, datos.getRolAcceso());
    }

    // Lo usa el registro de empresa dentro de la misma transaccion que la crea.
    public Usuario crearCreador(Integer empresaId, Usuario datos) {
        return persistirNuevo(empresaId, datos, RolAcceso.CREADOR);
    }

    private Usuario persistirNuevo(Integer empresaId, Usuario datos, RolAcceso rol) {
        requerido(datos.getNombre(), "nombre");
        requerido(datos.getEmail(), "email");
        requerido(datos.getPassword(), "password");
        requerido(rol, "rolAcceso");
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
        usuario.setRolAcceso(rol);
        usuario.setActivo(true);
        return usuarioRepository.save(usuario);
    }

    // Solo cambia los campos que vengan con valor; la contrasena es opcional
    public Usuario actualizar(Integer id, Integer empresaId, Integer actorId, Usuario datos) {
        Usuario usuario = bloquearEnEmpresa(id, empresaId);
        if (!id.equals(actorId)) {
            protegerCreador(usuario);
        }
        RolAcceso nuevoRol = datos.getRolAcceso();
        if (nuevoRol != null && nuevoRol != usuario.getRolAcceso()) {
            protegerCreador(usuario);
            verificarRolAsignable(nuevoRol);
            if (usuario.getRolAcceso() == RolAcceso.ADMINISTRADOR && usuario.isActivo()) {
                verificarQueQuedaOtroAdministrador(usuario, empresaId);
            }
            usuario.setRolAcceso(nuevoRol);
        }

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
        return usuarioRepository.save(usuario);
    }

    // HU-02: se desactiva, no se borra. Su historial y sus procesos siguen intactos.
    // Ademas se cierran sus sesiones abiertas.
    public void desactivar(Integer id, Integer empresaId) {
        Usuario usuario = bloquearEnEmpresa(id, empresaId);
        protegerCreador(usuario);
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
        protegerCreador(usuario);
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
                .orElseThrow(() -> new RecursoNoEncontradoException(USUARIO_NO_ENCONTRADO));
        usuario.setVersionSesion(usuario.getVersionSesion() + 1);
        usuarioRepository.save(usuario);
    }

    private Usuario bloquearEnEmpresa(Integer id, Integer empresaId) {
        Usuario usuario = usuarioRepository.buscarParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(USUARIO_NO_ENCONTRADO));
        if (!usuario.getEmpresa().getId().equals(empresaId)) {
            throw new RecursoNoEncontradoException(USUARIO_NO_ENCONTRADO);
        }
        return usuario;
    }

    private void protegerCreador(Usuario usuario) {
        if (usuario.getRolAcceso() == RolAcceso.CREADOR) {
            throw new AccessDeniedException("La cuenta del creador de la empresa esta protegida");
        }
    }

    private void verificarRolAsignable(RolAcceso rol) {
        if (rol == RolAcceso.CREADOR) {
            throw new AccessDeniedException("El rol de creador se asigna al registrar la empresa");
        }
    }

    // La empresa siempre debe conservar una cuenta activa con permisos de administracion.
    private void verificarQueQuedaOtroAdministrador(Usuario usuario, Integer empresaId) {
        boolean hayOtro = usuarioRepository.findByEmpresaId(empresaId).stream()
                .anyMatch(u -> u.isActivo() && u.getRolAcceso().puedeAdministrar()
                        && !u.getId().equals(usuario.getId()));
        if (!hayOtro) {
            throw new ConflictoDominioException("La empresa debe conservar al menos un administrador activo");
        }
    }

    // ---------- Frontera con el controlador: entra y sale DTO, nunca la entidad ----------
    // Las reglas de negocio siguen en los metodos de arriba; estos solo convierten con ModelMapper.

    public UsuarioResponse aResponse(Usuario usuario) {
        return modelMapper.map(usuario, UsuarioResponse.class);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarDto(Integer empresaId) {
        return listar(empresaId).stream().map(this::aResponse).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorIdDto(Integer id, Integer empresaId) {
        return aResponse(buscarPorId(id, empresaId));
    }

    public UsuarioResponse crear(Integer empresaId, UsuarioCreateRequest datos) {
        return aResponse(crear(empresaId, modelMapper.map(datos, Usuario.class)));
    }

    public UsuarioResponse actualizar(Integer id, Integer empresaId, Integer actorId, UsuarioUpdateRequest datos) {
        return aResponse(actualizar(id, empresaId, actorId, modelMapper.map(datos, Usuario.class)));
    }

    public UsuarioResponse activarDto(Integer id, Integer empresaId) {
        return aResponse(activar(id, empresaId));
    }
}
