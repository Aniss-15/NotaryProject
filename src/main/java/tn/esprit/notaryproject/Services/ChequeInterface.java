package tn.esprit.notaryproject.Services;

import org.springframework.web.multipart.MultipartFile;
import tn.esprit.notaryproject.Entities.Cheque;
import tn.esprit.notaryproject.Entities.ChequeValidationStatus;


import java.io.IOException;
import java.util.List;

public interface ChequeInterface {
    // OCR text extraction methode
     ChequeValidationStatus checkChequeValidity(MultipartFile file) throws Exception ;
     Cheque chequeAjout(Cheque cheque, MultipartFile file) throws Exception ;
     void DeleteCheque(Long IdCheque) ;



    }
