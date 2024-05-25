package org.xtinastudio.com.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.xtinastudio.com.enums.Language;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "language")
    private Language language;

    @Column(name = "registration_date")
    private LocalDate registrationDate = LocalDate.now();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Appointment> appointments;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<MasterReview> masterReviews;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<ClientReview> clientReviews;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "salon_id")
    private Salon salon;
}
