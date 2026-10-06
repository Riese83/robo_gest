package py.edu.une.politecnica.robogest.dao;

import py.edu.une.politecnica.robogest.entity.Integrante;
import py.edu.une.politecnica.robogest.entity.enums.CarreraEnum;
import py.edu.une.politecnica.robogest.entity.enums.EstadoIntegranteEnum;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz DAO para operaciones de persistencia de la entidad Integrante.
 */
public interface IntegranteDAO extends GenericDAO<Integrante, Long> {

    Optional<Integrante> findByCi(String ci);

    Optional<Integrante> findByEmail(String email);

    Optional<Integrante> findByCarnetUniversitario(String carnetUniversitario);

    Optional<Integrante> findByNfcUid(String nfcUid);

    List<Integrante> findByCarrera(CarreraEnum carrera);

    List<Integrante> findByEstado(EstadoIntegranteEnum estado);
}
