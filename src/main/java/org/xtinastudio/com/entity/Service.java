package org.xtinastudio.com.entity;

import jakarta.persistence.*;
import org.xtinastudio.com.service.MasterService;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "services")
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "duration", nullable = false)
    private int duration;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @ManyToMany
    @JoinTable(name = "master_services",
            joinColumns = @JoinColumn(name = "service_id"),
            inverseJoinColumns = @JoinColumn(name = "master_id"))
    private List<Master> masters;

    @OneToMany(mappedBy = "service")
    private List<Appointment> appointments;
}
