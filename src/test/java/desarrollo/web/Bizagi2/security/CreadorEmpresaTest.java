package desarrollo.web.Bizagi2.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import desarrollo.web.Bizagi2.dto.auth.LoginRequest;
import desarrollo.web.Bizagi2.dto.auth.RegistroRequest;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.service.AuthService;
import jakarta.servlet.Filter;

@SpringBootTest
@ActiveProfiles("test")
class CreadorEmpresaTest {

    @Autowired AuthService auth;
    @Autowired WebApplicationContext contexto;
    @Autowired @Qualifier("springSecurityFilterChain") Filter seguridad;

    @Test
    void laCuentaCreadoraSeProtegeEnLaApiYConservaPermisosDeAdministracion() throws Exception {
        var api = MockMvcBuilders.webAppContextSetup(contexto).addFilters(seguridad).build();
        String sufijo = UUID.randomUUID().toString();
        String email = sufijo + "@example.com";
        String emailAdmin = "admin-" + email;
        String password = "Password123!";
        var creador = auth.registrar(new RegistroRequest("Empresa de prueba", sufijo, email,
                "Creador de prueba", email, password)).cuerpo();
        assertThat(creador.getRolAcceso()).isEqualTo(RolAcceso.CREADOR);
        String tokenCreador = "Bearer " + creador.getToken();
        String rutaCreador = "/api/usuarios/" + creador.getUsuarioId();

        api.perform(post("/api/usuarios").header("Authorization", tokenCreador)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                         {"nombre":"Administrador","email":"%s","password":"%s","rolAcceso":"ADMINISTRADOR"}
                         """.formatted(emailAdmin, password)))
                .andExpect(status().isCreated());

        var admin = auth.login(new LoginRequest(emailAdmin, password)).cuerpo();
        String tokenAdmin = "Bearer " + admin.getToken();
        String rutaAdmin = "/api/usuarios/" + admin.getUsuarioId();
        api.perform(get("/api/usuarios").header("Authorization", tokenAdmin)).andExpect(status().isOk());
        api.perform(delete(rutaCreador).header("Authorization", tokenAdmin)).andExpect(status().isForbidden());
        api.perform(put(rutaCreador + "/activar").header("Authorization", tokenAdmin))
                .andExpect(status().isForbidden());
        api.perform(put(rutaCreador).header("Authorization", tokenAdmin).contentType(MediaType.APPLICATION_JSON)
                .content("""
                         {"email":"otro@example.com","password":"OtraPassword123!"}
                         """))
                .andExpect(status().isForbidden());
        api.perform(put(rutaCreador).header("Authorization", tokenAdmin).contentType(MediaType.APPLICATION_JSON)
                .content("""
                         {"rolAcceso":"LECTOR"}
                         """))
                .andExpect(status().isForbidden());
        api.perform(put(rutaAdmin).header("Authorization", tokenAdmin).contentType(MediaType.APPLICATION_JSON)
                .content("""
                         {"rolAcceso":"CREADOR"}
                         """))
                .andExpect(status().isForbidden());
        api.perform(post("/api/usuarios").header("Authorization", tokenCreador)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                         {"nombre":"Otro creador","email":"otro@example.com","password":"Password123!","rolAcceso":"CREADOR"}
                         """))
                .andExpect(status().isForbidden());

        api.perform(put(rutaCreador).header("Authorization", tokenCreador).contentType(MediaType.APPLICATION_JSON)
                .content("""
                         {"nombre":"Perfil actualizado"}
                         """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Perfil actualizado"))
                .andExpect(jsonPath("$.rolAcceso").value("CREADOR"));
        api.perform(delete(rutaAdmin).header("Authorization", tokenCreador)).andExpect(status().isNoContent());
        api.perform(delete(rutaCreador).header("Authorization", tokenCreador)).andExpect(status().isForbidden());
        assertThat(auth.login(new LoginRequest(email, password)).cuerpo().getRolAcceso())
                .isEqualTo(RolAcceso.CREADOR);
    }
}
