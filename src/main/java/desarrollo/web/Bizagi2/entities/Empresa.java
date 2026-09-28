package desarrollo.web.Bizagi2.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "empresas")
@Data
@NoArgsConstructor
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nombre;
    private String nit;
    private String emailContacto;

    // HU-24: indica si el EDITOR puede crear, editar y eliminar pools y lanes.
    // El ADMINISTRADOR siempre puede y el LECTOR nunca. Vacio (null) se trata como true.
    private Boolean editorModificaEstructura = true;
}
