package io.github.evertonsoethe.etlimporter.batch.listener;

import io.github.evertonsoethe.etlimporter.batch.dto.SaleCsvDto;
import io.github.evertonsoethe.etlimporter.domain.ImportError;
import io.github.evertonsoethe.etlimporter.domain.ImportExecution;
import io.github.evertonsoethe.etlimporter.domain.Sale;
import io.github.evertonsoethe.etlimporter.repository.ImportErrorRepository;
import io.github.evertonsoethe.etlimporter.repository.ImportExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImportErrorListener implements SkipListener<SaleCsvDto, Sale>, StepExecutionListener {

    private final ImportExecutionRepository importExecutionRepository;
    private final ImportErrorRepository importErrorRepository;

    private StepExecution stepExecution;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

    @Override
    public void onSkipInRead(Throwable t) {
        // Not handled — read errors are not expected with FlatFileItemReader in this scenario
    }

    @Override
    public void onSkipInWrite(Sale item, Throwable t) {
        // Not handled — write errors would rollback the chunk
    }

    @Override
    public void onSkipInProcess(SaleCsvDto item, Throwable t) {
        long executionId = stepExecution.getJobExecution().getExecutionContext().getLong("executionId");

        ImportExecution execution = importExecutionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalStateException("ImportExecution not found: " + executionId));

        ImportError error = ImportError.builder()
                .execution(execution)
                .lineNumber(stepExecution.getReadCount())
                .errorMessage(t.getMessage())
                .rawData(item.toString())
                .build();

        importErrorRepository.save(error);

        log.debug("Registered import error at line {} - {}", error.getLineNumber(), t.getMessage());
    }
}
