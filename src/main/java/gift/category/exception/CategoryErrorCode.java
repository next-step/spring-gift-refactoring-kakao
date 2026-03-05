package gift.category.exception;

import gift.common.BaseErrorCode;
import org.springframework.http.HttpStatus;

public enum CategoryErrorCode implements BaseErrorCode {
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Category not found.");

    private final HttpStatus httpStatus;
    private final String message;

    CategoryErrorCode(HttpStatus httpStatus, String message) {
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
