package io.github.evertonsoethe.etlimporter.batch.config;

import io.github.evertonsoethe.etlimporter.batch.dto.SaleCsvDto;
import io.github.evertonsoethe.etlimporter.batch.listener.ImportErrorListener;
import io.github.evertonsoethe.etlimporter.domain.Sale;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class ImportSalesStepConfig {

    private static final int CHUNK_SIZE = 500;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ItemReader<SaleCsvDto> saleCsvReader;
    private final ItemProcessor<SaleCsvDto, Sale> saleProcessor;
    private final ItemWriter<Sale> saleWriter;
    private final ImportErrorListener importErrorListener;

    @Bean
    public Step importSalesStep() {
        return new StepBuilder("importSalesStep", jobRepository)
                //.<SaleCsvDto, Sale>chunk(CHUNK_SIZE, transactionManager)
                .<SaleCsvDto, Sale>chunk(CHUNK_SIZE)
                .reader(saleCsvReader)
                .processor(saleProcessor)
                .writer(saleWriter)
                .listener(importErrorListener)
                .faultTolerant()
                .skipLimit(10000)
                .skip(Exception.class)
                .listener((SkipListener<SaleCsvDto, Sale>) importErrorListener)
                .build();
    }
}
