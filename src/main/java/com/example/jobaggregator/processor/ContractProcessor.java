package com.example.jobaggregator.processor;

import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.writer.InvalidContractFileWriter;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public final class ContractProcessor implements ItemProcessor<Contract, Contract> {

    private final InvalidContractFileWriter invalidWriter;

    public ContractProcessor(InvalidContractFileWriter invalidWriter) {
        this.invalidWriter = invalidWriter;
    }

    @Override
    public Contract process(Contract item) throws Exception {
        if (!isValid(item)) {
            invalidWriter.write(item);
            return null;
        }
        return item;
    }

    private boolean isValid(Contract contract) {
        long accCount = contract.lines().stream()
                .filter(line -> line.type().name().equals("ACC"))
                .count();

        if (accCount < 1) {
            return false;
        }

        long omCount = contract.lines().stream()
                .filter(line -> line.type().name().equals("OM"))
                .count();

        if (omCount < 1) {
            return false;
        }

        long artCount = contract.lines().stream()
                .filter(line -> line.type().name().equals("ART_N"))
                .count();

        if (artCount < 1) {
            return false;
        }

        return true;
    }
}
