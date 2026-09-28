package desarrollo.web.Bizagi2.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

// Lee el header "Authorization: Bearer <token>", valida el token y deja al Usuario
// (cargado de la BD) como usuario autenticado. Asi el rol y la empresa siempre son los actuales.
// El token deja de servir si el usuario fue desactivado o si cerro sesion (cambia su version).
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.leerClaims(header.substring(7));
                Integer usuarioId = Integer.valueOf(claims.getSubject());
                Integer version = claims.get("v", Integer.class);
                Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
                if (usuario != null && usuario.isActivo() && version != null
                        && version == usuario.getVersionSesion()) {
                    var autenticacion = new UsernamePasswordAuthenticationToken(
                            usuario, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRolAcceso().name())));
                    SecurityContextHolder.getContext().setAuthentication(autenticacion);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
