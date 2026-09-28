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

import desarrollo.web.Bizagi2.entities.Mensaje;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.MensajeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Mensajes")
public class MensajeController {

    private final MensajeService mensajeService;

    @GetMapping("/api/procesos/{procesoId}/mensajes")
    public ResponseEntity<List<Mensaje>> listar(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer procesoId) {
        return ResponseEntity.ok(mensajeService.listarPorProceso(procesoId, usuario.getEmpresa().getId()));
    }

    // Body: nombre, origen: { id } (evento de lanzamiento o actividad ENVIO), destinoPool: { id }, contenido.
    // Si el pool destino es un SISTEMA_EXTERNO: tipoDestino (CORREO | SERVICIO_WEB | COLA), momento y
    // accionFallo (CONTINUAR_FLUJO | DERIVAR_A_MANEJO_ERROR | FINALIZAR_PROCESO).
    @PostMapping("/api/procesos/{procesoId}/mensajes")
    public ResponseEntity<Mensaje> crear(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer procesoId, @RequestBody Mensaje datos) {
        Mensaje creado = mensajeService.crear(procesoId, usuario, datos);
        return ResponseEntity.created(URI.create("/api/mensajes/" + creado.getId())).body(creado);
    }

    @GetMapping("/api/mensajes/{id}")
    public ResponseEntity<Mensaje> buscar(@AuthenticationPrincipal Usuario usuario, @PathVariable Integer id) {
        return ResponseEntity.ok(mensajeService.buscarPorId(id, usuario.getEmpresa().getId()));
    }

    @PutMapping("/api/mensajes/{id}")
    public ResponseEntity<Mensaje> actualizar(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id, @RequestBody Mensaje datos) {
        return ResponseEntity.ok(mensajeService.actualizar(id, usuario, datos));
    }

    @DeleteMapping("/api/mensajes/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal Usuario usuario, @PathVariable Integer id) {
        mensajeService.eliminar(id, usuario);
        return ResponseEntity.noContent().build();
    }
}
