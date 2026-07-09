package io.github.evertonsoethe.etlimporter.repository;

import io.github.evertonsoethe.etlimporter.domain.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, Long> {

}
