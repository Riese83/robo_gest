package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import py.edu.une.politecnica.robogest.dao.ProyectoDAO;
import py.edu.une.politecnica.robogest.dto.ProyectoRequestDTO;
import py.edu.une.politecnica.robogest.dto.ProyectoResponseDTO;
import py.edu.une.politecnica.robogest.entity.Proyecto;
import py.edu.une.politecnica.robogest.entity.enums.EstadoProyectoEnum;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProyectoControllerTest {

    @Mock
    private ProyectoDAO proyectoDAO;

    @Mock
    private py.edu.une.politecnica.robogest.dao.IntegranteProyectoDAO integranteProyectoDAO;

    @Mock
    private py.edu.une.politecnica.robogest.dao.ProyectoMaterialDAO proyectoMaterialDAO;

    @Mock
    private py.edu.une.politecnica.robogest.dao.IntegranteDAO integranteDAO;

    @Mock
    private py.edu.une.politecnica.robogest.dao.MaterialDAO materialDAO;

    @Mock
    private py.edu.une.politecnica.robogest.dao.DetallePrestamoDAO detallePrestamoDAO;

    private ProyectoController proyectoController;

    @BeforeEach
    void setUp() {
        proyectoController = new ProyectoController(
                proyectoDAO,
                integranteProyectoDAO,
                proyectoMaterialDAO,
                integranteDAO,
                materialDAO,
                detallePrestamoDAO
        );
    }

    @Test
    @DisplayName("Debe listar todos los proyectos retornando HTTP 200")
    void testListarProyectos() {
        Proyecto p1 = new Proyecto("Robot Sumo", "Robot para competencia", LocalDate.now(), null, EstadoProyectoEnum.ACTIVO);
        p1.setId(1L);
        when(proyectoDAO.findAll()).thenReturn(List.of(p1));

        Response resp = proyectoController.listarProyectos();
        assertEquals(200, resp.getStatus());
        assertNotNull(resp.getEntity());
        assertInstanceOf(List.class, resp.getEntity());
    }

    @Test
    @DisplayName("Debe crear un proyecto exitosamente retornando HTTP 201")
    void testCrearProyecto() {
        ProyectoRequestDTO dto = new ProyectoRequestDTO();
        dto.setNombre("Dron Autonomo");
        dto.setDescripcion("Dron cuadricoptero");
        dto.setFechaInicio(LocalDate.now());
        dto.setEstado("ACTIVO");

        Proyecto guardado = new Proyecto("Dron Autonomo", "Dron cuadricoptero", LocalDate.now(), null, EstadoProyectoEnum.ACTIVO);
        guardado.setId(10L);

        when(proyectoDAO.save(any(Proyecto.class))).thenReturn(guardado);

        Response resp = proyectoController.crearProyecto(dto);
        assertEquals(201, resp.getStatus());
        assertNotNull(resp.getEntity());
        assertInstanceOf(ProyectoResponseDTO.class, resp.getEntity());
        assertEquals(10L, ((ProyectoResponseDTO) resp.getEntity()).getId());
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException al crear proyecto sin nombre")
    void testCrearProyectoSinNombre() {
        ProyectoRequestDTO dto = new ProyectoRequestDTO();
        dto.setNombre("");
        assertThrows(IllegalArgumentException.class, () -> proyectoController.crearProyecto(dto));
    }
}
