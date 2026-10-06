package py.edu.une.politecnica.robogest.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import py.edu.une.politecnica.robogest.dao.DetallePrestamoDAO;
import py.edu.une.politecnica.robogest.entity.DetallePrestamo;
import py.edu.une.politecnica.robogest.entity.DetallePrestamoId;

import java.util.List;

/**
 * Implementacion JPA de DetallePrestamoDAO con consultas JPQL tipadas.
 */
public class DetallePrestamoDAOJpaImpl extends GenericDAOJpaImpl<DetallePrestamo, DetallePrestamoId> implements DetallePrestamoDAO {

    public DetallePrestamoDAOJpaImpl() {
        super(DetallePrestamo.class);
    }

    public DetallePrestamoDAOJpaImpl(EntityManager em) {
        super(DetallePrestamo.class, em);
    }

    @Override
    public List<DetallePrestamo> findByPrestamoId(Long prestamoId) {
        String jpql = "SELECT dp FROM DetallePrestamo dp WHERE dp.prestamo.id = :prestamoId";
        TypedQuery<DetallePrestamo> query = em.createQuery(jpql, DetallePrestamo.class);
        query.setParameter("prestamoId", prestamoId);
        return query.getResultList();
    }

    @Override
    public List<DetallePrestamo> findByMaterialId(Long materialId) {
        String jpql = "SELECT dp FROM DetallePrestamo dp WHERE dp.material.id = :materialId";
        TypedQuery<DetallePrestamo> query = em.createQuery(jpql, DetallePrestamo.class);
        query.setParameter("materialId", materialId);
        return query.getResultList();
    }
}
