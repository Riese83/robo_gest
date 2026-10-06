package py.edu.une.politecnica.robogest.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import py.edu.une.politecnica.robogest.dao.impl.MaterialDAOJpaImpl;
import py.edu.une.politecnica.robogest.entity.Categoria;
import py.edu.une.politecnica.robogest.entity.Material;
import py.edu.une.politecnica.robogest.entity.enums.EstadoMaterialEnum;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialDAOJpaImplTest {

    @Mock
    private EntityManager entityManager;

    private MaterialDAOJpaImpl materialDAO;

    @BeforeEach
    void setUp() {
        materialDAO = new MaterialDAOJpaImpl(entityManager);
    }

    @Test
    @DisplayName("Debe consultar material aplicando bloqueo pesimista PESSIMISTIC_WRITE")
    void testFindByIdWithLock_Success() {
        Long materialId = 1L;
        Material mockMaterial = new Material(
                new Categoria("Microcontroladores", "Placas y MCUs"),
                "ESP32 NodeMCU",
                "Modulo WiFi + Bluetooth",
                "Espressif",
                "WROOM-32",
                15,
                EstadoMaterialEnum.ACTIVO,
                "Estante A-1"
        );
        mockMaterial.setId(materialId);

        when(entityManager.find(Material.class, materialId, LockModeType.PESSIMISTIC_WRITE))
                .thenReturn(mockMaterial);

        Optional<Material> result = materialDAO.findByIdWithLock(materialId);

        assertTrue(result.isPresent());
        assertEquals("ESP32 NodeMCU", result.get().getNombre());
        assertEquals(15, result.get().getCantidadTotal());

        // Verificacion critica: debe invocarse entityManager.find con PESSIMISTIC_WRITE
        verify(entityManager, times(1)).find(Material.class, materialId, LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    @DisplayName("Debe retornar Optional.empty cuando no existe el material con bloqueo pesimista")
    void testFindByIdWithLock_NotFound() {
        Long materialId = 999L;
        when(entityManager.find(Material.class, materialId, LockModeType.PESSIMISTIC_WRITE))
                .thenReturn(null);

        Optional<Material> result = materialDAO.findByIdWithLock(materialId);

        assertFalse(result.isPresent());
        verify(entityManager, times(1)).find(Material.class, materialId, LockModeType.PESSIMISTIC_WRITE);
    }
}
