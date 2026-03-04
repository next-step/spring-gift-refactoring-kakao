package gift.ui;

import gift.infrastructure.kakao.KakaoMessageException;
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

  @ExceptionHandler(IllegalStateException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, String> handleIllegalState(IllegalStateException e) {
    return Map.of(ERROR_MESSAGE_KEY, Objects.toString(e.getMessage(), "Bad Request"));
  }

  @ExceptionHandler(NoSuchElementException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Map<String, String> handleNoSuchElement(NoSuchElementException e) {
    return Map.of(ERROR_MESSAGE_KEY, Objects.toString(e.getMessage(), "Bad Request"));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, String> handleIllegalArgument(IllegalArgumentException e) {
    return Map.of(ERROR_MESSAGE_KEY, Objects.toString(e.getMessage(), "Bad Request"));
  }

  @ExceptionHandler(KakaoMessageException.class)
  @ResponseStatus(HttpStatus.BAD_GATEWAY)
  public Map<String, String> handleKakaoMessage(KakaoMessageException e) {
    return Map.of(ERROR_MESSAGE_KEY, Objects.toString(e.getMessage(), "카카오 메시지 전송 실패"));
  }
}
