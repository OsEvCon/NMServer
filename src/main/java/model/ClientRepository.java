package model;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
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

    //Метод для проверки есть ли у данного мастера(master) клиент с данным номером телефона(phoneNumber)
    boolean existsByMastersContainingAndPhoneNumber(Master master, String phoneNumber);

    /**
     * Метод для проверки есть ли у данного master клиента с phoneNumber исключая клиента с excludeId
     */
    @Query("SELECT COUNT(c) > 0 FROM Client c " +
            "JOIN c.masters m " +
            "WHERE m = :master AND c.phoneNumber = :phoneNumber AND c.id != :excludeId")
    boolean existsByMastersAndPhoneNumberExcludingId(
            @Param("master") Master master,
            @Param("phoneNumber") String phoneNumber,
            @Param("excludeId") Integer excludeId);
}
