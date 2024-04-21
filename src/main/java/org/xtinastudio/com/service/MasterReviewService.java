package org.xtinastudio.com.service;

import org.xtinastudio.com.entity.MasterReview;

import java.util.List;

public interface MasterReviewService {

    MasterReview create(MasterReview masterReview);

    MasterReview editById(Long id, MasterReview masterReview);

    MasterReview findByMasterId(Long id);

    List<MasterReview> getAll();

    void delete(Long id);
}
