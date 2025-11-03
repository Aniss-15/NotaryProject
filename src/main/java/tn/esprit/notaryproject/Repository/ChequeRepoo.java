package tn.esprit.notaryproject.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.notaryproject.Entities.Cheque;

import java.util.List;

public interface ChequeRepoo extends JpaRepository<Cheque, Long> {


}
