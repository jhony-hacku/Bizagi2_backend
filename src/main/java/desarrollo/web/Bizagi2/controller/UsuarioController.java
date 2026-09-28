package desarrollo.web.Bizagi2.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.UsuarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    // Cualquier rol puede consultar su propio perfil
    @GetMapping("/me")
    public ResponseEntity<Usuario> me(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(usuario);
    }

    // El resto de rutas son solo para el ADMINISTRADOR (ver SecurityConfig)
    @GetMapping
    public ResponseEntity<List<Usuario>> listar(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(usuarioService.listar(usuario.getEmpresa().getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> buscar(@AuthenticationPrincipal Usuario usuario, @PathVariable Integer id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id, usuario.getEmpresa().getId()));
    }

    @PostMapping
    public ResponseEntity<Usuario> crear(@AuthenticationPrincipal Usuario usuario, @RequestBody Usuario datos) {
        Usuario creado = usuarioService.crear(usuario.getEmpresa().getId(), datos);
        return ResponseEntity.created(URI.create("/api/usuarios/" + creado.getId())).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizar(@AuthenticationPrincipal Usuario usuario, @PathVariable Integer id,
            @RequestBody Usuario datos) {
        return ResponseEntity.ok(usuarioService.actualizar(id, usuario.getEmpresa().getId(), datos));
    }

    // No borra: desactiva al usuario (conserva su historial) y cierra sus sesiones
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@AuthenticationPrincipal Usuario usuario, @PathVariable Integer id) {
        usuarioService.desactivar(id, usuario.getEmpresa().getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activar")
    public ResponseEntity<Usuario> activar(@AuthenticationPrincipal Usuario usuario, @PathVariable Integer id) {
        return ResponseEntity.ok(usuarioService.activar(id, usuario.getEmpresa().getId()));
    }
}
