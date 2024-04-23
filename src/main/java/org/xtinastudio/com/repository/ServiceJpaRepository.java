package org.xtinastudio.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.xtinastudio.com.entity.Services;

@Repository
public interface ServiceJpaRepository extends JpaRepository<Services, Long> {

    Services findByName(String name);
}
