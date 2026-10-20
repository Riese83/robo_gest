package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import py.edu.une.politecnica.robogest.dao.CategoriaDAO;
import py.edu.une.politecnica.robogest.dto.CategoriaRequestDTO;
import py.edu.une.politecnica.robogest.dto.CategoriaResponseDTO;
import py.edu.une.politecnica.robogest.entity.Categoria;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaControllerTest {

    @Mock
    private CategoriaDAO categoriaDAO;

    private CategoriaController controller;

    @BeforeEach
    void setUp() {
        controller = new CategoriaController(categoriaDAO);
    }

    @Test
    @DisplayName("Debe listar categorias exitosamente")
    void testListarCategorias() {
        Categoria c = new Categoria("Motores", "Motores paso a paso y servos");
        c.setId(1L);
        when(categoriaDAO.findAll()).thenReturn(List.of(c));

        Response response = controller.listarCategorias();

        assertEquals(200, response.getStatus());
        List<?> lista = (List<?>) response.getEntity();
        assertEquals(1, lista.size());
    }

    @Test
    @DisplayName("Debe crear una categoria y retornar HTTP 201")
    void testCrearCategoria_Exitoso() {
        when(categoriaDAO.findByNombre("Actuadores")).thenReturn(Optional.empty());

        Categoria guardada = new Categoria("Actuadores", "Pistones y servos");
        guardada.setId(10L);
        when(categoriaDAO.save(any(Categoria.class))).thenReturn(guardada);

        CategoriaRequestDTO dto = new CategoriaRequestDTO();
        dto.setNombre("Actuadores");
        dto.setDescripcion("Pistones y servos");

        Response response = controller.crearCategoria(dto);

        assertEquals(201, response.getStatus());
        CategoriaResponseDTO resDto = (CategoriaResponseDTO) response.getEntity();
        assertEquals(10L, resDto.getId());
        assertEquals("Actuadores", resDto.getNombre());
    }

    @Test
    @DisplayName("Debe fallar al crear categoria si el nombre ya existe")
    void testCrearCategoria_NombreDuplicado() {
        when(categoriaDAO.findByNombre("Sensores")).thenReturn(Optional.of(new Categoria("Sensores", null)));

        CategoriaRequestDTO dto = new CategoriaRequestDTO();
        dto.setNombre("Sensores");

        assertThrows(IllegalArgumentException.class, () -> controller.crearCategoria(dto));
    }

    @Test
    @DisplayName("Debe eliminar categoria por ID")
    void testEliminarCategoria() {
        Categoria c = new Categoria("Herramientas", null);
        c.setId(3L);
        when(categoriaDAO.findById(3L)).thenReturn(Optional.of(c));

        Response response = controller.eliminarCategoria(3L);

        assertEquals(204, response.getStatus());
        verify(categoriaDAO).delete(c);
    }
}
