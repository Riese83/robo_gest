package py.edu.une.politecnica.robogest.dao;

import py.edu.une.politecnica.robogest.entity.Categoria;

import java.util.Optional;

/**
 * Interfaz DAO para operaciones de persistencia de Categoria.
 */
public interface CategoriaDAO extends GenericDAO<Categoria, Long> {

    Optional<Categoria> findByNombre(String nombre);
}
