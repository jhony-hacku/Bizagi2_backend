package desarrollo.web.Bizagi2.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import desarrollo.web.Bizagi2.entities.Arco;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.ArcoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Arcos")
public class ArcoController {

    private final ArcoService arcoService;

    @GetMapping("/api/pools/{poolId}/arcos")
    public ResponseEntity<List<Arco>> listar(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer poolId) {
        return ResponseEntity.ok(arcoService.listarPorPool(poolId, usuario.getEmpresa().getId()));
    }

    // Body: origen: { id }, destino: { id } (actividades, gateways o eventos del mismo pool),
    // etiqueta (opcional) y condicion (solo si sale de un gateway exclusivo o inclusivo)
    @PostMapping("/api/pools/{poolId}/arcos")
    public ResponseEntity<Arco> crear(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer poolId, @RequestBody Arco datos) {
        Arco creado = arcoService.crear(poolId, usuario, datos);
        return ResponseEntity.created(URI.create("/api/arcos/" + creado.getId())).body(creado);
    }

    @GetMapping("/api/arcos/{id}")
    public ResponseEntity<Arco> buscar(@AuthenticationPrincipal Usuario usuario, @PathVariable Integer id) {
        return ResponseEntity.ok(arcoService.buscarPorId(id, usuario.getEmpresa().getId()));
    }

    @PutMapping("/api/arcos/{id}")
    public ResponseEntity<Arco> actualizar(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id, @RequestBody Arco datos) {
        return ResponseEntity.ok(arcoService.actualizar(id, usuario, datos));
    }

    @DeleteMapping("/api/arcos/{id}")
    public ResponseEntity<Map<String, Object>> eliminar(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id) {
        return ResponseEntity.ok(arcoService.eliminar(id, usuario));
    }
}
