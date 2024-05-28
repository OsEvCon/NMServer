package model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitTimeRepository extends CrudRepository<VisitTime, Integer> {
}
