package desarrollo.web.Bizagi2.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Operation(summary = "Get current authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("principal", authentication.getName());
        response.put(
                "authorities",
                authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList())
        );
        return response;
    }
}
