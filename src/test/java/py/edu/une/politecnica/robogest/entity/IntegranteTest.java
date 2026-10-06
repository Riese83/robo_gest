package py.edu.une.politecnica.robogest.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import py.edu.une.politecnica.robogest.entity.enums.CarreraEnum;
import py.edu.une.politecnica.robogest.entity.enums.EstadoIntegranteEnum;
import py.edu.une.politecnica.robogest.entity.enums.RolEnum;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class IntegranteTest {

    @Test
    @DisplayName("Debe instanciar Integrante con CarreraEnum segun regla de negocio")
    void testIntegranteCarreraEnum() {
        Integrante integrante = new Integrante(
                "Lucas",
                "Benitez",
                "4567890",
                "FP-2023-01",
                "0981123456",
                "lucas.benitez@fpune.edu.py",
                LocalDate.now(),
                EstadoIntegranteEnum.ACTIVO,
                "A1B2C3D4",
                CarreraEnum.INGENIERIA_DE_SISTEMAS,
                RolEnum.MIEMBRO,
                "hash_bcrypt"
        );

        assertEquals(CarreraEnum.INGENIERIA_DE_SISTEMAS, integrante.getCarrera());
        assertEquals("Lucas", integrante.getNombre());
        assertEquals(EstadoIntegranteEnum.ACTIVO, integrante.getEstado());

        // Verificar todos los valores requeridos en CarreraEnum
        assertNotNull(CarreraEnum.valueOf("INGENIERIA_DE_SISTEMAS"));
        assertNotNull(CarreraEnum.valueOf("INGENIERIA_ELECTRICA"));
        assertNotNull(CarreraEnum.valueOf("ANALISIS_DE_SISTEMAS"));
        assertNotNull(CarreraEnum.valueOf("LICENCIATURA_EN_TURISMO"));
        assertNotNull(CarreraEnum.valueOf("COLABORADOR"));
    }
}
