package io.github.evertonsoethe.etlimporter.batch.processor;

import io.github.evertonsoethe.etlimporter.batch.dto.SaleCsvDto;
import io.github.evertonsoethe.etlimporter.domain.Sale;
import io.github.evertonsoethe.etlimporter.enums.PaymentMethodEnum;
import io.github.evertonsoethe.etlimporter.enums.SaleStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SaleProcessorTest {

    private SaleProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new SaleProcessor();
    }

    @Test
    @DisplayName("Deve retornar Sale com todos os campos mapeados quando item é válido")
    void shouldReturnSaleWithAllFieldsMapped_whenItemIsValid() {
        SaleCsvDto dto = buildValidDto();

        Sale result = processor.process(dto);

        assertNotNull(result);
        assertEquals(dto.getSaleId(), result.getSaleId());
        assertEquals(dto.getSaleDate(), result.getSaleDate());
        assertEquals(dto.getCustomerId(), result.getCustomerId());
        assertEquals(dto.getCustomerName(), result.getCustomerName());
        assertEquals(dto.getProductId(), result.getProductId());
        assertEquals(dto.getProductName(), result.getProductName());
        assertEquals(dto.getCategory(), result.getCategory());
        assertEquals(dto.getQuantity(), result.getQuantity());
        assertEquals(dto.getUnitPrice(), result.getUnitPrice());
        assertEquals(dto.getTotalPrice(), result.getTotalPrice());
        assertEquals(PaymentMethodEnum.PIX, result.getPaymentMethod());
        assertEquals(SaleStatusEnum.COMPLETED, result.getStatus());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando quantity = 0")
    void shouldThrowException_whenQuantityIsZero() {
        SaleCsvDto dto = buildValidDto();
        dto.setQuantity(0);

        assertThrows(IllegalArgumentException.class, () -> processor.process(dto));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando quantity = -1")
    void shouldThrowException_whenQuantityIsNegative() {
        SaleCsvDto dto = buildValidDto();
        dto.setQuantity(-1);

        assertThrows(IllegalArgumentException.class, () -> processor.process(dto));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando paymentMethod é inválido")
    void shouldThrowException_whenPaymentMethodIsInvalid() {
        SaleCsvDto dto = buildValidDto();
        dto.setPaymentMethod("INVALID");

        assertThrows(IllegalArgumentException.class, () -> processor.process(dto));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando status é inválido")
    void shouldThrowException_whenStatusIsInvalid() {
        SaleCsvDto dto = buildValidDto();
        dto.setStatus("invalid_status");

        assertThrows(IllegalArgumentException.class, () -> processor.process(dto));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando paymentMethod está em lowercase (case-sensitive)")
    void shouldThrowException_whenPaymentMethodIsLowercase() {
        SaleCsvDto dto = buildValidDto();
        dto.setPaymentMethod("cash");

        assertThrows(IllegalArgumentException.class, () -> processor.process(dto));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando status está em lowercase (case-sensitive)")
    void shouldThrowException_whenStatusIsLowercase() {
        SaleCsvDto dto = buildValidDto();
        dto.setStatus("completed");

        assertThrows(IllegalArgumentException.class, () -> processor.process(dto));
    }

    private SaleCsvDto buildValidDto() {
        return SaleCsvDto.builder()
                .saleId(1L)
                .saleDate(LocalDate.of(2024, 1, 15))
                .customerId(100L)
                .customerName("João Silva")
                .productId("PROD-001")
                .productName("Notebook")
                .category("Eletrônicos")
                .quantity(2)
                .unitPrice(new BigDecimal("2500.00"))
                .totalPrice(new BigDecimal("5000.00"))
                .paymentMethod("PIX")
                .status("COMPLETED")
                .build();
    }
}
