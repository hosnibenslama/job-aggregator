package com.example.jobaggregator.writer;

import com.example.jobaggregator.domain.BusinessLine;
import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.domain.CtrLine;
import java.sql.PreparedStatement;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

@Component
public class ContractJdbcWriter implements ItemWriter<Contract> {

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
        CtrLine ctr = contract.ctrLine();

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO contracts (
                        devise, state, motif, ou_distribution, ou_management,
                        address_id, business_relationship, effective_date,
                        periode_facturation, dates_facturation,
                        x_b3_trace_id, x_b3_span_id, user_id,
                        channel, media
                    ) VALUES (
                        ?, ?, ?, ?, ?,
                        ?, ?, ?,
                        ?, ?,
                        ?, ?, ?,
                        ?, ?
                    )
                    """,
                    new String[]{"id"});

            int i = 1;
            ps.setString(i++, ctr.devise());
            ps.setString(i++, ctr.state());
            ps.setString(i++, blankToNull(ctr.motif()));
            ps.setString(i++, blankToNull(ctr.ouDistribution()));
            ps.setString(i++, ctr.ouManagement());
            ps.setString(i++, blankToNull(ctr.addressId()));
            ps.setString(i++, ctr.businessRelationship());
            ps.setString(i++, blankToNull(ctr.effectiveDate()));
            ps.setString(i++, blankToNull(ctr.periodeFacturation()));
            ps.setString(i++, blankToNull(ctr.datesFacturation()));
            ps.setString(i++, ctr.xB3TraceId());
            ps.setString(i++, ctr.xB3SpanId());
            ps.setString(i++, ctr.userId());
            ps.setString(i++, ctr.channel());
            ps.setString(i++, ctr.media());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to retrieve generated id for contract with businessRelationship="
                    + ctr.businessRelationship());
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

    /** Returns null for blank or null strings, so optional columns are stored as SQL NULL. */
    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
