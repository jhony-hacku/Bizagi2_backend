package desarrollo.web.Bizagi2.entities;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "arcos")
@SQLRestriction("activo = true")
@Data
@NoArgsConstructor
public class Arco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "pool_id", nullable = false)
    private Pool pool;

    @ManyToOne
    @JoinColumn(name = "origen_id", nullable = false)
    private NodoFlujo origen;

    @ManyToOne
    @JoinColumn(name = "destino_id", nullable = false)
    private NodoFlujo destino;

    private String etiqueta;

    // Solo tiene sentido cuando el arco sale de un gateway exclusivo o inclusivo
    private String condicion;

    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean activo = true;
}
