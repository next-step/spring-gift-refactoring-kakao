package gift.wish;

import gift.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum WishErrorCode implements ErrorCode {
    WISH_NOT_FOUND("WISH_NOT_FOUND", "위시를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND(
        "WISH_PRODUCT_NOT_FOUND", "위시 대상 상품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    WishErrorCode(final String code, final String message, final HttpStatus httpStatus) {
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
