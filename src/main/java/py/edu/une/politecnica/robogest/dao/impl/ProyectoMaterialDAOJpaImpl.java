package py.edu.une.politecnica.robogest.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import py.edu.une.politecnica.robogest.dao.ProyectoMaterialDAO;
import py.edu.une.politecnica.robogest.entity.ProyectoMaterial;
import py.edu.une.politecnica.robogest.entity.ProyectoMaterialId;

import java.util.List;

/**
 * Implementacion JPA de ProyectoMaterialDAO con consultas JPQL tipadas.
 */
public class ProyectoMaterialDAOJpaImpl extends GenericDAOJpaImpl<ProyectoMaterial, ProyectoMaterialId> implements ProyectoMaterialDAO {

    public ProyectoMaterialDAOJpaImpl() {
        super(ProyectoMaterial.class);
    }

    public ProyectoMaterialDAOJpaImpl(EntityManager em) {
        super(ProyectoMaterial.class, em);
    }

    @Override
    public List<ProyectoMaterial> findByProyectoId(Long proyectoId) {
        String jpql = "SELECT pm FROM ProyectoMaterial pm WHERE pm.proyecto.id = :proyectoId";
        TypedQuery<ProyectoMaterial> query = em.createQuery(jpql, ProyectoMaterial.class);
        query.setParameter("proyectoId", proyectoId);
        return query.getResultList();
    }

    @Override
    public List<ProyectoMaterial> findByMaterialId(Long materialId) {
        String jpql = "SELECT pm FROM ProyectoMaterial pm WHERE pm.material.id = :materialId";
        TypedQuery<ProyectoMaterial> query = em.createQuery(jpql, ProyectoMaterial.class);
        query.setParameter("materialId", materialId);
        return query.getResultList();
    }
}
