package io.github.evertonsoethe.etlimporter.batch.writer;

import io.github.evertonsoethe.etlimporter.domain.Sale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

import javax.sql.DataSource;
import java.io.StringReader;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ItemWriter que utiliza PostgreSQL COPY protocol para escrita bulk.
 * Cada chunk é serializado em formato pipe-delimited e enviado via CopyManager.copyIn().
 * A operação é atômica por chunk: em caso de falha, todos os itens do chunk são considerados falhos.
 *
 * @see CopyItemWriterConfig para ativação via property etl.import.writer.strategy=pg-copy
 */
@Slf4j
@RequiredArgsConstructor
public class CopyItemWriter implements ItemWriter<Sale> {

    private static final String COPY_SQL =
            "COPY sale (sale_id, sale_date, customer_id, customer_name, product_id, " +
            "product_name, category, quantity, unit_price, total_price, payment_method, " +
            "status, created_at) FROM STDIN WITH (DELIMITER '|', NULL '')";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DataSource dataSource;

    @Override
    public void write(Chunk<? extends Sale> chunk) throws Exception {
        String csvData = serializeChunk(chunk.getItems());

        try (Connection conn = dataSource.getConnection()) {
            PGConnection pgConn = conn.unwrap(PGConnection.class);
            CopyManager copyManager = pgConn.getCopyAPI();

            try (StringReader reader = new StringReader(csvData)) {
                long rowsCopied = copyManager.copyIn(COPY_SQL, reader);
                log.debug("COPY inserted {} rows", rowsCopied);
            }
        } catch (Exception e) {
            log.error("COPY operation failed for chunk of {} items, all items considered failed (atomic rollback)",
                    chunk.size(), e);
            throw e;
        }
    }

    String serializeChunk(List<? extends Sale> items) {
        StringBuilder sb = new StringBuilder(items.size() * 200);
        String now = LocalDateTime.now().format(TS_FMT);

        for (Sale sale : items) {
            sb.append(sale.getSaleId()).append('|')
              .append(sale.getSaleDate().format(DATE_FMT)).append('|')
              .append(sale.getCustomerId()).append('|')
              .append(escapeNull(sale.getCustomerName())).append('|')
              .append(escapeNull(sale.getProductId())).append('|')
              .append(escapeNull(sale.getProductName())).append('|')
              .append(escapeNull(sale.getCategory())).append('|')
              .append(sale.getQuantity()).append('|')
              .append(sale.getUnitPrice()).append('|')
              .append(sale.getTotalPrice()).append('|')
              .append(sale.getPaymentMethod().name()).append('|')
              .append(sale.getStatus().name()).append('|')
              .append(now)
              .append('\n');
        }
        return sb.toString();
    }

    private String escapeNull(String value) {
        return value != null ? value : "";
    }
}
