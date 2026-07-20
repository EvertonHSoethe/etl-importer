package io.github.evertonsoethe.etlimporter.batch.writer;

import io.github.evertonsoethe.etlimporter.domain.Sale;
import io.github.evertonsoethe.etlimporter.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.data.RepositoryItemWriter;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("saleWriterConfig")
@RequiredArgsConstructor
public class SaleWriter {

    private final SaleRepository repository;

    @Bean
    public ItemWriter<Sale> saleWriter() {

        return new RepositoryItemWriter<>(repository);
    }

}
