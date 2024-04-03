package model;

import java.util.List;
import java.util.Optional;

public interface ScheduleRepository {
    Optional<List<Schedule>> getSchedulesByMasterId();
}
