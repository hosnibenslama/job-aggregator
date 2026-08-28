package com.example.jobaggregator.writer;

import com.example.jobaggregator.domain.BusinessLine;
import com.example.jobaggregator.domain.Contract;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.batch.item.database.BatchItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public final class ContractJdbcWriter extends BatchItemWriter<Contract> {

    private final JdbcTemplate jdbcTemplate;

    public ContractJdbcWriter(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void write(List<? extends Contract> items) throws Exception {
        for (Contract contract : items) {
            long contractId = insertContract(contract);
            insertLines(contractId, contract.lines());
        }
    }

    private long insertContract(Contract contract) {
        jdbcTemplate.update(
                """
                INSERT INTO contracts (contract_id, first_line, last_line)
                VALUES (?, ?, ?)
                """,
                contract.contractId(),
                contract.firstPhysicalLine(),
                contract.lastPhysicalLine());

        return jdbcTemplate.queryForObject(
                "SELECT id FROM contracts WHERE contract_id = ?",
                Long.class,
                contract.contractId());
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
