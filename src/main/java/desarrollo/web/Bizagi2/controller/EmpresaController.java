package desarrollo.web.Bizagi2.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.EmpresaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/empresa")
@RequiredArgsConstructor
@Tag(name = "Empresa")
public class EmpresaController {

    private final EmpresaService empresaService;

    @GetMapping
    public ResponseEntity<Empresa> miEmpresa(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(empresaService.buscarPorId(usuario.getEmpresa().getId()));
    }

    @PutMapping
    public ResponseEntity<Empresa> actualizar(@AuthenticationPrincipal Usuario usuario, @RequestBody Empresa datos) {
        return ResponseEntity.ok(empresaService.actualizar(usuario.getEmpresa().getId(), datos));
    }
}
