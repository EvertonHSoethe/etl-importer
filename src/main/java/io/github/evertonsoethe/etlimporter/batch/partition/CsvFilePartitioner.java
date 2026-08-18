package io.github.evertonsoethe.etlimporter.batch.partition;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class CsvFilePartitioner implements Partitioner {

    private final Resource csvResource;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        long totalDataLines = countDataLines();
        Map<String, ExecutionContext> partitions = new LinkedHashMap<>();

        if (totalDataLines == 0) {
            ExecutionContext ctx = new ExecutionContext();
            ctx.putLong("startLine", 2);
            ctx.putLong("endLine", 1);
            ctx.putString("partitionName", "partition-0");
            partitions.put("partition-0", ctx);
            return partitions;
        }

        long baseSize = totalDataLines / gridSize;
        long remainder = totalDataLines % gridSize;

        long currentStart = 2; // line 1 = header, data starts at line 2
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext ctx = new ExecutionContext();
            long partitionSize = baseSize + (i < remainder ? 1 : 0);
            long endLine = currentStart + partitionSize - 1;

            ctx.putLong("startLine", currentStart);
            ctx.putLong("endLine", endLine);
            ctx.putString("partitionName", "partition-" + i);
            partitions.put("partition-" + i, ctx);

            log.debug("Partition {} - lines [{}, {}] ({} lines)", i, currentStart, endLine, partitionSize);
            currentStart = endLine + 1;
        }

        log.info("Partitioned {} data lines into {} partitions", totalDataLines, gridSize);
        return partitions;
    }

    private long countDataLines() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(csvResource.getInputStream()))) {
            long count = reader.lines().count() - 1; // subtract header
            return Math.max(0, count);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to count lines in CSV resource: " + csvResource, e);
        }
    }
}
