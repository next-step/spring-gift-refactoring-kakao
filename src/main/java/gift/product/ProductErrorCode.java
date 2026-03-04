package gift.product;

import gift.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ProductErrorCode implements ErrorCode {
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ProductErrorCode(final String code, final String message, final HttpStatus httpStatus) {
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
