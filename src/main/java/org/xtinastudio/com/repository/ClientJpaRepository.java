package org.xtinastudio.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.xtinastudio.com.entity.Client;

@Repository
public interface ClientJpaRepository extends JpaRepository<Client, Long> {

    Client findClientByPhoneNumber(String phoneNumber);

    Boolean existsByChatId(Long chatId);

    Client findByChatId(Long chatId);
}
