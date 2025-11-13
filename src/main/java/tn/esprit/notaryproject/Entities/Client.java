package tn.esprit.notaryproject.Entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.annotation.Generated;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clients")
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Idclient;
    @Column(nullable = false)
    private String NomClient;
    @Column(nullable = false)
    private String PrenomClient;
    @Column(nullable = false , name = "nationalid")
    private String NationalID;
    private String addresse ;
    private String numeroTelephone;
    @ManyToOne
    @JsonIgnore
    private User users;
    @OneToMany(cascade = CascadeType.ALL , mappedBy = "clients")
    List<Article> articles = new ArrayList<>();
    @OneToMany(cascade = CascadeType.ALL , mappedBy ="clients")
    List<Cheque> cheques = new ArrayList<>();

}
