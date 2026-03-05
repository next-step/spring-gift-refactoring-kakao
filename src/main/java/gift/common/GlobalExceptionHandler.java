package gift.common;

import gift.product.ProductErrorCode;
import gift.product.ProductException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ProductException.class)
    public ResponseEntity<?> handleProductException(ProductException e) {
        ProductErrorCode errorCode = (ProductErrorCode) e.getErrorCode();
        if (errorCode == ProductErrorCode.INVALID_PRODUCT_NAME) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        if (errorCode == ProductErrorCode.PRODUCT_NOT_FOUND || errorCode == ProductErrorCode.CATEGORY_NOT_FOUND) {
            return ResponseEntity.notFound().build();
        }
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
