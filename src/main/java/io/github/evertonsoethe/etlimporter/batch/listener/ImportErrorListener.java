package io.github.evertonsoethe.etlimporter.batch.listener;

import io.github.evertonsoethe.etlimporter.batch.dto.SaleCsvDto;
import io.github.evertonsoethe.etlimporter.domain.ImportError;
import io.github.evertonsoethe.etlimporter.domain.ImportExecution;
import io.github.evertonsoethe.etlimporter.domain.Sale;
import io.github.evertonsoethe.etlimporter.repository.ImportErrorRepository;
import io.github.evertonsoethe.etlimporter.repository.ImportExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class ImportErrorListener implements SkipListener<SaleCsvDto, Sale>, StepExecutionListener {

    private static final int FLUSH_THRESHOLD = 1000;

    private final ImportExecutionRepository importExecutionRepository;
    private final ImportErrorRepository importErrorRepository;

    private final List<ImportError> errorBuffer = new ArrayList<>();
    private StepExecution stepExecution;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

    @Override
    public void onSkipInRead(Throwable t) {
        // Not handled — read errors are not expected with FlatFileItemReader
    }

    @Override
    public void onSkipInWrite(Sale item, Throwable t) {
        // Not handled — write errors would rollback the chunk
    }

    @Override
    public void onSkipInProcess(SaleCsvDto item, Throwable t) {
        long executionId = stepExecution.getJobExecution()
                .getExecutionContext().getLong("executionId");

        ImportExecution execution = importExecutionRepository.getReferenceById(executionId);

        ImportError error = ImportError.builder()
                .execution(execution)
                .lineNumber(stepExecution.getReadCount())
                .errorMessage(t.getMessage())
                .rawData(item.toString())
                .build();

        errorBuffer.add(error);

        if (errorBuffer.size() >= FLUSH_THRESHOLD) {
            flush();
        }

        log.debug("Buffered import error at line {} - {}", error.getLineNumber(), t.getMessage());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        flush();
        return null;
    }

    private void flush() {
        if (!errorBuffer.isEmpty()) {
            int count = errorBuffer.size();
            importErrorRepository.saveAll(new ArrayList<>(errorBuffer));
            errorBuffer.clear();
            log.debug("Flushed {} import errors to database", count);
        }
    }
}
