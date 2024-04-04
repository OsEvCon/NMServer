package model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface ScheduleRepository extends CrudRepository<Schedule, Integer> {
    Optional<List<Schedule>> getSchedulesByMasterId(Integer id);

}
