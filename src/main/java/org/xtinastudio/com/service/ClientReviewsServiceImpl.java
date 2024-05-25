package org.xtinastudio.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xtinastudio.com.entity.Client;
import org.xtinastudio.com.entity.ClientReview;
import org.xtinastudio.com.entity.Master;
import org.xtinastudio.com.exceptions.ClientReviewNotFoundException;
import org.xtinastudio.com.repository.ClientsReviewJpaRepository;

import java.util.List;

@Service
public class ClientReviewsServiceImpl implements ClientReviewsService {

    @Autowired
    ClientsReviewJpaRepository repository;

    @Override
    public ClientReview create(ClientReview clientReview) {
        return repository.save(clientReview);
    }

    @Override
    public ClientReview editById(Long id, ClientReview clientReview) {
        ClientReview existingMasterReview = repository.findById(id).orElseThrow(() -> new ClientReviewNotFoundException("Client Review not found with id: " + id));

        existingMasterReview.setRating(existingMasterReview.getRating());

        ClientReview updatedClientReview = repository.save(existingMasterReview);

        return updatedClientReview;
    }

    @Override
    public ClientReview findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ClientReviewNotFoundException("Client Review not found with id: " + id));
    }

    @Override
    public ClientReview findByClient(Client client) {
        return repository.findByClient(client);
    }

    @Override
    public ClientReview findByMaster(Master master) {
        return repository.findByMaster(master);
    }

    @Override
    public int countByClient(Client client) {
        return repository.countByClient(client);
    }

    @Override
    public List<ClientReview> getByClient(Client client) {
        return repository.getByClient(client);
    }

    @Override
    public Double getClientRating(Client client) {
        return repository.findAverageRatingByClient(client);
    }

    @Override
    public List<ClientReview> getAll() {
        return repository.findAll();
    }

    @Override
    public void delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        } else {
            throw new ClientReviewNotFoundException("Client Review not found with id: " + id);
        }
    }
}
