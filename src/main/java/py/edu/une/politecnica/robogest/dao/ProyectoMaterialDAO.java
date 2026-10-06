package py.edu.une.politecnica.robogest.dao;

import py.edu.une.politecnica.robogest.entity.ProyectoMaterial;
import py.edu.une.politecnica.robogest.entity.ProyectoMaterialId;

import java.util.List;

/**
 * Interfaz DAO para operaciones de persistencia de ProyectoMaterial.
 */
public interface ProyectoMaterialDAO extends GenericDAO<ProyectoMaterial, ProyectoMaterialId> {

    List<ProyectoMaterial> findByProyectoId(Long proyectoId);

    List<ProyectoMaterial> findByMaterialId(Long materialId);
}
