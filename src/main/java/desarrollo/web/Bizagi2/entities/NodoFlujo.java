package desarrollo.web.Bizagi2.entities;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

// Clase padre de todo lo que se dibuja y se puede conectar con arcos: Actividad, Gateway y Evento
@Entity
@Table(name = "nodos_flujo")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_nodo")
@SQLRestriction("activo = true")
@Data
@NoArgsConstructor
public class NodoFlujo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "pool_id", nullable = false)
    private Pool pool;

    private String nombre;
    private double posicionX;
    private double posicionY;

    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean activo = true;
}
