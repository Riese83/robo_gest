package py.edu.une.politecnica.robogest.dao;

import py.edu.une.politecnica.robogest.entity.Proyecto;
import py.edu.une.politecnica.robogest.entity.enums.EstadoProyectoEnum;

import java.util.List;

/**
 * Interfaz DAO para operaciones de persistencia de Proyecto.
 */
public interface ProyectoDAO extends GenericDAO<Proyecto, Long> {

    List<Proyecto> findByEstado(EstadoProyectoEnum estado);

    List<Proyecto> findByNombreContaining(String nombre);
}
