package gift.option;

import gift.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum OptionErrorCode implements ErrorCode {
    OPTION_NOT_FOUND("OPTION_NOT_FOUND", "옵션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_OPTION_NAME(
        "DUPLICATE_OPTION_NAME", "이미 존재하는 옵션명입니다.", HttpStatus.CONFLICT),
    CANNOT_DELETE_LAST_OPTION(
        "CANNOT_DELETE_LAST_OPTION",
        "옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.",
        HttpStatus.BAD_REQUEST),
    INSUFFICIENT_STOCK(
        "INSUFFICIENT_STOCK", "차감할 수량이 현재 재고보다 많습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    OptionErrorCode(final String code, final String message, final HttpStatus httpStatus) {
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
