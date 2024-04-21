package org.xtinastudio.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xtinastudio.com.entity.Client;
import org.xtinastudio.com.repository.ClientJpaRepository;

import java.util.List;

@Service
public class ClientServiceImpl implements ClientService {

    @Autowired
    ClientJpaRepository repository;

    @Override
    public Client create(Client client) {
        return repository.save(client);
    }

    @Override
    public Client editById(Long id, Client client) {
        return null;
    }

    @Override
    public Client findByPhoneNumber(Long id) {
        return null;
    }

    @Override
    public List<Client> getAll() {
        return repository.findAll();
    }

    @Override
    public void delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        }
    }
}
