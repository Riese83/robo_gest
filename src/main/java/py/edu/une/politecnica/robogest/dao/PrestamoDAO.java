package py.edu.une.politecnica.robogest.dao;

import py.edu.une.politecnica.robogest.entity.Prestamo;
import py.edu.une.politecnica.robogest.entity.enums.EstadoPrestamoEnum;

import java.util.List;

/**
 * Interfaz DAO para operaciones de persistencia de Prestamo.
 */
public interface PrestamoDAO extends GenericDAO<Prestamo, Long> {

    List<Prestamo> findByIntegranteId(Long integranteId);

    List<Prestamo> findByProyectoId(Long proyectoId);

    List<Prestamo> findByEstado(EstadoPrestamoEnum estado);

    /**
     * Consulta los prestamos actualmente vencidos (estado ENTREGADO, sin devolver y con fecha prevista expirada).
     */
    List<Prestamo> findVencidos();
}
