package desarrollo.web.Bizagi2.dto.empresa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaResponse {
    private Integer id;
    private String nombre;
    private String nit;
    private String emailContacto;
    private Boolean editorModificaEstructura;
}
