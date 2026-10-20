package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import py.edu.une.politecnica.robogest.dao.CategoriaDAO;
import py.edu.une.politecnica.robogest.dao.MaterialDAO;
import py.edu.une.politecnica.robogest.dto.MaterialRequestDTO;
import py.edu.une.politecnica.robogest.dto.MaterialResponseDTO;
import py.edu.une.politecnica.robogest.entity.Categoria;
import py.edu.une.politecnica.robogest.entity.Material;
import py.edu.une.politecnica.robogest.entity.enums.EstadoMaterialEnum;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialControllerTest {

    @Mock
    private MaterialDAO materialDAO;

    @Mock
    private CategoriaDAO categoriaDAO;

    @Mock
    private py.edu.une.politecnica.robogest.dao.DetallePrestamoDAO detallePrestamoDAO;

    private MaterialController controller;
    private Categoria categoriaMock;

    @BeforeEach
    void setUp() {
        controller = new MaterialController(materialDAO, categoriaDAO, detallePrestamoDAO);
        categoriaMock = new Categoria("Sensores", "Sensores electronicos");
        categoriaMock.setId(10L);
    }

    @Test
    @DisplayName("Debe listar materiales exitosamente")
    void testListarMateriales() {
        Material m = new Material(categoriaMock, "Sensor Ultrasonico", "HC-SR04", "Elec", "V1", 5, EstadoMaterialEnum.ACTIVO, "Gaveta 1");
        m.setId(1L);
        when(materialDAO.findAll()).thenReturn(List.of(m));

        Response response = controller.listarMateriales();

        assertEquals(200, response.getStatus());
        List<?> lista = (List<?>) response.getEntity();
        assertEquals(1, lista.size());
    }

    @Test
    @DisplayName("Debe crear un material exitosamente y retornar HTTP 201")
    void testCrearMaterial_Exitoso() {
        when(categoriaDAO.findById(10L)).thenReturn(Optional.of(categoriaMock));

        MaterialRequestDTO dto = new MaterialRequestDTO();
        dto.setNombre("Arduino UNO R3");
        dto.setCategoriaId(10L);
        dto.setCantidadTotal(8);
        dto.setEstado("ACTIVO");
        dto.setMarca("Arduino");

        Material materialGuardado = new Material(categoriaMock, "Arduino UNO R3", null, "Arduino", null, 8, EstadoMaterialEnum.ACTIVO, null);
        materialGuardado.setId(100L);

        when(materialDAO.save(any(Material.class))).thenReturn(materialGuardado);

        Response response = controller.crearMaterial(dto);

        assertEquals(201, response.getStatus());
        MaterialResponseDTO resDto = (MaterialResponseDTO) response.getEntity();
        assertEquals(100L, resDto.getId());
        assertEquals("Arduino UNO R3", resDto.getNombre());
        assertEquals(10L, resDto.getCategoriaId());
    }

    @Test
    @DisplayName("Debe fallar al crear material si el nombre es nulo o vacio")
    void testCrearMaterial_NombreVacio() {
        MaterialRequestDTO dto = new MaterialRequestDTO();
        dto.setNombre("");
        dto.setCategoriaId(10L);

        assertThrows(IllegalArgumentException.class, () -> controller.crearMaterial(dto));
    }

    @Test
    @DisplayName("Debe actualizar un material existente")
    void testActualizarMaterial() {
        Material existente = new Material(categoriaMock, "Sensor Antiguo", null, null, null, 2, EstadoMaterialEnum.ACTIVO, null);
        existente.setId(5L);
        when(materialDAO.findById(5L)).thenReturn(Optional.of(existente));
        when(materialDAO.update(any(Material.class))).thenReturn(existente);

        MaterialRequestDTO updateDto = new MaterialRequestDTO();
        updateDto.setNombre("Sensor Actualizado");
        updateDto.setCantidadTotal(10);

        Response response = controller.actualizarMaterial(5L, updateDto);

        assertEquals(200, response.getStatus());
        verify(materialDAO).update(existente);
        assertEquals("Sensor Actualizado", existente.getNombre());
        assertEquals(10, existente.getCantidadTotal());
    }

    @Test
    @DisplayName("Debe eliminar un material por ID")
    void testEliminarMaterial() {
        Material m = new Material();
        m.setId(7L);
        when(materialDAO.findById(7L)).thenReturn(Optional.of(m));

        Response response = controller.eliminarMaterial(7L);

        assertEquals(204, response.getStatus());
        verify(materialDAO).delete(m);
    }
}
