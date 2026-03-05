package gift.option.exception;

import gift.common.BaseErrorCode;
import org.springframework.http.HttpStatus;

public enum OptionErrorCode implements BaseErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found."),
    OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Option not found."),
    DUPLICATE_OPTION_NAME(HttpStatus.BAD_REQUEST, "이미 존재하는 옵션명입니다."),
    CANNOT_DELETE_LAST_OPTION(HttpStatus.BAD_REQUEST, "옵션이 1개인 상품은 옵션을 삭제할 수 없습니다."),
    INSUFFICIENT_OPTION_QUANTITY(HttpStatus.BAD_REQUEST, "차감할 수량이 현재 재고보다 많습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    OptionErrorCode(HttpStatus httpStatus, String message) {
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
