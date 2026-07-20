package io.github.evertonsoethe.etlimporter.batch.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ImportSalesJobConfig {

    private final JobRepository jobRepository;
    private final Step importSalesStep;
    private final JobExecutionListener importJobListener;

    @Bean
    public Job importSalesJob() {

        return new JobBuilder("importSalesJob", jobRepository)

                .listener(importJobListener)

                .start(importSalesStep)

                .build();
    }

}
