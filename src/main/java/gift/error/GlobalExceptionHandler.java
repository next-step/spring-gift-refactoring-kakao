package gift.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(final BusinessException e) {
        final ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(ErrorResponse.from(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        final MethodArgumentNotValidException e
    ) {
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.from(CommonErrorCode.INVALID_REQUEST));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(
        final MissingRequestHeaderException e
    ) {
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.from(CommonErrorCode.INVALID_REQUEST));
    }
}
