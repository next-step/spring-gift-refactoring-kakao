package gift.common;

import gift.category.exception.CategoryErrorCode;
import gift.category.exception.CategoryException;
import gift.option.exception.OptionErrorCode;
import gift.option.exception.OptionException;
import gift.order.exception.OrderErrorCode;
import gift.order.exception.OrderException;
import gift.product.exception.ProductErrorCode;
import gift.product.exception.ProductException;
import gift.wish.exception.WishErrorCode;
import gift.wish.exception.WishException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CategoryException.class)
    public ResponseEntity<ErrorResponse> handleCategoryException(CategoryException e) {
        CategoryErrorCode errorCode = (CategoryErrorCode) e.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
            .body(new ErrorResponse(errorCode.getHttpStatus().value(), errorCode.getMessage()));
    }

    @ExceptionHandler(ProductException.class)
    public ResponseEntity<ErrorResponse> handleProductException(ProductException e) {
        ProductErrorCode errorCode = (ProductErrorCode) e.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
            .body(new ErrorResponse(errorCode.getHttpStatus().value(), errorCode.getMessage()));
    }

    @ExceptionHandler(OptionException.class)
    public ResponseEntity<ErrorResponse> handleOptionException(OptionException e) {
        OptionErrorCode errorCode = (OptionErrorCode) e.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
            .body(new ErrorResponse(errorCode.getHttpStatus().value(), errorCode.getMessage()));
    }

    @ExceptionHandler(OrderException.class)
    public ResponseEntity<ErrorResponse> handleOrderException(OrderException e) {
        OrderErrorCode errorCode = (OrderErrorCode) e.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
            .body(new ErrorResponse(errorCode.getHttpStatus().value(), errorCode.getMessage()));
    }

    @ExceptionHandler(WishException.class)
    public ResponseEntity<ErrorResponse> handleWishException(WishException e) {
        WishErrorCode errorCode = (WishErrorCode) e.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
            .body(new ErrorResponse(errorCode.getHttpStatus().value(), errorCode.getMessage()));
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        BaseErrorCode errorCode = e.getErrorCode();
        HttpStatus status = errorCode.getHttpStatus();
        return ResponseEntity.status(status)
            .body(new ErrorResponse(status.value(), errorCode.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null ? "잘못된 요청입니다." : fieldError.getDefaultMessage();
        return ResponseEntity.badRequest()
            .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElement(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류가 발생했습니다."));
    }
}
