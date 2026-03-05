package gift.wish.exception;

import gift.common.BaseErrorCode;
import org.springframework.http.HttpStatus;

public enum WishErrorCode implements BaseErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found."),
    WISH_NOT_FOUND(HttpStatus.NOT_FOUND, "Wish not found.");

    private final HttpStatus httpStatus;
    private final String message;

    WishErrorCode(HttpStatus httpStatus, String message) {
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
