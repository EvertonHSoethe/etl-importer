package io.github.evertonsoethe.etlimporter.batch.mapper;

import io.github.evertonsoethe.etlimporter.batch.dto.SaleCsvDto;
import org.springframework.batch.infrastructure.item.file.mapping.FieldSetMapper;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class SaleFieldSetMapper implements FieldSetMapper<SaleCsvDto> {

    @Override
    public SaleCsvDto mapFieldSet(FieldSet fieldSet) throws BindException {

        return SaleCsvDto.builder()
                .saleId(fieldSet.readLong("sale_id"))
                .saleDate(LocalDate.parse(fieldSet.readString("sale_date")))
                .customerId(fieldSet.readLong("customer_id"))
                .customerName(fieldSet.readString("customer_name"))
                .productId(fieldSet.readString("product_id"))
                .productName(fieldSet.readString("product_name"))
                .category(fieldSet.readString("category"))
                .quantity(fieldSet.readInt("quantity"))
                .unitPrice(new BigDecimal(fieldSet.readString("unit_price")))
                .totalPrice(new BigDecimal(fieldSet.readString("total_price")))
                .paymentMethod(fieldSet.readString("payment_method"))
                .status(fieldSet.readString("status"))
                .build();

    }

}
