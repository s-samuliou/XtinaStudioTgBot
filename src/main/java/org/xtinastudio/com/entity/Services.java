package org.xtinastudio.com.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "services")
public class Services {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "kind")
    private String kind;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "duration")
    private int duration;

    @Column(name = "price")
    private BigDecimal price;

    @ManyToMany
    @JoinTable(name = "master_services",
            joinColumns = @JoinColumn(name = "service_id"),
            inverseJoinColumns = @JoinColumn(name = "master_id"))
    private List<Master> masters;

    @OneToMany(mappedBy = "service")
    private List<Appointment> appointments;

    @ManyToMany
    @JoinTable(name = "salon_services",
            joinColumns = @JoinColumn(name = "service_id"),
            inverseJoinColumns = @JoinColumn(name = "salon_id"))
    private List<Salon> salons;
}
