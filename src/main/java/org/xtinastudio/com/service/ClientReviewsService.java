package org.xtinastudio.com.service;

import org.xtinastudio.com.entity.Client;
import org.xtinastudio.com.entity.ClientReview;
import org.xtinastudio.com.entity.Master;
import org.xtinastudio.com.entity.MasterReview;

import java.util.List;

public interface ClientReviewsService {

    ClientReview create(ClientReview clientReview);

    ClientReview editById(Long id, ClientReview clientReview);

    ClientReview findById(Long id);

    ClientReview findByClient(Client client);

    ClientReview findByMaster(Master master);

    int countByClient(Client client);

    List<ClientReview> getByClient(Client client);

    Double getClientRating(Client client);

    List<ClientReview> getAll();

    void delete(Long id);
}
