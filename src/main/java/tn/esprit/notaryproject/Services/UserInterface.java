package tn.esprit.notaryproject.Services;

import tn.esprit.notaryproject.Entities.Client;
import tn.esprit.notaryproject.Entities.User;

import java.util.List;

public interface UserInterface {
    public User registerUser(User user);
    User authenticate(String EmailUser, String Password);
    User UpdateUser(Long idUser , User updateduser);
    public void DeleteUser(Long idUser);
    List<User> getAllUsers();
    User getUserByEmail(String emailUser);
    Client assignExistingClientToUser(Long userId, Long Idclient);
    Client DesafecterClientsFromUser(Long userId, Long Idclient);
    User getUsersWithclients(Long userId);
     

}
