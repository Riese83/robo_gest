package py.edu.une.politecnica.robogest.dao;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz generica que define las operaciones atomicas CRUD de persistencia.
 *
 * @param <T>  Tipo de la entidad de persistencia
 * @param <ID> Tipo del identificador unico de la entidad
 */
public interface GenericDAO<T, ID> {

    /**
     * Busca una entidad por su identificador unico.
     */
    Optional<T> findById(ID id);

    /**
     * Retorna todas las entidades del tipo T.
     */
    List<T> findAll();

    /**
     * Persiste una nueva entidad en la base de datos.
     */
    T save(T entity);

    /**
     * Actualiza el estado de una entidad existente (merge).
     */
    T update(T entity);

    /**
     * Elimina una entidad dada.
     */
    void delete(T entity);

    /**
     * Elimina una entidad por su identificador unico.
     */
    void deleteById(ID id);

    /**
     * Verifica la existencia de una entidad por su ID.
     */
    boolean existsById(ID id);

    /**
     * Cuenta la cantidad total de registros de la entidad.
     */
    long count();
}
