package desarrollo.web.Bizagi2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Historial;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.repository.HistorialRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("HistorialServiceTest - Pruebas de trazabilidad y auditoría")
class HistorialServiceTest {

    @Mock
    private HistorialRepository historialRepository;

    @InjectMocks
    private HistorialService historialService;

    @Test
    @DisplayName("Registrar entrada en historial guarda usuario, proceso, acción y fecha")
    void registrar_guardaEntradaCorrecta() {
        Usuario usuario = new Usuario();
        usuario.setId(1);

        Proceso proceso = new Proceso();
        proceso.setId(10);

        historialService.registrar(usuario, proceso, AccionHistorial.CREAR, "ACTIVIDAD", 50, "Actividad creada");

        ArgumentCaptor<Historial> captor = ArgumentCaptor.forClass(Historial.class);
        verify(historialRepository).save(captor.capture());

        Historial guardado = captor.getValue();
        assertThat(guardado.getUsuario()).isEqualTo(usuario);
        assertThat(guardado.getProceso()).isEqualTo(proceso);
        assertThat(guardado.getAccion()).isEqualTo(AccionHistorial.CREAR);
        assertThat(guardado.getEntidad()).isEqualTo("ACTIVIDAD");
        assertThat(guardado.getEntidadId()).isEqualTo(50);
        assertThat(guardado.getDetalle()).isEqualTo("Actividad creada");
        assertThat(guardado.getFecha()).isNotNull();
    }

    @Test
    @DisplayName("Consultar historial de un proceso ordenado descendente")
    void delProceso_retornaHistorial() {
        when(historialRepository.findByProcesoIdOrderByFechaDescIdDesc(10)).thenReturn(List.of(new Historial()));

        List<Historial> lista = historialService.delProceso(10);

        assertThat(lista).hasSize(1);
    }

    @Test
    @DisplayName("Consultar historial de una entidad global (ej. ROL)")
    void deEntidad_retornaHistorialDeEntidad() {
        when(historialRepository.findByEntidadAndEntidadIdOrderByFechaDescIdDesc("ROL", 5))
                .thenReturn(List.of(new Historial()));

        List<Historial> lista = historialService.deEntidad("ROL", 5);

        assertThat(lista).hasSize(1);
    }
}
