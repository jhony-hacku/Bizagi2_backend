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
import org.springframework.web.bind.annotation.RestController;

import desarrollo.web.Bizagi2.entities.Lane;
import desarrollo.web.Bizagi2.entities.RolProceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.LaneService;
import desarrollo.web.Bizagi2.service.PoolService;
import desarrollo.web.Bizagi2.service.RolProcesoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Lanes")
public class LaneController {

    private final LaneService laneService;
    private final RolProcesoService rolProcesoService;
    private final PoolService poolService;

    @GetMapping("/api/pools/{poolId}/lanes")
    public ResponseEntity<List<Lane>> listar(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer poolId) {
        return ResponseEntity.ok(laneService.listarPorPool(poolId, usuario.getEmpresa().getId()));
    }

    // Los roles de proceso de la empresa: los unicos que se pueden asignar a las lanes del pool
    @GetMapping("/api/pools/{poolId}/roles-disponibles")
    public ResponseEntity<List<RolProceso>> rolesDisponibles(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer poolId) {
        poolService.buscarPorId(poolId, usuario.getEmpresa().getId());
        return ResponseEntity.ok(rolProcesoService.disponibles(usuario.getEmpresa().getId()));
    }

    // Body: rolProceso: { id } (obligatorio) y nombre (opcional: por defecto el nombre del rol)
    @PostMapping("/api/pools/{poolId}/lanes")
    public ResponseEntity<Lane> crear(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer poolId, @RequestBody Lane datos) {
        Lane creada = laneService.crear(poolId, usuario, datos);
        return ResponseEntity.created(URI.create("/api/lanes/" + creada.getId())).body(creada);
    }

    // Body: lista con los ids de todas las lanes del pool en el orden deseado, ej. [3, 1, 2]
    @PutMapping("/api/pools/{poolId}/lanes/orden")
    public ResponseEntity<List<Lane>> reordenar(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer poolId, @RequestBody List<Integer> idsEnOrden) {
        return ResponseEntity.ok(laneService.reordenar(poolId, usuario, idsEnOrden));
    }

    @GetMapping("/api/lanes/{id}")
    public ResponseEntity<Lane> buscar(@AuthenticationPrincipal Usuario usuario, @PathVariable Integer id) {
        return ResponseEntity.ok(laneService.buscarPorId(id, usuario.getEmpresa().getId()));
    }

    @PutMapping("/api/lanes/{id}")
    public ResponseEntity<Lane> actualizar(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id, @RequestBody Lane datos) {
        return ResponseEntity.ok(laneService.actualizar(id, usuario, datos));
    }

    // 409 si la lane tiene actividades: primero se reasignan a otra lane
    @DeleteMapping("/api/lanes/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal Usuario usuario, @PathVariable Integer id) {
        laneService.eliminar(id, usuario);
        return ResponseEntity.noContent().build();
    }
}
