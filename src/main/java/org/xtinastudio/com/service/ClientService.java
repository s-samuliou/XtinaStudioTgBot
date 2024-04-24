package org.xtinastudio.com.service;

import org.xtinastudio.com.entity.Client;

import java.util.List;

public interface ClientService {

    Client create(Client client);

    Client editById(Long id, Client client);

    Client findById(Long id);

    Client findByPhoneNumber(String phoneNumber);

    Boolean existsByChatId(Long chatId);

    Client findByChatId(Long chatId);

    List<Client> getAll();

    void delete(Long id);
}
