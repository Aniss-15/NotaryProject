package tn.esprit.notaryproject.Entities;

import jakarta.persistence.*;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name ="articles ")
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long IdArticle ;
    private String Reference ;
    private String Description ;
    private Date DateCreation ;
    private String Image ;
    @ManyToOne
    Client clients;


}
