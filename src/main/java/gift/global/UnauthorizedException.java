package gift.global;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends CustomException {

    private static final String DEFAULT_MESSAGE = "Unauthorized request";

    public UnauthorizedException() {
        this(DEFAULT_MESSAGE);
    }

    public UnauthorizedException(String responseMessage) {
        super(HttpStatus.UNAUTHORIZED, responseMessage);
    }
}
