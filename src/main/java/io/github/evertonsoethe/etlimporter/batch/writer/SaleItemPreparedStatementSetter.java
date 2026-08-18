package io.github.evertonsoethe.etlimporter.batch.writer;

import io.github.evertonsoethe.etlimporter.domain.Sale;
import org.springframework.batch.infrastructure.item.database.ItemPreparedStatementSetter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class SaleItemPreparedStatementSetter implements ItemPreparedStatementSetter<Sale> {

    @Override
    public void setValues(Sale sale, PreparedStatement ps) throws SQLException {
        ps.setLong(1, sale.getSaleId());
        ps.setObject(2, sale.getSaleDate());
        ps.setLong(3, sale.getCustomerId());
        ps.setString(4, sale.getCustomerName());
        ps.setString(5, sale.getProductId());
        ps.setString(6, sale.getProductName());
        ps.setString(7, sale.getCategory());
        ps.setInt(8, sale.getQuantity());
        ps.setBigDecimal(9, sale.getUnitPrice());
        ps.setBigDecimal(10, sale.getTotalPrice());
        ps.setString(11, sale.getPaymentMethod().name());
        ps.setString(12, sale.getStatus().name());
        ps.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
    }
}
