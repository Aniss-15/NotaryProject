package tn.esprit.notaryproject.Services;

import tn.esprit.notaryproject.Entities.Client;
import tn.esprit.notaryproject.Entities.User;

import java.util.List;

public interface ClientInterface {
    Client addClient(Client client);
    Client updateClient(Long  Idclient , Client clientDetails);
    void deleteClient(Long Idclient);
    Client SearchClientByNationalID(String NationalID);
    Client SearchClientByName(String PrenomClient);
    List<Client> DisplayAllClients();
    Client AssingArticleToClient(Long Idclient , Long Idarticle);
    Client DessafecterArticleToClient(Long Idclient , Long Idarticle);
    Client AssingChequeTocClient(Long Idclient , Long IdCheque);
   //test
}
