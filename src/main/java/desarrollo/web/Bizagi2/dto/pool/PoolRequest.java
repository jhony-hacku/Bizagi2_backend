package desarrollo.web.Bizagi2.dto.pool;

import desarrollo.web.Bizagi2.entities.TipoParticipante;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Los participantes externos (CLIENTE, PROVEEDOR, SISTEMA_EXTERNO) son cajas negras
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoolRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotNull(message = "El tipo de participante es obligatorio")
    private TipoParticipante tipoParticipante;
}
