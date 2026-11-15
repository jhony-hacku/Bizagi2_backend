package desarrollo.web.Bizagi2.service;

import java.time.Instant;

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.AccionSinCaso;
import desarrollo.web.Bizagi2.entities.Actividad;
import desarrollo.web.Bizagi2.entities.Arco;
import desarrollo.web.Bizagi2.entities.Correlacion;
import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.EstadoProceso;
import desarrollo.web.Bizagi2.entities.Evento;
import desarrollo.web.Bizagi2.entities.Gateway;
import desarrollo.web.Bizagi2.entities.Historial;
import desarrollo.web.Bizagi2.entities.Lane;
import desarrollo.web.Bizagi2.entities.Mensaje;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.RolProceso;
import desarrollo.web.Bizagi2.entities.TipoActividad;
import desarrollo.web.Bizagi2.entities.TipoEvento;
import desarrollo.web.Bizagi2.entities.TipoGateway;
import desarrollo.web.Bizagi2.entities.TipoParticipante;
import desarrollo.web.Bizagi2.entities.Usuario;

// Entidades de ejemplo, ya relacionadas entre si, para probar la conversion a DTO en los servicios
final class DatosDto {

    static final Integer EMPRESA_ID = 7;

    private DatosDto() {
    }

    static Empresa empresa() {
        Empresa empresa = new Empresa();
        empresa.setId(EMPRESA_ID);
        empresa.setNombre("Acme SAS");
        empresa.setNit("900123456");
        empresa.setEmailContacto("contacto@acme.com");
        empresa.setEditorModificaEstructura(true);
        return empresa;
    }

    static Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setNombre("Ana Perez");
        usuario.setEmail("ana@acme.com");
        usuario.setPassword("$2a$10$hash");
        usuario.setRolAcceso(RolAcceso.ADMINISTRADOR);
        usuario.setActivo(true);
        usuario.setEmpresa(empresa());
        return usuario;
    }

    static Proceso proceso() {
        Proceso proceso = new Proceso();
        proceso.setId(10);
        proceso.setNombre("Credito");
        proceso.setCategoria("Finanzas");
        proceso.setEstado(EstadoProceso.BORRADOR);
        proceso.setActivo(true);
        proceso.setEmpresa(empresa());
        return proceso;
    }

    static Pool pool(Integer id, TipoParticipante tipo) {
        Pool pool = new Pool();
        pool.setId(id);
        pool.setNombre(tipo == TipoParticipante.EMPRESA_PROPIETARIA ? "Acme" : "Proveedor");
        pool.setTipoParticipante(tipo);
        pool.setProceso(proceso());
        return pool;
    }

    static Pool pool() {
        return pool(20, TipoParticipante.EMPRESA_PROPIETARIA);
    }

    static RolProceso rol() {
        RolProceso rol = new RolProceso();
        rol.setId(30);
        rol.setNombre("Analista");
        rol.setDescripcion("Revisa solicitudes");
        rol.setEmpresa(empresa());
        return rol;
    }

    static Lane lane() {
        Lane lane = new Lane();
        lane.setId(40);
        lane.setNombre("Analistas");
        lane.setOrden(1);
        lane.setPool(pool());
        lane.setRolProceso(rol());
        return lane;
    }

    static Actividad actividad(Integer id, String nombre, TipoActividad tipo) {
        Actividad actividad = new Actividad();
        actividad.setId(id);
        actividad.setNombre(nombre);
        actividad.setTipo(tipo);
        actividad.setPosicionX(100);
        actividad.setPosicionY(50);
        actividad.setActivo(true);
        Lane lane = lane();
        actividad.setLane(lane);
        actividad.setPool(lane.getPool());
        return actividad;
    }

    static Gateway gateway() {
        Gateway gateway = new Gateway();
        gateway.setId(60);
        gateway.setNombre("Aprobado?");
        gateway.setTipoGateway(TipoGateway.EXCLUSIVA);
        gateway.setActivo(true);
        gateway.setPool(pool());
        return gateway;
    }

    static Evento evento(Integer id, String nombre, TipoEvento tipo) {
        Evento evento = new Evento();
        evento.setId(id);
        evento.setNombre(nombre);
        evento.setTipoEvento(tipo);
        evento.setActivo(true);
        evento.setPool(pool());
        return evento;
    }

    static Arco arco() {
        Arco arco = new Arco();
        arco.setId(5);
        arco.setPool(pool());
        arco.setOrigen(actividad(50, "Revisar", TipoActividad.TAREA));
        arco.setDestino(actividad(51, "Aprobar", TipoActividad.USUARIO));
        arco.setEtiqueta("siguiente");
        arco.setActivo(true);
        return arco;
    }

    static Mensaje mensaje() {
        Mensaje mensaje = new Mensaje();
        mensaje.setId(3);
        mensaje.setNombre("PedidoConfirmado");
        mensaje.setProceso(proceso());
        mensaje.setOrigen(evento(70, "PedidoConfirmado", TipoEvento.MENSAJE_LANZAMIENTO));
        mensaje.setDestinoPool(pool(21, TipoParticipante.PROVEEDOR));
        mensaje.setContenido("numero:texto");
        mensaje.setActivo(true);
        return mensaje;
    }

    static Correlacion correlacion() {
        Correlacion correlacion = new Correlacion();
        correlacion.setId(9);
        correlacion.setMensaje(mensaje());
        correlacion.setCriterio("numeroPedido");
        correlacion.setAccionSinCaso(AccionSinCaso.DESCARTAR);
        return correlacion;
    }

    static Historial historial() {
        Historial historial = new Historial();
        historial.setId(80);
        historial.setProceso(proceso());
        historial.setUsuario(usuario());
        historial.setAccion(AccionHistorial.EDITAR);
        historial.setEntidad("PROCESO");
        historial.setEntidadId(10);
        historial.setFecha(Instant.parse("2026-10-28T10:00:00Z"));
        historial.setDetalle("Proceso modificado");
        return historial;
    }
}
