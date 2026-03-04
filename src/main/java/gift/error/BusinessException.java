package gift.error;

public abstract class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    protected BusinessException(final ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
