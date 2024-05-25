package org.xtinastudio.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.xtinastudio.com.entity.ClientReview;

@Repository
public interface ClientsReviewJpaRepository extends JpaRepository<ClientReview, Long> {

}
