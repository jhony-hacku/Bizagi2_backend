package desarrollo.web.Bizagi2.dto.proceso;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// HU-23: una empresa con la que se comparte el proceso
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaInvitadaResponse {
    private Integer empresaId;
    private String empresa;
    private String nit;
    private Instant compartidoDesde;
}
