package io.github.evertonsoethe.etlimporter.batch.reader;

import io.github.evertonsoethe.etlimporter.batch.dto.SaleCsvDto;
import io.github.evertonsoethe.etlimporter.batch.mapper.SaleFieldSetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

@Configuration("saleCsvReaderConfig")
@RequiredArgsConstructor
public class SaleCsvReader {

    private final SaleFieldSetMapper mapper;

    @Bean
    @StepScope
    public FlatFileItemReader<SaleCsvDto> saleCsvReader(
            @Value("#{jobParameters['filePath']}") String filePath,
            @Value("#{stepExecutionContext['startLine']}") Long startLine,
            @Value("#{stepExecutionContext['endLine']}") Long endLine) {

        Resource resource = new FileSystemResource(filePath);

        // startLine is 1-indexed where line 1 = header, line 2 = first data
        // linesToSkip skips the first N lines (header + lines before this partition's range)
        int linesToSkip = startLine != null ? startLine.intValue() - 1 : 1;

        // maxItemCount limits how many items this reader will produce
        int maxItemCount = (startLine != null && endLine != null)
                ? (int) (endLine - startLine + 1)
                : Integer.MAX_VALUE;

        return new FlatFileItemReaderBuilder<SaleCsvDto>()
                .name("partitionedSaleCsvReader")
                .resource(resource)
                .linesToSkip(linesToSkip)
                .maxItemCount(maxItemCount)
                .delimited()
                .delimiter(",")
                .names(
                        "sale_id", "sale_date", "customer_id", "customer_name",
                        "product_id", "product_name", "category", "quantity",
                        "unit_price", "total_price", "payment_method", "status"
                )
                .fieldSetMapper(mapper)
                .build();
    }
}
