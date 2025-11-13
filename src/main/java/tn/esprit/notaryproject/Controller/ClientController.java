package tn.esprit.notaryproject.Controller;

import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.notaryproject.Entities.Client;
import tn.esprit.notaryproject.Entities.User;
import tn.esprit.notaryproject.Services.ClientInterface;

import java.util.List;

@RestController
@NoArgsConstructor
@RequestMapping("/Clientss")
public class ClientController {
    @Autowired
    ClientInterface clientInterface;

    @PostMapping(path = "/Addclients")
    public ResponseEntity<?> addClient(@RequestBody Client client) {
        try {
            Client savedClient = clientInterface.addClient(client);
            return new ResponseEntity<>(savedClient, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("An error occurred while adding the client", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/updateC{Idclient}")
    public ResponseEntity<?> updateClient(@PathVariable Long Idclient , @RequestBody Client clientDetails) {
        Client updatedClient = clientInterface.updateClient(Idclient,clientDetails);
        return ResponseEntity.ok("Client updated successfully");
    }
    @DeleteMapping("/{Idclient}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long Idclient) {
        clientInterface.deleteClient(Idclient);
        return ResponseEntity.noContent().build();
    }
    @GetMapping(path="/search/national-id/{nationalID}")
    public ResponseEntity<?> searchClientByNationalID(@PathVariable String nationalID) {
        try {
            Client client = clientInterface.SearchClientByNationalID(nationalID);
            return ResponseEntity.ok(client);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("An error occurred while searching for the client");
        }
    }
    @GetMapping("/search/prenom/{prenomClient}")
    public ResponseEntity<?> searchClientByPrenomClient(@PathVariable String prenomClient) {
        try {
            Client client = clientInterface.SearchClientByName(prenomClient);
            return ResponseEntity.ok(client);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("An error occurred while searching for the client: " + e.getMessage());
        }
    }

    @GetMapping("/Allclients")
    public ResponseEntity<List<Client>> AllClients() {
        List<Client> clients = clientInterface.DisplayAllClients();
        return ResponseEntity.ok(clients);
    }

    @PostMapping("/assign-article/{clientId}/{articleId}")
    public ResponseEntity<Client> assignArticleToClient(
            @PathVariable Long clientId,
            @PathVariable Long articleId) {
        Client updatedClient = clientInterface.AssingArticleToClient(clientId, articleId);
        return ResponseEntity.ok(updatedClient);
    }

    @DeleteMapping("/Dessafectation/{clientId}/{articleId}")
    public ResponseEntity<Client> DessafectClientFromArticle(
            @PathVariable Long clientId,
            @PathVariable Long articleId) {
        Client updatedClient1 = clientInterface.DessafecterArticleToClient(clientId, articleId);
        return ResponseEntity.ok(updatedClient1);
    }
    @PostMapping("/assign-cheque-client/{clientId}/{chequeId}")
    public ResponseEntity<Client> assignchequeToClient(
            @PathVariable Long clientId,
            @PathVariable Long chequeId) {
        Client updatedClient = clientInterface.AssingChequeTocClient(clientId, chequeId);
        return ResponseEntity.ok(updatedClient);
    }
}