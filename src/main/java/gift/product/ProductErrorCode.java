package gift.product;

import gift.common.BaseErrorCode;
import org.springframework.http.HttpStatus;

public enum ProductErrorCode implements BaseErrorCode {
    INVALID_PRODUCT_NAME(HttpStatus.BAD_REQUEST, "상품명이 유효하지 않습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Category not found.");

    private final HttpStatus httpStatus;
    private final String message;

    ProductErrorCode(HttpStatus httpStatus, String message) {
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
