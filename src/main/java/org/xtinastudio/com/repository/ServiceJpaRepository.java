package org.xtinastudio.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.xtinastudio.com.entity.Salon;
import org.xtinastudio.com.entity.Services;

import java.util.List;

@Repository
public interface ServiceJpaRepository extends JpaRepository<Services, Long> {

    Services findByName(String name);

    List<Services> findBySalons(Salon salon);

    List<Services> findBySalonsAndKind(Salon salon, String kind);
}
