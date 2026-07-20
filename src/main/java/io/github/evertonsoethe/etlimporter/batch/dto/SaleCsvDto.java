package io.github.evertonsoethe.etlimporter.batch.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Data
public class SaleCsvDto {

    private Long saleId;

    private LocalDate saleDate;

    private Long customerId;

    private String customerName;

    private String productId;

    private String productName;

    private String category;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private String paymentMethod;

    private String status;

}