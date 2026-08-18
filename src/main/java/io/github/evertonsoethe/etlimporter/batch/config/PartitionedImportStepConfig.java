package io.github.evertonsoethe.etlimporter.batch.config;

import io.github.evertonsoethe.etlimporter.batch.dto.SaleCsvDto;
import io.github.evertonsoethe.etlimporter.batch.partition.CsvFilePartitioner;
import io.github.evertonsoethe.etlimporter.config.ImportProperties;
import io.github.evertonsoethe.etlimporter.domain.Sale;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@RequiredArgsConstructor
public class PartitionedImportStepConfig {

    private static final int CHUNK_SIZE = 500;

    private final JobRepository jobRepository;
    private final ImportProperties importProperties;
    private final ItemReader<SaleCsvDto> saleCsvReader;
    private final ItemProcessor<SaleCsvDto, Sale> saleProcessor;
    private final ItemWriter<Sale> saleWriter;
    private final StepExecutionListener importErrorListener;

    @Bean
    @JobScope
    public Partitioner csvFilePartitioner(
            @Value("#{jobParameters['filePath']}") String filePath) {
        Resource resource = new FileSystemResource(filePath);
        return new CsvFilePartitioner(resource);
    }

    @Bean
    public Step workerStep() {
        return new StepBuilder("workerStep", jobRepository)
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

    @Bean
    public Step partitionedImportStep(Partitioner csvFilePartitioner) {
        int threads = importProperties.getPartition().getThreads();
        return new StepBuilder("partitionedImportStep", jobRepository)
                .partitioner("workerStep", csvFilePartitioner)
                .step(workerStep())
                .gridSize(threads)
                .taskExecutor(importTaskExecutor())
                .build();
    }

    @Bean
    public TaskExecutor importTaskExecutor() {
        int threads = importProperties.getPartition().getThreads();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setThreadNamePrefix("import-partition-");
        return executor;
    }
}
