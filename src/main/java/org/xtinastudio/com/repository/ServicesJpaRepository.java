package org.xtinastudio.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.xtinastudio.com.entity.Service;

@Repository
public interface ServicesJpaRepository extends JpaRepository<Service, Long> {
}
