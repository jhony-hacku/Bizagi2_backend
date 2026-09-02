package desarrollo.web.Bizagi2.presentation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import desarrollo.web.Bizagi2.infrastructure.persistence.SpringDataUserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataUserJpaRepository jpaRepository;

    @BeforeEach
    void cleanData() {
        jpaRepository.deleteAll();
    }

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"john\",\"email\":\"john@example.com\",\"password\":\"MySecurePassword123\"}")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(notNullValue()))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        String body = "{\"username\":\"john\",\"email\":\"john@example.com\",\"password\":\"MySecurePassword123\"}";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void shouldRejectInvalidRegisterData() throws Exception {
        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"\",\"email\":\"invalid\",\"password\":\"123\"}")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldStorePasswordAsHash() throws Exception {
        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"john\",\"email\":\"john@example.com\",\"password\":\"MySecurePassword123\"}")
                )
                .andExpect(status().isCreated());

        String hash = jpaRepository.findByEmail("john@example.com").orElseThrow().getPasswordHash();
        org.junit.jupiter.api.Assertions.assertNotEquals("MySecurePassword123", hash);
    }

    @Test
    void shouldLoginSuccessfullyAndReturnJwt() throws Exception {
        registerUser();
        String loginResponse = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"john@example.com\",\"password\":\"MySecurePassword123\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not("")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(loginResponse);
        org.junit.jupiter.api.Assertions.assertNotNull(json.get("token").asText());
    }

    @Test
    void shouldRejectWrongPassword() throws Exception {
        registerUser();

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"john@example.com\",\"password\":\"WrongPassword123\"}")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void shouldRejectNonExistingUser() throws Exception {
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"noone@example.com\",\"password\":\"MySecurePassword123\"}")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void shouldAllowPublicEndpointWithoutJwt() throws Exception {
        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"public\",\"email\":\"public@example.com\",\"password\":\"MySecurePassword123\"}")
                )
                .andExpect(status().isCreated());
    }

    @Test
    void shouldRejectProtectedEndpointWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowProtectedEndpointWithJwt() throws Exception {
        registerUser();

        String loginResponse = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"john@example.com\",\"password\":\"MySecurePassword123\"}")
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(loginResponse).get("token").asText();

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.principal").value("john@example.com"));
    }

    private void registerUser() throws Exception {
        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"john\",\"email\":\"john@example.com\",\"password\":\"MySecurePassword123\"}")
                )
                .andExpect(status().isCreated());
    }
}
