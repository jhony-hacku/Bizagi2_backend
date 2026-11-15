package desarrollo.web.Bizagi2.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenLimpieza - tarea programada")
class RefreshTokenLimpiezaTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private RefreshTokenLimpieza limpieza;

    @Test
    @DisplayName("limpiar delega en el servicio el borrado de los refresh tokens vencidos")
    void limpiar() {
        when(refreshTokenService.limpiarVencidos()).thenReturn(3);

        limpieza.limpiar();

        verify(refreshTokenService).limpiarVencidos();
    }
}
