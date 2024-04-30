package org.xtinastudio.com.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.xtinastudio.com.enums.Role;
import org.xtinastudio.com.enums.WorkStatus;

import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "masters")
public class Master {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "name")
    private String name;

    @Column(name = "login")
    private String login;

    @Column(name = "password")
    private String password;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "photo_url")
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private WorkStatus workStatus;

    @OneToMany(mappedBy = "master")
    private List<Appointment> appointments;

    @ManyToMany(mappedBy = "masters")
    private List<Services> services;

    @OneToMany(mappedBy = "master")
    private List<MasterReview> masterReviews;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "salon_id")
    private Salon salon;
}
