package gift.order;

import gift.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "주문을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    OPTION_NOT_FOUND(
        "ORDER_OPTION_NOT_FOUND", "주문 대상 옵션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    OrderErrorCode(final String code, final String message, final HttpStatus httpStatus) {
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
