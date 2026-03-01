package gift.global;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<String> handleCustomException(CustomException e) {

        HttpStatus httpStatus = e.getHttpStatus();
        String message = e.getResponseMessage();

        return ResponseEntity
                .status(httpStatus)
                .body(message);
    }
}
