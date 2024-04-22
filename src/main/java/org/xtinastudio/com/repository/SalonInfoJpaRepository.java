package org.xtinastudio.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.xtinastudio.com.entity.SalonInfo;

@Repository
public interface SalonInfoJpaRepository extends JpaRepository<SalonInfo, Long> {

    SalonInfo findByName(String name);
}
