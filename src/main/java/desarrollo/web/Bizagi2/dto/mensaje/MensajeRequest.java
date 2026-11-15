package desarrollo.web.Bizagi2.dto.mensaje;

import desarrollo.web.Bizagi2.entities.AccionFallo;
import desarrollo.web.Bizagi2.entities.TipoDestino;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// HU-25 / HU-26: sale de un evento de lanzamiento o de una actividad ENVIO hacia otro pool.
// Si el pool destino es SISTEMA_EXTERNO: tipoDestino es obligatorio, y se documentan momento y accionFallo.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MensajeRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotNull(message = "El elemento que envia el mensaje es obligatorio")
    private Integer origenId;

    @NotNull(message = "El pool destino es obligatorio")
    private Integer destinoPoolId;
    private String contenido;
    private TipoDestino tipoDestino;
    private String momento;
    private AccionFallo accionFallo;

    // HU-26: obligatoria si accionFallo = DERIVAR_A_MANEJO_ERROR (actividad del mismo proceso)
    private Integer actividadErrorId;
}
