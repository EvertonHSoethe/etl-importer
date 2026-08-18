package io.github.evertonsoethe.etlimporter.batch.writer;

import io.github.evertonsoethe.etlimporter.domain.Sale;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "etl.import.writer.strategy", havingValue = "jdbc-batch", matchIfMissing = true)
public class JdbcBatchSaleWriterConfig {

    private static final String INSERT_SQL = """
            INSERT INTO sale (sale_id, sale_date, customer_id, customer_name,
                             product_id, product_name, category, quantity,
                             unit_price, total_price, payment_method, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    @Bean
    public ItemWriter<Sale> saleWriter(DataSource dataSource) {
        JdbcBatchItemWriter<Sale> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql(INSERT_SQL);
        writer.setItemPreparedStatementSetter(new SaleItemPreparedStatementSetter());
        return writer;
    }
}
