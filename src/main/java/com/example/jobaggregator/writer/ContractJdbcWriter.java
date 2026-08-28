package com.example.jobaggregator.writer;

import com.example.jobaggregator.domain.BusinessLine;
import com.example.jobaggregator.domain.Contract;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

@Component
public final class ContractJdbcWriter implements ItemWriter<Contract> {

    private final JdbcTemplate jdbcTemplate;

    public ContractJdbcWriter(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void write(Chunk<? extends Contract> items) throws Exception {
        for (Contract contract : items) {
            long contractId = insertContract(contract);
            insertLines(contractId, contract.lines());
        }
    }

    private long insertContract(Contract contract) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO contracts (contract_id, first_line, last_line)
                    VALUES (?, ?, ?)
                    """,
                    new String[] {"id"});
            ps.setString(1, contract.contractId());
            ps.setLong(2, contract.firstPhysicalLine());
            ps.setLong(3, contract.lastPhysicalLine());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to retrieve generated id for contract");
        }
        return key.longValue();
    }

    private void insertLines(long contractId, List<BusinessLine> lines) {
        jdbcTemplate.batchUpdate(
                """
                INSERT INTO contract_lines (contract_id, line_number, line_type, raw_line)
                VALUES (?, ?, ?, ?)
                """,
                lines,
                lines.size(),
                (ps, line) -> {
                    ps.setLong(1, contractId);
                    ps.setLong(2, line.lineNumber());
                    ps.setString(3, line.type().name());
                    ps.setString(4, line.raw());
                });
    }
}
