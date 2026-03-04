package gift.error;

import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements ErrorCode {
    INVALID_REQUEST("INVALID_REQUEST", "입력값이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    CommonErrorCode(final String code, final String message, final HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
