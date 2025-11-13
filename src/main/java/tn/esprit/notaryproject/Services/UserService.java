package tn.esprit.notaryproject.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.esprit.notaryproject.Entities.Client;
import tn.esprit.notaryproject.Entities.User;
import tn.esprit.notaryproject.Repository.ClientRepo;
import tn.esprit.notaryproject.Repository.UserRepo;

import java.util.List;
@Service
public class UserService implements UserInterface{
    @Autowired
    UserRepo userRepo;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    ClientRepo clientRepo;

    @Override
    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepo.save(user);

    }

    @Override
    public User authenticate(String emailUser, String Password) {
        return userRepo.findByEmailUser(emailUser)
                .filter(u -> passwordEncoder.matches(Password, u.getPassword()))
                .orElse(null);
    }


    @Override
    public User UpdateUser(Long id, User updatedUser) {
        return userRepo.findById(id)
                .map(existingUser -> {
                    existingUser.setNomUser(updatedUser.getNomUser());
                    existingUser.setPrenomUser(updatedUser.getPrenomUser());
                    existingUser.setEmailUser(updatedUser.getEmailUser());
                    existingUser.setRole(updatedUser.getRole());
                    existingUser.setImage(updatedUser.getImage());
                    if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                        // Facultatif : éviter de réencoder si c’est déjà le même hash
                        if (!passwordEncoder.matches(updatedUser.getPassword(), existingUser.getPassword())) {
                            existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
                        }
                    }
                    return userRepo.save(existingUser);
                })
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }

    @Override
    public void DeleteUser(Long idUser) {
        User user = userRepo.findUsersByIdUser(idUser);
        userRepo.delete(user);
    }


    @Override
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    @Override
    public User getUserByEmail(String emailUser) {
        return userRepo.findByEmailUser(emailUser).orElse(null);
    }


    @Override
    public Client assignExistingClientToUser(Long userId, Long Idclient) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        Client client = clientRepo.findById(Idclient)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        client.setUsers(user);
        user.getClients().add(client);
        clientRepo.save(client);
        return client;
    }

    @Override
    public Client DesafecterClientsFromUser(Long userId, Long Idclient) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Client client = clientRepo.findById(Idclient)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        // Check if client is associated with user
        if (user.getClients() == null || user.getClients().stream().noneMatch(c -> c.getIdclient().equals(Idclient))) {
            throw new IllegalArgumentException("Client not associated with this user");
        }

        // Remove association
        client.setUsers(null);

        return clientRepo.save(client);
    }


    @Override
    public User getUsersWithclients(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

}
