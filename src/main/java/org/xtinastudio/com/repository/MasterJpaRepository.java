package org.xtinastudio.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.xtinastudio.com.entity.Master;
import org.xtinastudio.com.entity.Salon;
import org.xtinastudio.com.entity.Services;

import java.util.Collection;
import java.util.List;

@Repository
public interface MasterJpaRepository extends JpaRepository<Master, Long> {

    Master findByLogin(String login);

    Master findByName(String name);

    Master findByChatId(Long chatId);

    boolean existsByChatId(Long chatId);

    List<Master> findByServicesContainingAndSalon(Services services, Salon salon);

    List<Master> getAllBySalon(Salon salon);
}
