package repository;

import domain.ImportError;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtlImportErrorRepository extends JpaRepository<ImportError, Long> {

}
