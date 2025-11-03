package tn.esprit.notaryproject.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.notaryproject.Entities.Cheque;
import tn.esprit.notaryproject.Entities.ChequeValidationStatus;
import tn.esprit.notaryproject.Entities.ChequesStatus;
import tn.esprit.notaryproject.Services.ChequeInterface;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cheques")
@CrossOrigin(origins = "*")
public class ChequeController {

    @Autowired
    private ChequeInterface chequeInterface;

    @Operation(summary = "Add cheque with validation", description = "Add a cheque with all details and automatic validation")
    @PostMapping(
            value = "/ajoutavecVerification",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> ajoutAvecVerification(
            @Parameter(description = "Cheque Number", required = true)
            @RequestParam("ChequeNumber") Long ChequeNumber,

            @Parameter(description = "Bank name", required = true)
            @RequestParam("bankeName") String bankeName,

            @Parameter(description = "Amount", required = true)
            @RequestParam("montant") Float montant,

            @Parameter(description = "Expected deposit date in YYYY-MM-DD format", required = true)
            @RequestParam("depositDateCheque") @DateTimeFormat(pattern = "yyyy-MM-dd") Date depositDateCheque,

            @Parameter(description = "Image file containing the cheque (JPEG, PNG, TIFF, BMP)", required = true)
            @RequestParam("imageFile") MultipartFile imageFile) {

        try {
            Map<String, Object> response = new HashMap<>();

            // Create new cheque entity with all fields
            Cheque cheque = new Cheque();

            // Set fields from request parameters
            cheque.setChequeNumber(ChequeNumber);
            cheque.setBankeName(bankeName);
            cheque.setMontant(montant);
            cheque.setDepositDateCheque(depositDateCheque);

            // Set default status - la validation se fera dans le service
            cheque.setStatus(ChequesStatus.Recieved);
            cheque.setValidationStatus(ChequeValidationStatus.PENDING);

            // Save cheque to database - le service gérera l'image et la validation
            Cheque savedCheque = chequeInterface.chequeAjout(cheque, imageFile);

            // Réponse basée sur le statut de validation final
            response.put("success", true);
            response.put("message", "Cheque processed successfully");
            response.put("validationStatus", savedCheque.getValidationStatus().toString());
            response.put("cheque", mapChequeToResponse(savedCheque));
            response.put("chequeNumber", ChequeNumber);
            response.put("bankeName", bankeName);
            response.put("montant", montant);
            response.put("depositDateCheque", depositDateCheque);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "❌ Error processing cheque: " + e.getMessage());
            errorResponse.put("chequeNumber", ChequeNumber);
            errorResponse.put("bankeName", bankeName);
            errorResponse.put("montant", montant);
            errorResponse.put("depositDateCheque", depositDateCheque);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @Operation(summary = "Check cheque validity", description = "Validate a cheque using OCR and AI without saving to database")
    @PostMapping(
            value = "/check",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> checkChequeValidity(
            @RequestPart("file") MultipartFile file) throws Exception {

        ChequeValidationStatus status = chequeInterface.checkChequeValidity(file);

        Map<String, Object> response = new HashMap<>();
        response.put("validationStatus", status);
        response.put("message", status == ChequeValidationStatus.VALID
                ? "✅ The cheque is valid."
                : "❌ The cheque is invalid.");

        return ResponseEntity.ok(response);
    }

    // Helper methods
    private Map<String, Object> mapChequeToResponse(Cheque cheque) {
        Map<String, Object> chequeResponse = new HashMap<>();
        chequeResponse.put("idCheque", cheque.getIdCheque());
        chequeResponse.put("chequeNumber", cheque.getChequeNumber());
        chequeResponse.put("bankeName", cheque.getBankeName());
        chequeResponse.put("montant", cheque.getMontant());
        chequeResponse.put("depositDateCheque", cheque.getDepositDateCheque());
        chequeResponse.put("status", cheque.getStatus());
        chequeResponse.put("validationStatus", cheque.getValidationStatus());
        return chequeResponse;
    }

    @Operation(summary = "Delete cheque by ID", description = "Permanently delete a cheque from the database")
    @DeleteMapping("/delete/{idCheque}")
    public ResponseEntity<Map<String, Object>> deleteCheque(
            @Parameter(description = "ID of the cheque to delete", required = true)
            @PathVariable Long idCheque) {

        try {
            Map<String, Object> response = new HashMap<>();

            // Appeler le service pour supprimer le chèque
            chequeInterface.DeleteCheque(idCheque);

            response.put("success", true);
            response.put("message", "✅ Cheque deleted successfully");
            response.put("deletedChequeId", idCheque);
            response.put("timestamp", new Date());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // Gérer le cas où le chèque n'est pas trouvé
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "❌ " + e.getMessage());
            errorResponse.put("chequeId", idCheque);
            errorResponse.put("timestamp", new Date());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

        } catch (Exception e) {
            // Gérer les autres erreurs
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "❌ Error deleting cheque: " + e.getMessage());
            errorResponse.put("chequeId", idCheque);
            errorResponse.put("timestamp", new Date());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}