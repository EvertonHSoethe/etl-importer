package io.github.evertonsoethe.etlimporter.integration;

import io.github.evertonsoethe.etlimporter.domain.ImportExecution;
import io.github.evertonsoethe.etlimporter.enums.ExecutionStatusEnum;
import io.github.evertonsoethe.etlimporter.repository.ImportErrorRepository;
import io.github.evertonsoethe.etlimporter.repository.ImportExecutionRepository;
import io.github.evertonsoethe.etlimporter.repository.SaleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
@Testcontainers
class ImportJobIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.docker.compose.enabled", () -> "false");
    }

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private Job importSalesJob;

    @Autowired
    private ImportExecutionRepository importExecutionRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ImportErrorRepository importErrorRepository;

    @Test
    void shouldProcessCsvAndUpdateCountersCorrectly() throws Exception {
        // Given: test CSV has 5 valid lines and 2 invalid lines
        int validLines = 5;
        int invalidLines = 2;

        // When
        JobExecution jobExecution = jobOperator.run(
                importSalesJob,
                new JobParametersBuilder()
                        .addLong("timestamp", System.currentTimeMillis())
                        .toJobParameters()
        );

        // Then
        long executionId = jobExecution.getExecutionContext().getLong("executionId");
        ImportExecution importExecution = importExecutionRepository.findById(executionId)
                .orElseThrow();

        // Property 6: Corretude dos contadores da execução
        assertEquals(ExecutionStatusEnum.COMPLETED, importExecution.getStatus());
        assertEquals(validLines, importExecution.getSuccessLines());
        assertEquals(invalidLines, importExecution.getErrorLines());
        assertEquals(validLines + invalidLines, importExecution.getTotalLines());
        assertNotNull(importExecution.getFinishedDate());
        assertNotNull(importExecution.getDuration());
        assertTrue(importExecution.getDuration() > 0);

        // Property 5: Persistência completa de chunks pelo Writer
        assertEquals(validLines, saleRepository.count());

        // Property 7: Completude e corretude dos registros de erro por linha
        assertEquals(invalidLines, importErrorRepository.count());
    }
}
