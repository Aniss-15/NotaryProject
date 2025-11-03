package tn.esprit.notaryproject.Controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.notaryproject.Entities.Client;
import tn.esprit.notaryproject.Entities.User;
import tn.esprit.notaryproject.Services.UserInterface;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {
    @Autowired
    UserInterface userInterface;
    @PostMapping(
            path = "/ajoutUtilisateur",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> addUser(@RequestBody User user) {
        try {
            // 🧩 Debug: Log what Swagger actually sends
            // ✅ Save user through your service
            User savedUser = userInterface.registerUser(user);
            System.out.println("User saved successfully: " + savedUser);

            // ✅ Return success response
            return ResponseEntity.ok(savedUser);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Error creating user: " + e.getMessage());
        }
    }

    @PostMapping(path = "/Loginutilisateur", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> signIn(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        User user = userInterface.authenticate(email, password);

        if (user != null) {
            return ResponseEntity.ok("logged in successfully");
        } else {
            // Renvoyer un JSON bien formé
            Map<String, String> error = Map.of("error", "Email ou mot de passe incorrect");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
    @PostMapping(
            path = "/updateUtilisateur/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User updatedUser) {
        try {
            User user = userInterface.UpdateUser(id, updatedUser);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Erreur lors de la mise à jour : " + e.getMessage()));
        }
    }

    @DeleteMapping(path = "deleteUser/{idUser}")
    public ResponseEntity<String> deleteUser(@PathVariable Long idUser) {
        userInterface.DeleteUser(idUser);
        return ResponseEntity.ok("User deleted successfully");
    }
    @PutMapping(path = "assignclienttouser/{userId}/clients/{clientId}")
    public ResponseEntity<Client> assignClientToUser(
            @PathVariable Long userId,
            @PathVariable Long clientId) {

        Client updatedClient = userInterface.assignExistingClientToUser(userId, clientId);
        return ResponseEntity.ok(updatedClient);
    }
    @GetMapping(path ="listedesutilisateur/ListUsers")
    public ResponseEntity<List<User>> listUsers() {
        List<User> users = userInterface.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping(path="afficherutilisateuravecEmail/{emailUser}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String emailUser) {
        User users = userInterface.getUserByEmail(emailUser);
        if(users == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(users);
    }

    @GetMapping(path ="Afficherutilisateuravecclient/{id}")
    public ResponseEntity<User> getUserWithClients(@PathVariable Long id) {
        User user = userInterface.getUsersWithclients(id);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping(path="desafecterclientfromuser/{userId}/{clientId}")
    public ResponseEntity<Client> desaffectClientFromUser(
            @PathVariable Long userId,
            @PathVariable Long clientId) {
        Client updatedClient = userInterface.DesafecterClientsFromUser(userId, clientId);
        return ResponseEntity.ok(updatedClient);
    }




}
