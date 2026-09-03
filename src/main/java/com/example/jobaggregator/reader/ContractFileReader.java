package com.example.jobaggregator.reader;

import com.example.jobaggregator.domain.BusinessLine;
import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.error.ContractFormatException;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.support.SingleItemPeekableItemReader;

public class ContractFileReader implements ItemStreamReader<Contract> {

    private final SingleItemPeekableItemReader<BusinessLine> lineReader;

    public ContractFileReader(SingleItemPeekableItemReader<BusinessLine> lineReader) {
        this.lineReader = lineReader;
    }

    @Override
    public Contract read() throws Exception {
        BusinessLine line;

        while ((line = lineReader.read()) != null) {
            if (line.type() == LineType.HDR) {
                continue;
            }
            if (line.type() == LineType.TRL) {
                return null;
            }
            if (line.type() == LineType.CTR) {
                break;
            }
            throw new ContractFormatException(line.lineNumber(), null,
                    "Expected CTR at a contract boundary but found " + line.type());
        }

        if (line == null) {
            return null;
        }

        ContractBuilder builder = new ContractBuilder(line);

        while (lineReader.peek() != null) {
            LineType nextType = lineReader.peek().type();
            if (nextType == LineType.CTR || nextType == LineType.TRL) {
                break;
            }
            if (nextType == LineType.HDR) {
                throw new ContractFormatException(lineReader.peek().lineNumber(), null,
                        "HDR is not allowed inside a contract");
            }
            builder.accept(lineReader.read());
        }

        return builder.build();
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        lineReader.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        lineReader.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        lineReader.close();
    }
}
