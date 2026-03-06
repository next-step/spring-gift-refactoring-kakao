package gift.{domain};

import gift.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum {Domain}ErrorCode implements ErrorCode {
    {DOMAIN}_NOT_FOUND("{DOMAIN}_NOT_FOUND", "{도메인}을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    {Domain}ErrorCode(final String code, final String message, final HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getCode() { return code; }
    @Override
    public String getMessage() { return message; }
    @Override
    public HttpStatus getHttpStatus() { return httpStatus; }
}
