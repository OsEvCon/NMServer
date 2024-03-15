package model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface VisitRepository extends CrudRepository<Visit, Integer> {
    Optional<Visit> findVisitsByMasterId(Integer masterId);
}
