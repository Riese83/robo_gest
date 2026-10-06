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

    @Override
    public int getCantidadReservadaPorMaterial(Long materialId) {
        String jpql = "SELECT COALESCE(SUM(dp.cantidad), 0) FROM DetallePrestamo dp " +
                      "WHERE dp.material.id = :materialId " +
                      "  AND dp.prestamo.estado IN :estados";
        TypedQuery<Number> query = em.createQuery(jpql, Number.class);
        query.setParameter("materialId", materialId);
        query.setParameter("estados", List.of(
                py.edu.une.politecnica.robogest.entity.enums.EstadoPrestamoEnum.APROBADO,
                py.edu.une.politecnica.robogest.entity.enums.EstadoPrestamoEnum.ENTREGADO,
                py.edu.une.politecnica.robogest.entity.enums.EstadoPrestamoEnum.VENCIDO
        ));
        Number result = query.getSingleResult();
        return result != null ? result.intValue() : 0;
    }
}
