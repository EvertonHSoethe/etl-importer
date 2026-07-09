package io.github.evertonsoethe.etlimporter.repository;

import io.github.evertonsoethe.etlimporter.domain.ImportExecution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtlExecutionRepository extends JpaRepository<ImportExecution, Long> {

}
