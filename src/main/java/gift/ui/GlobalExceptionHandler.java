package gift.ui;

import gift.auth.UnauthorizedException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final String ERROR_MESSAGE_KEY = "message";

  @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, String> handleBadRequest(RuntimeException e) {
    return Map.of(ERROR_MESSAGE_KEY, Objects.toString(e.getMessage(), "Bad Request"));
  }

  @ExceptionHandler(NoSuchElementException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Map<String, String> handleNoSuchElement(NoSuchElementException e) {
    return Map.of(ERROR_MESSAGE_KEY, Objects.toString(e.getMessage(), "Not Found"));
  }

  @ExceptionHandler(UnauthorizedException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public Map<String, String> handleUnauthorized(UnauthorizedException e) {
    return Map.of(ERROR_MESSAGE_KEY, Objects.toString(e.getMessage(), "Unauthorized"));
  }
}
