package py.edu.une.politecnica.robogest.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import py.edu.une.politecnica.robogest.dao.GenericDAO;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementacion base generica de GenericDAO utilizando JPA (Jakarta Persistence).
 *
 * @param <T>  Tipo de la entidad
 * @param <ID> Tipo del identificador
 */
public abstract class GenericDAOJpaImpl<T, ID> implements GenericDAO<T, ID> {

    protected final Class<T> entityClass;
    protected EntityManager em;

    public GenericDAOJpaImpl(Class<T> entityClass) {
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass no puede ser null");
    }

    public GenericDAOJpaImpl(Class<T> entityClass, EntityManager em) {
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass no puede ser null");
        this.em = em;
    }

    public EntityManager getEntityManager() {
        return em;
    }

    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<T> findById(ID id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(em.find(entityClass, id));
    }

    @Override
    public List<T> findAll() {
        String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e";
        TypedQuery<T> query = em.createQuery(jpql, entityClass);
        return query.getResultList();
    }

    @Override
    public T save(T entity) {
        Objects.requireNonNull(entity, "La entidad a persistir no puede ser null");
        em.persist(entity);
        return entity;
    }

    @Override
    public T update(T entity) {
        Objects.requireNonNull(entity, "La entidad a actualizar no puede ser null");
        return em.merge(entity);
    }

    @Override
    public void delete(T entity) {
        Objects.requireNonNull(entity, "La entidad a eliminar no puede ser null");
        if (em.contains(entity)) {
            em.remove(entity);
        } else {
            em.remove(em.merge(entity));
        }
    }

    @Override
    public void deleteById(ID id) {
        findById(id).ifPresent(this::delete);
    }

    @Override
    public boolean existsById(ID id) {
        if (id == null) {
            return false;
        }
        return findById(id).isPresent();
    }

    @Override
    public long count() {
        String jpql = "SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e";
        TypedQuery<Long> query = em.createQuery(jpql, Long.class);
        return query.getSingleResult();
    }
}
