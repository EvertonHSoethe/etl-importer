package io.github.evertonsoethe.etlimporter.batch.processor;

import io.github.evertonsoethe.etlimporter.batch.dto.SaleCsvDto;
import io.github.evertonsoethe.etlimporter.domain.Sale;
import io.github.evertonsoethe.etlimporter.enums.PaymentMethodEnum;
import io.github.evertonsoethe.etlimporter.enums.SaleStatusEnum;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class SaleProcessor implements ItemProcessor<SaleCsvDto, Sale> {

    @Override
    public Sale process(SaleCsvDto item) {

        if (item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        return Sale.builder()

                .saleId(item.getSaleId())

                .saleDate(item.getSaleDate())

                .customerId(item.getCustomerId())

                .customerName(item.getCustomerName())

                .productId(item.getProductId())

                .productName(item.getProductName())

                .category(item.getCategory())

                .quantity(item.getQuantity())

                .unitPrice(item.getUnitPrice())

                .totalPrice(item.getTotalPrice())

                .paymentMethod(
                        PaymentMethodEnum.valueOf(item.getPaymentMethod())
                )

                .status(
                        SaleStatusEnum.valueOf(item.getStatus())
                )

                .build();

    }

}
