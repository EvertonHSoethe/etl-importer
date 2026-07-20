package io.github.evertonsoethe.etlimporter.controller;

import io.github.evertonsoethe.etlimporter.domain.ImportExecution;
import io.github.evertonsoethe.etlimporter.repository.ImportExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/import")
@RequiredArgsConstructor
public class ImportController {

    private final JobOperator jobOperator;
    private final Job importSalesJob;
    private final ImportExecutionRepository importExecutionRepository;

    @PostMapping
    public ResponseEntity<?> importData() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution jobExecution = jobOperator.run(importSalesJob, jobParameters);

            long executionId = jobExecution.getExecutionContext().getLong("executionId");
            ImportExecution importExecution = importExecutionRepository.findById(executionId)
                    .orElseThrow(() -> new IllegalStateException("ImportExecution not found after job launch"));

            return ResponseEntity.accepted().body(importExecution);

        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("Import job already running", e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Um job de importação já está em execução.");
        } catch (Exception e) {
            log.error("Failed to launch import job", e);
            return ResponseEntity.internalServerError()
                    .body("Falha ao iniciar o job de importação: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImportExecution> getStatus(@PathVariable Long id) {
        return importExecutionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
