package gift.error;

public record ErrorResponse(String code, String message) {
    public static ErrorResponse from(final ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
    }
}
