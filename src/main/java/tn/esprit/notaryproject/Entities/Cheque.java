package tn.esprit.notaryproject.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cheques")
public class Cheque {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long IdCheque;

    @Column(nullable = false)
    private Long ChequeNumber;

    @Column(nullable = false)
    private String BankeName;

    @Column(nullable = false)
    private Float Montant;

    @Column(nullable = false)
    private Date DepositDateCheque;

    @Enumerated(EnumType.STRING)
    private ChequesStatus status;

    @Lob
    @Column(columnDefinition = "VARBINARY(MAX)")
    private byte[] imageData;;

    // Validation fields
    @Enumerated(EnumType.STRING)
    private ChequeValidationStatus validationStatus;


    @ManyToOne
    @JsonIgnore
    Client clients;

    @ManyToOne
    @JsonIgnore
    User users;
}