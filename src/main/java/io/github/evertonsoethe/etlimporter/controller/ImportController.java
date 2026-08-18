package io.github.evertonsoethe.etlimporter.controller;

import io.github.evertonsoethe.etlimporter.domain.ImportExecution;
import io.github.evertonsoethe.etlimporter.repository.ImportExecutionRepository;
import io.github.evertonsoethe.etlimporter.service.CsvValidationService;
import io.github.evertonsoethe.etlimporter.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping("/import")
@RequiredArgsConstructor
@Tag(name = "Import", description = "Endpoints for managing sales data imports")
@SecurityRequirement(name = "basicAuth")
public class ImportController {

    private final JobOperator jobOperator;
    private final Job importSalesJob;
    private final ImportExecutionRepository importExecutionRepository;
    private final CsvValidationService csvValidationService;
    private final FileStorageService fileStorageService;

    @Operation(
        summary = "Start sales import",
        description = "Receives a CSV file via multipart, validates, stores and starts the import job asynchronously."
    )
    @ApiResponse(responseCode = "202", description = "Import job started successfully",
        content = @Content(schema = @Schema(implementation = ImportExecution.class)))
    @ApiResponse(responseCode = "400", description = "Invalid file (missing, wrong extension or empty)",
        content = @Content(schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - valid credentials required")
    @ApiResponse(responseCode = "409", description = "An import job is already running",
        content = @Content(schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "413", description = "File exceeds the maximum allowed size",
        content = @Content(schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "422", description = "CSV header does not match the expected schema",
        content = @Content(schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "500", description = "Internal error while processing the upload",
        content = @Content(schema = @Schema(implementation = String.class)))
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importData(@RequestParam("file") MultipartFile file) {
        csvValidationService.validate(file);

        Path storedPath = fileStorageService.store(file);

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("filePath", storedPath.toString())
                    .addString("originalFilename", file.getOriginalFilename())
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
                    .body("An import job is already running.");
        } catch (Exception e) {
            log.error("Failed to launch import job", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to start the import job: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Query import status",
        description = "Returns the status and progress of an import execution by ID."
    )
    @ApiResponse(responseCode = "200", description = "Execution found",
        content = @Content(schema = @Schema(implementation = ImportExecution.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - valid credentials required")
    @ApiResponse(responseCode = "404", description = "Execution not found")
    @GetMapping("/{id}")
    public ResponseEntity<ImportExecution> getStatus(@PathVariable Long id) {
        return importExecutionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
