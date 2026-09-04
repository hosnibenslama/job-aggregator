package com.example.jobaggregator.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.domain.ParsedLine;
import com.example.jobaggregator.writer.ContractRejectWriter;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContractStructureValidatorTest {

    @Mock
    private ContractRejectWriter rejectWriter;

    @InjectMocks
    private ContractStructureValidator validator;

    @Test
    void validContractPassesThrough() throws Exception {
        Contract contract = new Contract(List.of(
                line(1, LineType.CTR, "CTR"),
                line(2, LineType.ACC, "ACC", "BILL"),
                line(3, LineType.OM, "OM", "OM-001"),
                line(4, LineType.ART, "ART", "1")
        ));

        Contract result = validator.process(contract);

        assertThat(result).isSameAs(contract);
        verifyNoInteractions(rejectWriter);
    }

    @Test
    void invalidContractReturnsNullAndCallsRejectWriter() throws Exception {
        Contract contract = new Contract(List.of(
                line(1, LineType.CTR, "CTR"),
                line(2, LineType.ACC, "ACC", "BILL")
        ));

        Contract result = validator.process(contract);

        assertThat(result).isNull();
        verify(rejectWriter).reject(eq(contract), any(String.class));
    }

    @Test
    void rejectWriterCalledWithCorrectReason() throws Exception {
        Contract contract = new Contract(List.of(
                line(1, LineType.CTR, "CTR"),
                line(2, LineType.ACC, "ACC", "BILL"),
                line(3, LineType.OM, "OM", "OM-001")
        ));

        validator.process(contract);

        verify(rejectWriter).reject(eq(contract), eq("Invalid contract input: line=1, contractId=<unknown>, reason=A contract must contain at least one ART"));
    }

    @Test
    void ioExceptionFromRejectWriterPropagatesDirectly() throws Exception {
        Contract contract = new Contract(List.of(
                line(1, LineType.CTR, "CTR"),
                line(2, LineType.ACC, "ACC", "BILL")
        ));

        doThrow(new IOException("Disk full")).when(rejectWriter).reject(any(Contract.class), any(String.class));

        assertThatThrownBy(() -> validator.process(contract))
                .isInstanceOf(IOException.class)
                .hasMessage("Disk full");
    }

    @Test
    void invalidSequenceRejectsContract() throws Exception {
        Contract contract = new Contract(List.of(
                line(1, LineType.CTR, "CTR"),
                line(2, LineType.ACC, "ACC", "BILL"),
                line(3, LineType.OM, "OM", "OM-001"),
                line(4, LineType.IKAC, "IKAC", "value")
        ));

        Contract result = validator.process(contract);

        assertThat(result).isNull();
        verify(rejectWriter).reject(eq(contract), any(String.class));
    }

    @Test
    void unknownLineTypeRejectsContract() throws Exception {
        Contract contract = new Contract(List.of(
                line(1, LineType.UNKNOWN, "CTTR"),
                line(2, LineType.ACC, "ACC", "BILL"),
                line(3, LineType.OM, "OM", "OM-001"),
                line(4, LineType.ART, "ART", "1")
        ));

        Contract result = validator.process(contract);

        assertThat(result).isNull();
        verify(rejectWriter).reject(eq(contract), any(String.class));
    }

    private ParsedLine line(long number, LineType type, String... fields) {
        return new ParsedLine(number, type, String.join(";", fields), List.of(fields));
    }
}
