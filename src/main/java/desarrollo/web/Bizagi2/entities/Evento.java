package desarrollo.web.Bizagi2.entities;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

// Eventos del diagrama. Para los de mensaje (lanzamiento y recepcion) el nombre es el nombre del mensaje.
@Entity
@DiscriminatorValue("EVENTO")
@Data
@EqualsAndHashCode(callSuper = true)
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
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "evento_actividades_usuarias", joinColumns = @JoinColumn(name = "evento_id"))
    @Column(name = "actividad_id")
    private Set<Integer> actividadesUsuarias = new HashSet<>();
}
