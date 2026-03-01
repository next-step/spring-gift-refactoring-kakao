package gift.global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {

        Map<String, List<String>> errors = new HashMap<>();

        e.getBindingResult().getFieldErrors().forEach(fieldError -> {
            String field = fieldError.getField();
            String message = fieldError.getDefaultMessage();

            if (!errors.containsKey(field)) {
                errors.put(field, new ArrayList<>());
            }

            errors.get(field).add(message);
        });

        ValidationErrorResponse response = new ValidationErrorResponse(
                "Bad request: validation failed",
                errors
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }
}
