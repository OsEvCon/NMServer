package model;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ProcedureRepository extends CrudRepository<Procedure, Integer> {
}
