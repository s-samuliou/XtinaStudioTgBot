package org.xtinastudio.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xtinastudio.com.entity.MasterReview;
import org.xtinastudio.com.repository.MasterReviewJpaRepository;

import java.util.List;

@Service
public class MasterReviewServiceImpl implements MasterReviewService {

    @Autowired
    MasterReviewJpaRepository repository;

    @Override
    public MasterReview create(MasterReview masterReview) {
        return repository.save(masterReview);
    }

    @Override
    public MasterReview editById(Long id, MasterReview masterReview) {
        return null;
    }

    @Override
    public MasterReview findByMasterId(Long id) {
        return null;
    }

    @Override
    public List<MasterReview> getAll() {
        return repository.findAll();
    }

    @Override
    public void delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        }
    }
}
