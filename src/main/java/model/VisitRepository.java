package model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface VisitRepository extends CrudRepository<Visit, Integer> {
    Optional<List<Visit>> findVisitsByMasterId(Integer masterId);

    Optional<List<Visit>> findVisitsByMaster_Email(String masterEmail);
    Optional<List<Visit>> findVisitsByClient(Client client);
}
