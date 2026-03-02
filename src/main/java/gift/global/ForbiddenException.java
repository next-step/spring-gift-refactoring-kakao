package gift.global;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends CustomException {

    private static final String DEFAULT_MESSAGE = "Forbidden request";

    public ForbiddenException() {
        this(DEFAULT_MESSAGE);
    }

    public ForbiddenException(String responseMessage) {
        super(HttpStatus.FORBIDDEN, responseMessage);
    }
}
