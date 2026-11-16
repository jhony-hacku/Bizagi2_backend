package desarrollo.web.Bizagi2.security;

import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

/** Limite local por cuenta e IP. En varias replicas debe complementarse en el proxy. */
@Component
public class LoginIntentos {
    private static final int MAX_ENTRADAS = 4096;
    private static final long VENTANA_SEGUNDOS = 300;
    private final Map<String, Ventana> intentos = new HashMap<>();

    private record Ventana(long vence, int cantidad) { }

    public synchronized boolean permitir(String ip, String email) {
        if (email != null && email.length() > 254) return false;
        long ahora = Instant.now().getEpochSecond();
        intentos.values().removeIf(ventana -> ventana.vence() <= ahora);
        String cuenta = "cuenta:" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
        String origen = "ip:" + ip; // No se confia en X-Forwarded-For enviado por el cliente.
        if (intentos.size() + 2 > MAX_ENTRADAS
                || cantidad(cuenta) >= 10 || cantidad(origen) >= 100) {
            return false;
        }
        sumar(cuenta, ahora);
        sumar(origen, ahora);
        return true;
    }

    private int cantidad(String clave) {
        Ventana ventana = intentos.get(clave);
        return ventana == null ? 0 : ventana.cantidad();
    }

    private void sumar(String clave, long ahora) {
        Ventana anterior = intentos.get(clave);
        intentos.put(clave, anterior == null ? new Ventana(ahora + VENTANA_SEGUNDOS, 1)
                : new Ventana(anterior.vence(), anterior.cantidad() + 1));
    }
}
