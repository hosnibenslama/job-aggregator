package com.example.jobaggregator.partition;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;

public class GeneratedFilePartitioner implements Partitioner {

    private final String partitionFiles;

    public GeneratedFilePartitioner(String partitionFiles) {
        this.partitionFiles = partitionFiles;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        if (partitionFiles == null || partitionFiles.isBlank()) {
            throw new IllegalStateException("No generated partition files found in the job execution context");
        }

        Map<String, ExecutionContext> result = new LinkedHashMap<>();
        String[] files = partitionFiles.split("\\|", -1);

        for (int index = 0; index < files.length; index++) {
            ExecutionContext context = new ExecutionContext();
            context.putString("partitionFile", files[index]);
            context.putInt("partitionIndex", index);
            result.put("contract-partition-%05d".formatted(index), context);
        }

        return result;
    }
}
