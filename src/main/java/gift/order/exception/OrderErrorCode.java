package gift.order.exception;

import gift.common.BaseErrorCode;
import org.springframework.http.HttpStatus;

public enum OrderErrorCode implements BaseErrorCode {
    OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Option not found.");

    private final HttpStatus httpStatus;
    private final String message;

    OrderErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
