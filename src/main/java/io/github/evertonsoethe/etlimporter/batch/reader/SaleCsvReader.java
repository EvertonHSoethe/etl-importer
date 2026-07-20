package io.github.evertonsoethe.etlimporter.batch.reader;

import io.github.evertonsoethe.etlimporter.batch.dto.SaleCsvDto;
import io.github.evertonsoethe.etlimporter.batch.mapper.SaleFieldSetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration("saleCsvReaderConfig")
@RequiredArgsConstructor
public class SaleCsvReader {

    private final SaleFieldSetMapper mapper;

    @Bean
    public FlatFileItemReader<SaleCsvDto> saleCsvReader() {

        return new FlatFileItemReaderBuilder<SaleCsvDto>()

                .name("saleCsvReader")

                .resource(new ClassPathResource("static/sales_100000.csv"))

                .linesToSkip(1)

                .delimited()

                .delimiter(",")

                .names(
                        "sale_id",
                        "sale_date",
                        "customer_id",
                        "customer_name",
                        "product_id",
                        "product_name",
                        "category",
                        "quantity",
                        "unit_price",
                        "total_price",
                        "payment_method",
                        "status"
                )

                .fieldSetMapper(mapper)

                .build();

    }

}
