package desarrollo.web.Bizagi2.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import desarrollo.web.Bizagi2.entities.Historial;
import desarrollo.web.Bizagi2.entities.RolProceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.RolProcesoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

// Roles de proceso: son de la empresa y se usan en las lanes de cualquiera de sus procesos
@RestController
@RequestMapping("/api/roles-proceso")
@RequiredArgsConstructor
@Tag(name = "Roles de proceso")
public class RolProcesoController {

    private final RolProcesoService rolProcesoService;

    // Paginado, con busqueda por nombre. Cada rol indica en que procesos se usa y si se puede eliminar.
    @GetMapping
    public ResponseEntity<PagedModel<Map<String, Object>>> listar(@AuthenticationPrincipal Usuario usuario,
            @RequestParam(required = false) String nombre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new PagedModel<>(
                rolProcesoService.listar(usuario.getEmpresa().getId(), nombre, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detalle(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id) {
        return ResponseEntity.ok(rolProcesoService.detalle(id, usuario.getEmpresa().getId()));
    }

    // Solo el ADMINISTRADOR. Body: nombre, descripcion
    @PostMapping
    public ResponseEntity<RolProceso> crear(@AuthenticationPrincipal Usuario usuario, @RequestBody RolProceso datos) {
        RolProceso creado = rolProcesoService.crear(usuario, datos);
        return ResponseEntity.created(URI.create("/api/roles-proceso/" + creado.getId())).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RolProceso> actualizar(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id, @RequestBody RolProceso datos) {
        return ResponseEntity.ok(rolProcesoService.actualizar(id, usuario, datos));
    }

    // Eliminacion logica. 409 (indicando en que procesos) si alguna lane lo usa.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal Usuario usuario, @PathVariable Integer id) {
        rolProcesoService.eliminar(id, usuario);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<List<Historial>> historial(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id) {
        return ResponseEntity.ok(rolProcesoService.historial(id, usuario.getEmpresa().getId()));
    }
}
