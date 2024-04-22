package org.xtinastudio.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xtinastudio.com.entity.Client;
import org.xtinastudio.com.entity.Master;
import org.xtinastudio.com.entity.MasterReview;
import org.xtinastudio.com.exceptions.ClientNotFoundException;
import org.xtinastudio.com.exceptions.MasterNotFoundException;
import org.xtinastudio.com.exceptions.MasterReviewNotFoundException;
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
        MasterReview existingMasterReview = repository.findById(id).orElseThrow(() -> new MasterReviewNotFoundException("Master not found with id: " + id));

        existingMasterReview.setRating(masterReview.getRating());

        MasterReview updatedMasterReview = repository.save(existingMasterReview);

        return updatedMasterReview;
    }

    @Override
    public MasterReview findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new MasterReviewNotFoundException("Master not found with id: " + id));
    }

    @Override
    public MasterReview findByClient(Client client) {
        return repository.findByClient(client);
    }

    @Override
    public MasterReview findByMaster(Master master) {
        return repository.findByMaster(master);
    }

    @Override
    public List<MasterReview> getAll() {
        return repository.findAll();
    }

    @Override
    public void delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        } else {
            throw new MasterReviewNotFoundException("Master not found with id: " + id);
        }
    }
}
