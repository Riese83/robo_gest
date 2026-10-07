package py.edu.une.politecnica.robogest.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import py.edu.une.politecnica.robogest.dao.GenericDAO;
import py.edu.une.politecnica.robogest.util.JpaUtil;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementacion base generica de GenericDAO utilizando JPA (Jakarta Persistence).
 * Utiliza getEntityManager() dinamicamente para garantizar compatibilidad con ThreadLocal.
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
        if (this.em != null) {
            return this.em;
        }
        return JpaUtil.getEntityManager();
    }

    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<T> findById(ID id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(getEntityManager().find(entityClass, id));
    }

    @Override
    public List<T> findAll() {
        String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e";
        TypedQuery<T> query = getEntityManager().createQuery(jpql, entityClass);
        return query.getResultList();
    }

    @Override
    public T save(T entity) {
        Objects.requireNonNull(entity, "La entidad a persistir no puede ser null");
        EntityManager currentEm = getEntityManager();
        boolean activeTransaction = currentEm.getTransaction().isActive();
        if (!activeTransaction) {
            currentEm.getTransaction().begin();
        }
        try {
            currentEm.persist(entity);
            if (!activeTransaction) {
                currentEm.getTransaction().commit();
            }
            return entity;
        } catch (RuntimeException e) {
            if (!activeTransaction && currentEm.getTransaction().isActive()) {
                currentEm.getTransaction().rollback();
            }
            throw e;
        }
    }

    @Override
    public T update(T entity) {
        Objects.requireNonNull(entity, "La entidad a actualizar no puede ser null");
        EntityManager currentEm = getEntityManager();
        boolean activeTransaction = currentEm.getTransaction().isActive();
        if (!activeTransaction) {
            currentEm.getTransaction().begin();
        }
        try {
            T merged = currentEm.merge(entity);
            if (!activeTransaction) {
                currentEm.getTransaction().commit();
            }
            return merged;
        } catch (RuntimeException e) {
            if (!activeTransaction && currentEm.getTransaction().isActive()) {
                currentEm.getTransaction().rollback();
            }
            throw e;
        }
    }

    @Override
    public void delete(T entity) {
        Objects.requireNonNull(entity, "La entidad a eliminar no puede ser null");
        EntityManager currentEm = getEntityManager();
        boolean activeTransaction = currentEm.getTransaction().isActive();
        if (!activeTransaction) {
            currentEm.getTransaction().begin();
        }
        try {
            if (currentEm.contains(entity)) {
                currentEm.remove(entity);
            } else {
                currentEm.remove(currentEm.merge(entity));
            }
            if (!activeTransaction) {
                currentEm.getTransaction().commit();
            }
        } catch (RuntimeException e) {
            if (!activeTransaction && currentEm.getTransaction().isActive()) {
                currentEm.getTransaction().rollback();
            }
            throw e;
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
        TypedQuery<Long> query = getEntityManager().createQuery(jpql, Long.class);
        return query.getSingleResult();
    }
}
