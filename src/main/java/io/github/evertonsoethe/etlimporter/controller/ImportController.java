package io.github.evertonsoethe.etlimporter.controller;

import io.github.evertonsoethe.etlimporter.domain.ImportExecution;
import io.github.evertonsoethe.etlimporter.repository.ImportExecutionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Importação", description = "Endpoints para gerenciamento de importações de vendas")
public class ImportController {

    private final JobOperator jobOperator;
    private final Job importSalesJob;
    private final ImportExecutionRepository importExecutionRepository;

    @Operation(
        summary = "Iniciar importação de vendas",
        description = "Lança um job batch para importar dados de vendas a partir do arquivo CSV."
    )
    @ApiResponse(responseCode = "202", description = "Job de importação iniciado com sucesso",
        content = @Content(schema = @Schema(implementation = ImportExecution.class)))
    @ApiResponse(responseCode = "409", description = "Já existe um job de importação em execução",
        content = @Content(schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "500", description = "Erro interno ao iniciar o job de importação",
        content = @Content(schema = @Schema(implementation = String.class)))
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

    @Operation(
        summary = "Consultar status de importação",
        description = "Retorna o status e progresso de uma execução de importação pelo ID."
    )
    @ApiResponse(responseCode = "200", description = "Execução encontrada",
        content = @Content(schema = @Schema(implementation = ImportExecution.class)))
    @ApiResponse(responseCode = "404", description = "Execução não encontrada")
    @GetMapping("/{id}")
    public ResponseEntity<ImportExecution> getStatus(@PathVariable Long id) {
        return importExecutionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
