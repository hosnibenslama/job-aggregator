package com.example.jobaggregator.error;

public final class ContractFormatException extends RuntimeException {

    public ContractFormatException(long lineNumber, String contractId, String reason) {
        super("Invalid contract input: line=" + lineNumber
                + ", contractId=" + (contractId == null ? "<unknown>" : contractId)
                + ", reason=" + reason);
    }

    public ContractFormatException(long lineNumber, String contractId, String reason, Throwable cause) {
        super("Invalid contract input: line=" + lineNumber
                + ", contractId=" + (contractId == null ? "<unknown>" : contractId)
                + ", reason=" + reason, cause);
    }
}
