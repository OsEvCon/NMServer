package model;

import model.Master;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MasterRepository extends CrudRepository<Master, Integer> {

    Optional<Master> findMasterById(Integer id);

    Optional<Master> findByName(String name);

    /*
    if (optionalMaster.isPresent()) {
    Master master = optionalMaster.get();
    // Обработка найденного объекта Master
} else {
    // Обработка случая, когда Master с данным id не найден
}
     */
}
