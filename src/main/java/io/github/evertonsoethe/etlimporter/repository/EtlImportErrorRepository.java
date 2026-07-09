package io.github.evertonsoethe.etlimporter.repository;

import io.github.evertonsoethe.etlimporter.domain.ImportError;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtlImportErrorRepository extends JpaRepository<ImportError, Long> {

}
