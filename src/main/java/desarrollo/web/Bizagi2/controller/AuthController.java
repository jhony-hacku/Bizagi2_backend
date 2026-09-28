package desarrollo.web.Bizagi2.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacion")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Registrar una empresa y su administrador inicial",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(value = """
                            {
                              "nombreEmpresa": "Acme SAS",
                              "nit": "900123456",
                              "emailContacto": "contacto@acme.com",
                              "nombre": "Ana Perez",
                              "email": "ana@acme.com",
                              "password": "Clave12345"
                            }"""))))
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registrar(@RequestBody Map<String, String> datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(datos));
    }

    @Operation(
            summary = "Iniciar sesion y obtener el token JWT",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(value = """
                            {
                              "email": "ana@acme.com",
                              "password": "Clave12345"
                            }"""))))
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> datos) {
        return ResponseEntity.ok(authService.login(datos.get("email"), datos.get("password")));
    }

    @Operation(summary = "Cerrar sesion: el token actual (y los anteriores del usuario) dejan de ser validos")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Usuario usuario) {
        authService.logout(usuario);
        return ResponseEntity.noContent().build();
    }
}
