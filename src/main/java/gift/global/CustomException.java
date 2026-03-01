package gift.global;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class CustomException extends RuntimeException {

    private final HttpStatus httpStatus;

    private final String responseMessage;

    protected CustomException(HttpStatus httpStatus, String responseMessage) {
        this(
                httpStatus, responseMessage, null
        );
    }

    protected CustomException(HttpStatus httpStatus, String responseMessage, String message) {
        this(
                httpStatus, responseMessage, message, null
        );
    }

    protected CustomException(
            HttpStatus httpStatus, String responseMessage,
            String message, Throwable cause
    ) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.responseMessage = responseMessage;
    }
}
