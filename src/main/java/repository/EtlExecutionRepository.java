package repository;

import domain.ImportExecution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtlExecutionRepository extends JpaRepository<ImportExecution, Long> {

}
