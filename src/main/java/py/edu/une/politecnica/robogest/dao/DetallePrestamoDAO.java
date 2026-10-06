package py.edu.une.politecnica.robogest.dao;

import py.edu.une.politecnica.robogest.entity.DetallePrestamo;
import py.edu.une.politecnica.robogest.entity.DetallePrestamoId;

import java.util.List;

/**
 * Interfaz DAO para operaciones de persistencia de DetallePrestamo.
 */
public interface DetallePrestamoDAO extends GenericDAO<DetallePrestamo, DetallePrestamoId> {

    List<DetallePrestamo> findByPrestamoId(Long prestamoId);

    List<DetallePrestamo> findByMaterialId(Long materialId);

    /**
     * Calcula la cantidad total reservada/ocupada de un material en prestamos activos
     * (estados APROBADO, ENTREGADO o VENCIDO).
     */
    int getCantidadReservadaPorMaterial(Long materialId);
}
