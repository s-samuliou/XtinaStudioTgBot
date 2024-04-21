package org.xtinastudio.com.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.xtinastudio.com.enums.Role;
import org.xtinastudio.com.enums.WorkStatus;

import java.util.List;
import java.util.Set;

@Entity
@Table(name = "masters")
public class Master {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id")
    private String chatId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "login", nullable = false)
    private String login;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "last_name", nullable = false)
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
    private List<Service> services;

    @OneToMany(mappedBy = "master")
    private List<MasterReview> masterReviews;

}
