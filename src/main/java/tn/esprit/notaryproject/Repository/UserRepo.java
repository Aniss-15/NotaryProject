package tn.esprit.notaryproject.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;
import tn.esprit.notaryproject.Entities.User;

import java.util.Optional;

public interface UserRepo extends JpaRepository<User,Long> {
    @Query("SELECT u FROM User u WHERE u.emailUser = :email") // <-- correspond exactement au nom du champ
    Optional<User> findByEmailUser(@Param("email") String email);

    User findUsersByIdUser(Long id);

}
