package com.example.jobaggregator.processor;

import com.example.jobaggregator.domain.Contract;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public final class ContractProcessor implements ItemProcessor<Contract, Contract> {

    @Override
    public Contract process(Contract item) throws Exception {
        return item;
    }
}
