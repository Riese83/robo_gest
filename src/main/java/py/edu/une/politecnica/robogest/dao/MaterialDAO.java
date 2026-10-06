package py.edu.une.politecnica.robogest.dao;

import py.edu.une.politecnica.robogest.entity.Material;
import py.edu.une.politecnica.robogest.entity.enums.EstadoMaterialEnum;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz DAO para operaciones de persistencia e inventario de Material.
 */
public interface MaterialDAO extends GenericDAO<Material, Long> {

    /**
     * Busca un material por su ID aplicando un bloqueo pesimista de escritura (PESSIMISTIC_WRITE).
     * Garantiza la integridad transaccional al consultar y actualizar stock.
     */
    Optional<Material> findByIdWithLock(Long id);

    List<Material> findByCategoriaId(Long categoriaId);

    List<Material> findByEstado(EstadoMaterialEnum estado);

    List<Material> findByNombreContaining(String nombre);
}
