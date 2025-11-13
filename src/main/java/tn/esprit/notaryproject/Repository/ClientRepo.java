package tn.esprit.notaryproject.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;
import tn.esprit.notaryproject.Entities.Client;

public interface ClientRepo extends JpaRepository<Client, Long> {
    @Query("SELECT COUNT(c) > 0 FROM Client c WHERE c.NationalID = :nationalID")
    boolean existsByNationalID(@Param("nationalID") String NationalID);

    @Query("SELECT COUNT(c) > 0 FROM Client c WHERE c.NationalID = :nationalID AND c.Idclient <> :excludeId")
    boolean existsByNationalIDAndIdNot(@Param("nationalID") String nationalID,
                                       @Param("excludeId") Long excludeId);
    @Query("SELECT c FROM Client c WHERE c.NationalID = :NationalID")
    Client findByNationalID(@Param("NationalID") String NationalID);

    @Query("SELECT c FROM Client c WHERE c.PrenomClient = :PrenomClient")
    Client findByPrenomClient(@Param("PrenomClient") String PrenomClient);

}
