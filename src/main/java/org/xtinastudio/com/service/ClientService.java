package org.xtinastudio.com.service;

import org.xtinastudio.com.entity.Client;

import java.util.List;

public interface ClientService {

    Client create(Client client);

    Client editById(Long id, Client client);

    Client findByPhoneNumber(Long id);

    List<Client> getAll();

    void delete(Long id);
}
