package py.edu.une.politecnica.robogest.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import py.edu.une.politecnica.robogest.dao.MaterialDAO;
import py.edu.une.politecnica.robogest.entity.Material;
import py.edu.une.politecnica.robogest.entity.enums.EstadoMaterialEnum;

import java.util.List;
import java.util.Optional;

/**
 * Implementacion JPA de MaterialDAO con soporte para bloqueo pesimista y consultas parametrizadas.
 */
public class MaterialDAOJpaImpl extends GenericDAOJpaImpl<Material, Long> implements MaterialDAO {

    public MaterialDAOJpaImpl() {
        super(Material.class);
    }

    public MaterialDAOJpaImpl(EntityManager em) {
        super(Material.class, em);
    }

    @Override
    public Optional<Material> findByIdWithLock(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(getEntityManager().find(Material.class, id, LockModeType.PESSIMISTIC_WRITE));
    }

    @Override
    public List<Material> findByCategoriaId(Long categoriaId) {
        String jpql = "SELECT m FROM Material m WHERE m.categoria.id = :categoriaId ORDER BY m.nombre ASC";
        TypedQuery<Material> query = getEntityManager().createQuery(jpql, Material.class);
        query.setParameter("categoriaId", categoriaId);
        return query.getResultList();
    }

    @Override
    public List<Material> findByEstado(EstadoMaterialEnum estado) {
        String jpql = "SELECT m FROM Material m WHERE m.estado = :estado ORDER BY m.nombre ASC";
        TypedQuery<Material> query = getEntityManager().createQuery(jpql, Material.class);
        query.setParameter("estado", estado);
        return query.getResultList();
    }

    @Override
    public List<Material> findByNombreContaining(String nombre) {
        String jpql = "SELECT m FROM Material m WHERE LOWER(m.nombre) LIKE LOWER(:nombre) ORDER BY m.nombre ASC";
        TypedQuery<Material> query = getEntityManager().createQuery(jpql, Material.class);
        query.setParameter("nombre", "%" + nombre + "%");
        return query.getResultList();
    }
}
