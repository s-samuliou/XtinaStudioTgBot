package org.xtinastudio.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.xtinastudio.com.entity.Client;
import org.xtinastudio.com.entity.ClientReview;
import org.xtinastudio.com.entity.Master;

import java.util.List;

@Repository
public interface ClientsReviewJpaRepository extends JpaRepository<ClientReview, Long> {

    ClientReview findByClient(Client client);

    ClientReview findByMaster(Master master);

    List<ClientReview> getByClient(Client client);

    @Query("SELECT AVG(r.rating) FROM ClientReview r WHERE r.client = :client")
    Double findAverageRatingByClient(@Param("client") Client client);

    int countByClient(Client client);
}
