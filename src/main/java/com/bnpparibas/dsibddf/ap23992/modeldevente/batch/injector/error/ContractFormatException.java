package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.error;

/**
 * Thrown when a contract input record fails format or structural validation.
 *
 * <p>Exposes structured fields for programmatic error handling in addition
 * to the human-readable message.
 */
public final class ContractFormatException extends RuntimeException {

    private final long lineNumber;
    private final String contractId;
    private final String reason;

    public ContractFormatException(long lineNumber, String contractId, String reason) {
        super(formatMessage(lineNumber, contractId, reason));
        this.lineNumber = lineNumber;
        this.contractId = contractId;
        this.reason = reason;
    }

    public ContractFormatException(long lineNumber, String contractId, String reason, Throwable cause) {
        super(formatMessage(lineNumber, contractId, reason), cause);
        this.lineNumber = lineNumber;
        this.contractId = contractId;
        this.reason = reason;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public String getContractId() {
        return contractId;
    }

    public String getReason() {
        return reason;
    }

    private static String formatMessage(long lineNumber, String contractId, String reason) {
        return "Invalid contract input: line=" + lineNumber
                + ", contractId=" + (contractId == null ? "<unknown>" : contractId)
                + ", reason=" + reason;
    }
}
