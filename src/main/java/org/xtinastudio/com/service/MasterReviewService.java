package org.xtinastudio.com.service;

import org.springframework.data.repository.query.Param;
import org.xtinastudio.com.entity.Client;
import org.xtinastudio.com.entity.Master;
import org.xtinastudio.com.entity.MasterReview;

import java.util.List;

public interface MasterReviewService {

    MasterReview create(MasterReview masterReview);

    MasterReview editById(Long id, MasterReview masterReview);

    MasterReview findById(Long id);

    MasterReview findByClient(Client client);

    MasterReview findByMaster(Master master);

    int countByMaster(Master master);

    List<MasterReview> getByMaster(Master master);

    Double getMasterRating(Master master);

    List<MasterReview> getAll();

    void delete(Long id);
}
