package desarrollo.web.Bizagi2.entities;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "correlaciones")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class Correlacion {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mensaje_id", nullable = false, unique = true)
    private Mensaje mensaje;

    // Clave de correlacion: un identificador de negocio o un campo de los datos del mensaje
    @Column(nullable = false)
    private String criterio;

    // Que hacer si el mensaje no corresponde a ningun caso en espera
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccionSinCaso accionSinCaso;

    // Control de concurrencia optimista: si dos transacciones modifican la misma fila,
    // la segunda falla con OptimisticLockException en lugar de sobrescribir los cambios
    @Version
    @Column(nullable = false)
    @ColumnDefault("0")
    private long version;
}
