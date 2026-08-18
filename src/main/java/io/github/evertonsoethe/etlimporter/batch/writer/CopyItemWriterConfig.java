package io.github.evertonsoethe.etlimporter.batch.writer;

import io.github.evertonsoethe.etlimporter.domain.Sale;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Configuração condicional para o CopyItemWriter.
 * Ativado apenas quando etl.import.writer.strategy=pg-copy.
 * Mutuamente exclusivo com JdbcBatchSaleWriterConfig.
 */
@Configuration
@ConditionalOnProperty(name = "etl.import.writer.strategy", havingValue = "pg-copy")
public class CopyItemWriterConfig {

    @Bean
    public ItemWriter<Sale> saleWriter(DataSource dataSource) {
        return new CopyItemWriter(dataSource);
    }
}
