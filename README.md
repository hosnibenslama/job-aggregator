# Partitioned Contract Import

This module implements a restartable, parallel import of large contract files using Spring Batch. It splits the input file at `CTR` boundaries, processes each partition in parallel, and persists contracts and their lines to PostgreSQL.

## Design

- **Step 1 — Splitter tasklet**: Scans the source file once, creates partition files only before `CTR` lines, copies `HDR` to each partition, and appends `TRL` to each partition.
- **Step 2 — Parallel worker step**: Each worker reads one partition file, assembles complete contracts, and writes them to the database.
- **Contract assembly**: `ContractBuilder` enforces the allowed sequence of line types and prerequisites (e.g., `IKAC`/`COND`/`TAR`/`AVT` require a preceding `ART;N`).
- **Restartability**: The reader tracks the number of physical lines read and resumes from the last committed offset.

## Running the job

### Prerequisites

- PostgreSQL 14+ with database `job_aggregator` and user `job_aggregator`.
- Java 21+ and Maven.
- Input file at the path configured in `application.yml` (default: `/data/input/contracts.dat`).

### Local run

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Production run

1. Ensure `application-prod.yml` overrides:
   - `spring.datasource.url`, `username`, `password`
   - `contract.import.input-file` and `partition-directory`
   - `contract.import.requested-partitions` (match CPU cores / IO capacity)
2. Run the job with the `prod` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

## Tuning

- **Partitions**: Start with `requested-partitions` equal to the number of CPU cores. Increase if the job is IO-bound and storage can sustain more parallel readers.
- **Chunk size**: Default is 100 contracts per chunk. Increase for larger contracts or lower per-contract overhead; decrease if memory pressure is observed.
- **Thread pool**: The worker step uses a fixed-size executor matching `requested-partitions`. Ensure the database can handle the concurrent load.

## Idempotency and restarts

- The job is idempotent at the contract level: re-running the job will fail on duplicate `contract_id` due to the unique constraint on `contracts.contract_id`.
- To re-import, either:
  - Truncate `contract_lines` and `contracts` tables, or
  - Use a different input file / contract identifiers.
- On failure, Spring Batch will restart from the last committed chunk in each partition. The reader resumes from the stored line offset.

## Production checklist

- [ ] Set strong credentials and restrict network access to the database.
- [ ] Configure `contract.import.requested-partitions` based on load tests.
- [ ] Monitor database connections, CPU, and IO wait during the first runs.
- [ ] Ensure the partition directory is on fast storage (NVMe or equivalent).
- [ ] Set up log aggregation and alerting on job failures.

## Troubleshooting

- **"HDR must appear exactly once"**: The input file is missing `HDR` or has multiple `HDR` lines.
- **"TRL must appear once"**: The input file is missing `TRL` or has multiple `TRL` lines.
- **"No CTR blocks found"**: The input file contains no `CTR` lines; verify the file format.
- **Unique constraint violation on `contract_id`**: The contract was already imported; truncate tables or use a different input file.
