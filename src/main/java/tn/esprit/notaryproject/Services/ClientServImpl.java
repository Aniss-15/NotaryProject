package tn.esprit.notaryproject.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.notaryproject.Entities.Article;
import tn.esprit.notaryproject.Entities.Cheque;
import tn.esprit.notaryproject.Entities.Client;
import tn.esprit.notaryproject.Entities.User;
import tn.esprit.notaryproject.Repository.ArticleRepo;
import tn.esprit.notaryproject.Repository.ChequeRepoo;
import tn.esprit.notaryproject.Repository.ClientRepo;

import java.util.List;
import java.util.Optional;

@Service
public class ClientServImpl implements ClientInterface{
    @Autowired
     ClientRepo clientRepository;
    @Autowired
     ArticleRepo articleRepository;
    @Autowired
    ChequeRepoo chequeRepoo;
    @Override
    public Client addClient(Client client) {
        if (client.getNomClient() == null || client.getNomClient().isEmpty()) {
            throw new IllegalArgumentException("Client last name is required");
        }
        if (client.getPrenomClient() == null || client.getPrenomClient().isEmpty()) {
            throw new IllegalArgumentException("Client first name is required");
        }
        if (client.getNationalID() == null || client.getNationalID().isEmpty()) {
            throw new IllegalArgumentException("National ID is required");
        }
        if (client.getNumeroTelephone() == null || client.getNumeroTelephone().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        if (!isNationalIDUnique(client.getNationalID(), null)) {
            throw new IllegalArgumentException("A client with this National ID already exists");
        }

        // Associate client with use
        return clientRepository.save(client);
    }

    @Override
    public Client updateClient(Long Idclient, Client clientDetails) {
        Client existingClient = clientRepository.findById(Idclient)
                .orElseThrow(() -> new IllegalArgumentException("Client not found with ID: " + Idclient));

        // Update all fields that are provided
        if (clientDetails.getNomClient() != null) {
            existingClient.setNomClient(clientDetails.getNomClient());
        }
        if (clientDetails.getPrenomClient() != null) {
            existingClient.setPrenomClient(clientDetails.getPrenomClient());
        }
        if (clientDetails.getNationalID() != null) {
            existingClient.setNationalID(clientDetails.getNationalID());
        }
        if (clientDetails.getAddresse() != null) {
            existingClient.setAddresse(clientDetails.getAddresse());
        }
        if (clientDetails.getNumeroTelephone() != null) {
            existingClient.setNumeroTelephone(clientDetails.getNumeroTelephone());
        }

        return clientRepository.save(existingClient);
    }

    @Override
    public void deleteClient(Long Idclient) {
        Client client = clientRepository.findById(Idclient).orElseThrow(() -> new IllegalArgumentException("Client not found with ID: " + Idclient));
        clientRepository.deleteById(Idclient);
    }


    private boolean isNationalIDUnique(String NationalID, Long Idclient ) {
        if (Idclient == null) {
            return !clientRepository.existsByNationalID((NationalID));
        } else {
            return !clientRepository.existsByNationalIDAndIdNot(NationalID, Idclient);
        }
    }

    @Override
    public Client SearchClientByNationalID(String NationalID) {
        Client client = clientRepository.findByNationalID(NationalID);
        if (client == null) {
            throw new IllegalArgumentException("Client not found with National ID: " + NationalID);
        }else
        {
            client.getNationalID();
            throw new IllegalArgumentException("client found" + client.getNationalID());
        }
    }

    @Override
    public Client SearchClientByName(String PrenomClient) {
        Client client = clientRepository.findByPrenomClient(PrenomClient);
        if (client == null) {
            throw new IllegalArgumentException("Client not found with Prenom Client: " + PrenomClient);

        }
        else
        {
            client.getNomClient();
            return client;
        }
    }

    @Override
    public List<Client> DisplayAllClients() {
        return clientRepository.findAll();
    }

    @Override
    public Client AssingArticleToClient(Long Idclient, Long Idarticle) {
        Client client = clientRepository.findById(Idclient).orElseThrow(() -> new IllegalArgumentException("Client not found with ID: " + Idclient));
        Article article = articleRepository.findById(Idarticle).orElseThrow(() -> new IllegalArgumentException("Article not found with ID: " + Idarticle));
        article.setClients(client);
        client.getArticles().add(article);
        articleRepository.save(article);
        return clientRepository.save(client);
    }

   @Override
    public Client DessafecterArticleToClient(Long Idclient, Long Idarticle) {
        Client client = clientRepository.findById(Idclient).orElseThrow(()-> new IllegalArgumentException("Id client " + Idclient));
        Article article = articleRepository.findById(Idarticle).orElseThrow(()-> new IllegalArgumentException("Id Article " + Idarticle));
        if(client.getArticles() == null || client.getArticles().stream().noneMatch(a -> a.getIdArticle().equals(Idarticle))) {
            throw new IllegalArgumentException("Article not found with ID: " + Idarticle);
        }
        article.setClients(null);
        articleRepository.save(article);
        return client;
    }

    @Override
    public Client AssingChequeTocClient(Long Idclient, Long IdCheque) {
        Client client = clientRepository.findById(Idclient).orElseThrow(()->new IllegalArgumentException("Idlcient" +Idclient));
        Cheque cheque = chequeRepoo.findById(IdCheque).orElseThrow(()->new IllegalArgumentException("Cheque not found with ID: " + IdCheque));
        cheque.setClients(client);
        client.getCheques().add(cheque);
        clientRepository.save(client);
        return client;
    }


}
