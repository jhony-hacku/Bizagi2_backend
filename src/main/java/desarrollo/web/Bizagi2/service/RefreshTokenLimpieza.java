package desarrollo.web.Bizagi2.service;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Borra cada madrugada los refresh tokens vencidos para que la tabla no crezca sin limite
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class RefreshTokenLimpieza {

    private final RefreshTokenService refreshTokenService;

    @Scheduled(cron = "0 0 3 * * *")
    public void limpiar() {
        int borrados = refreshTokenService.limpiarVencidos();
        log.info("Refresh tokens vencidos eliminados: {}", borrados);
    }
}
