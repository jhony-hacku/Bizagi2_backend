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

import desarrollo.web.Bizagi2.dto.arco.ArcoRequest;
import desarrollo.web.Bizagi2.dto.arco.ArcoResponse;
import desarrollo.web.Bizagi2.dto.proceso.EliminacionResponse;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.ArcoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Arcos")
public class ArcoController {

    private final ArcoService arcoService;

    @GetMapping("/api/pools/{poolId}/arcos")
    public ResponseEntity<List<ArcoResponse>> listar(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer poolId) {
        return ResponseEntity.ok(arcoService.listarPorPoolDto(poolId, usuario.getEmpresa().getId()));
    }

    // Body: origenId, destinoId (actividades, gateways o eventos del mismo pool),
    // etiqueta (opcional) y condicion (solo si sale de un gateway exclusivo o inclusivo)
    @PostMapping("/api/pools/{poolId}/arcos")
    public ResponseEntity<ArcoResponse> crear(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer poolId, @Valid @RequestBody ArcoRequest datos) {
        ArcoResponse creado = arcoService.crear(poolId, usuario, datos);
        return ResponseEntity.created(URI.create("/api/arcos/" + creado.getId())).body(creado);
    }

    @GetMapping("/api/arcos/{id}")
    public ResponseEntity<ArcoResponse> buscar(@AuthenticationPrincipal Usuario usuario, @PathVariable Integer id) {
        return ResponseEntity.ok(arcoService.buscarPorIdDto(id, usuario.getEmpresa().getId()));
    }

    @PutMapping("/api/arcos/{id}")
    public ResponseEntity<ArcoResponse> actualizar(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id, @Valid @RequestBody ArcoRequest datos) {
        return ResponseEntity.ok(arcoService.actualizar(id, usuario, datos));
    }

    @DeleteMapping("/api/arcos/{id}")
    public ResponseEntity<EliminacionResponse> eliminar(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id) {
        return ResponseEntity.ok(arcoService.eliminar(id, usuario));
    }
}
