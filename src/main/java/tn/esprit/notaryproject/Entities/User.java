package tn.esprit.notaryproject.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUser;

    // CORRECTION: Utilisez camelCase pour correspondre au JSON
    private String nomUser;        // Était "NomUser"
    private String prenomUser;     // Était "PrenomUser"
    private String emailUser;      // Était "EmailUser"
    private String password;       // Déjà correct
    private String image;          // Déjà correct

    @Enumerated(EnumType.STRING)
    private RoleUser role;         // Était "Role"

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "users" , fetch = FetchType.LAZY)
    private List<Client> clients = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "users")
    @JsonIgnore
    private List<Cheque> cheques = new ArrayList<>();
}