package org.xtinastudio.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.xtinastudio.com.entity.Client;
import org.xtinastudio.com.entity.Master;
import org.xtinastudio.com.entity.MasterReview;

@Repository
public interface MasterReviewJpaRepository extends JpaRepository<MasterReview, Long> {

    MasterReview findByClient(Client client);

    MasterReview findByMaster(Master master);
}
