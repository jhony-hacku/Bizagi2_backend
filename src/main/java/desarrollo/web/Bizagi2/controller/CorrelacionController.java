package desarrollo.web.Bizagi2.controller;

import java.net.URI;

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

import desarrollo.web.Bizagi2.entities.Correlacion;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.CorrelacionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

// La correlacion se maneja como un subrecurso del mensaje (uno por mensaje)
@RestController
@RequestMapping("/api/mensajes/{mensajeId}/correlacion")
@RequiredArgsConstructor
@Tag(name = "Correlaciones")
public class CorrelacionController {

    private final CorrelacionService correlacionService;

    @GetMapping
    public ResponseEntity<Correlacion> obtener(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer mensajeId) {
        return ResponseEntity.ok(correlacionService.obtener(mensajeId, usuario.getEmpresa().getId()));
    }

    // Body: criterio (la clave de correlacion) y accionSinCaso (DESCARTAR | INICIAR_CASO_NUEVO)
    @PostMapping
    public ResponseEntity<Correlacion> crear(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer mensajeId, @RequestBody Correlacion datos) {
        Correlacion creada = correlacionService.crear(mensajeId, usuario, datos);
        return ResponseEntity.created(URI.create("/api/mensajes/" + mensajeId + "/correlacion")).body(creada);
    }

    @PutMapping
    public ResponseEntity<Correlacion> actualizar(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer mensajeId, @RequestBody Correlacion datos) {
        return ResponseEntity.ok(correlacionService.actualizar(mensajeId, usuario, datos));
    }

    @DeleteMapping
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal Usuario usuario, @PathVariable Integer mensajeId) {
        correlacionService.eliminar(mensajeId, usuario);
        return ResponseEntity.noContent().build();
    }
}
