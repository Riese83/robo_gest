package py.edu.une.politecnica.robogest.dao;

import py.edu.une.politecnica.robogest.entity.IntegranteProyecto;
import py.edu.une.politecnica.robogest.entity.IntegranteProyectoId;

import java.util.List;

/**
 * Interfaz DAO para operaciones de persistencia de IntegranteProyecto.
 */
public interface IntegranteProyectoDAO extends GenericDAO<IntegranteProyecto, IntegranteProyectoId> {

    List<IntegranteProyecto> findByProyectoId(Long proyectoId);

    List<IntegranteProyecto> findByIntegranteId(Long integranteId);
}
