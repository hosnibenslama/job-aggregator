package com.example.jobaggregator.reader;

import com.example.jobaggregator.domain.BusinessLine;
import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.error.ContractFormatException;
import java.util.Arrays;
import java.util.List;
import org.springframework.batch.item.file.LineMapper;

public final class BusinessLineMapper implements LineMapper<BusinessLine> {

    @Override
    public BusinessLine mapLine(String line, int lineNumber) {
        if (line == null || line.isBlank()) {
            throw new ContractFormatException(lineNumber, null, "Blank input line");
        }

        List<String> fields = Arrays.stream(line.split(";", -1))
                .map(String::strip)
                .toList();
        LineType type = LineType.from(fields.toArray(String[]::new));

        if (type == LineType.UNKNOWN) {
            throw new ContractFormatException(
                    lineNumber,
                    null,
                    "Unknown line code: " + fields.getFirst());
        }

        return new BusinessLine(lineNumber, type, line, fields);
    }
}
