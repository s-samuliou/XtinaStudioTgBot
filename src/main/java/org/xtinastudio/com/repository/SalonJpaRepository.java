package org.xtinastudio.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.xtinastudio.com.entity.Salon;

@Repository
public interface SalonJpaRepository extends JpaRepository<Salon, Long> {

    Salon findByAddress(String address);
}
