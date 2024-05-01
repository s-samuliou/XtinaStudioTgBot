package org.xtinastudio.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xtinastudio.com.entity.Client;
import org.xtinastudio.com.exceptions.ClientNotFoundException;
import org.xtinastudio.com.exceptions.MasterNotFoundException;
import org.xtinastudio.com.repository.ClientJpaRepository;

import java.util.List;
import java.util.Objects;

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
        Client existingClient = repository.findById(id).orElseThrow(() -> new ClientNotFoundException("Client not found with id: " + id));

        existingClient.setName(client.getName());
        existingClient.setPhoneNumber(client.getPhoneNumber());
        existingClient.setLanguage(client.getLanguage());
        existingClient.setSalon(client.getSalon());

        Client updatedClient = repository.save(existingClient);

        return updatedClient;
    }

    @Override
    public Client findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ClientNotFoundException("Client not found with id: " + id));
    }

    @Override
    public Client findByPhoneNumber(String phoneNumber) {
        return repository.findClientByPhoneNumber(phoneNumber);
    }

    @Override
    public Boolean existsByChatId(Long chatId) {
        return repository.existsByChatId(chatId);
    }

    @Override
    public Client findByChatId(Long chatId) {
        return repository.findByChatId(chatId);
    }

    /*@Override
    public Client getByChatId(Long chatId) {
        List<Client> clients = repository.findAll();
        return clients.stream()
                .filter(client -> Objects.equals(client.getChatId(), chatId))
                .findFirst()
                .orElse(null);
    }*/

    @Override
    public List<Client> getAll() {
        return repository.findAll();
    }

    @Override
    public void delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        } else {
            throw new ClientNotFoundException("Client not found with id: " + id);
        }
    }
}
