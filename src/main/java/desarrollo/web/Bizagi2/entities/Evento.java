package desarrollo.web.Bizagi2.entities;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Eventos del diagrama. Para los de mensaje (lanzamiento y recepcion) el nombre es el nombre del mensaje.
@Entity
@DiscriminatorValue("EVENTO")
@Getter
@Setter
@NoArgsConstructor
public class Evento extends NodoFlujo {

    @Enumerated(EnumType.STRING)
    private TipoEvento tipoEvento;

    // Campos que se envian o que se esperan recibir, con su tipo de dato (ej. "radicado:texto; monto:numero")
    @Column(length = 2000)
    private String contenido;

    // Solo recepcion: el mensaje llega de un participante externo, sin lanzamiento dentro del diagrama
    private Boolean origenExterno;

    // Clave con la que el mensaje se asocia a un caso concreto (HU-28)
    private String claveCorrelacion;

    // Solo recepcion: ids de las actividades del proceso que usan los datos recibidos
    // Muchos eventos pueden usar muchas actividades: tabla intermedia evento_actividades_usuarias.
    // Unidireccional (Actividad no la conoce) y con Set para no repetir actividades.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "evento_actividades_usuarias",
            joinColumns = @JoinColumn(name = "evento_id"),
            inverseJoinColumns = @JoinColumn(name = "actividad_id"))
    private Set<Actividad> actividadesUsuarias = new HashSet<>();
}
