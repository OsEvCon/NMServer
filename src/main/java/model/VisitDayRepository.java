package model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VisitDayRepository extends CrudRepository<VisitDay, Integer> {
    List<VisitDay> getVisitDaysByMasterId(int masterId);
}
