package model;

import model.Client;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends CrudRepository<Client, Integer> {

    Optional<Client> findClientById(Integer id);

    Optional<Client> findClientByName(String name);

    // Найти клиентов, у которых В СПИСКЕ masters есть конкретный мастер
    List<Client> findByMastersContaining(Master master);

    Optional<Client> findByIdAndMastersContaining(Integer id, Master master);

}
