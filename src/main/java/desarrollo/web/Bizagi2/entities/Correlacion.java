package desarrollo.web.Bizagi2.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "correlaciones")
@Data
@NoArgsConstructor
public class Correlacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "mensaje_id", nullable = false)
    private Mensaje mensaje;

    // Clave de correlacion: un identificador de negocio o un campo de los datos del mensaje
    private String criterio;

    // Que hacer si el mensaje no corresponde a ningun caso en espera
    @Enumerated(EnumType.STRING)
    private AccionSinCaso accionSinCaso;
}
