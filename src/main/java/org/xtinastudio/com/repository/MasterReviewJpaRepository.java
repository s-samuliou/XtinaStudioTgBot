package org.xtinastudio.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.xtinastudio.com.entity.Client;
import org.xtinastudio.com.entity.Master;
import org.xtinastudio.com.entity.MasterReview;

import java.util.List;

@Repository
public interface MasterReviewJpaRepository extends JpaRepository<MasterReview, Long> {

    MasterReview findByClient(Client client);

    MasterReview findByMaster(Master master);

    List<MasterReview> getByMaster(Master master);

    @Query("SELECT AVG(r.rating) FROM MasterReview r WHERE r.master = :master")
    Double findAverageRatingByMaster(@Param("master") Master master);

    int countByMaster(Master master);
}
