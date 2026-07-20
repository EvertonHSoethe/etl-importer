package io.github.evertonsoethe.etlimporter.batch.listener;

import io.github.evertonsoethe.etlimporter.domain.ImportExecution;
import io.github.evertonsoethe.etlimporter.enums.ExecutionStatusEnum;
import io.github.evertonsoethe.etlimporter.repository.ImportExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImportJobListener implements JobExecutionListener {

    private final ImportExecutionRepository importExecutionRepository;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Starting import job - creating ImportExecution record");

        ImportExecution execution = ImportExecution.builder()
                .filename("sales_100000.csv")
                .status(ExecutionStatusEnum.RUNNING)
                .totalLines(0L)
                .processedLines(0L)
                .successLines(0L)
                .errorLines(0L)
                .build();

        execution = importExecutionRepository.save(execution);

        jobExecution.getExecutionContext().putLong("executionId", execution.getId());

        log.info("ImportExecution created with id={}", execution.getId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long executionId = jobExecution.getExecutionContext().getLong("executionId");

        ImportExecution execution = importExecutionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalStateException("ImportExecution not found: " + executionId));

        long writeCount = jobExecution.getStepExecutions().stream()
                .mapToLong(StepExecution::getWriteCount)
                .sum();

        long skipCount = jobExecution.getStepExecutions().stream()
                .mapToLong(StepExecution::getSkipCount)
                .sum();

        execution.setSuccessLines(writeCount);
        execution.setErrorLines(skipCount);
        execution.setTotalLines(writeCount + skipCount);
        execution.setProcessedLines(writeCount + skipCount);
        execution.setFinishedDate(LocalDateTime.now());

        // Calculate duration in milliseconds
        long startTime = jobExecution.getStartTime() != null ? jobExecution.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : 0;
        long endTime = System.currentTimeMillis();
        execution.setDuration(endTime - startTime);

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            execution.setStatus(ExecutionStatusEnum.COMPLETED);
        } else {
            execution.setStatus(ExecutionStatusEnum.FAILED);
        }

        importExecutionRepository.save(execution);

        log.info("Import job finished - status={}, successLines={}, errorLines={}, totalLines={}, duration={}ms",
                execution.getStatus(), execution.getSuccessLines(), execution.getErrorLines(),
                execution.getTotalLines(), execution.getDuration());
    }
}
