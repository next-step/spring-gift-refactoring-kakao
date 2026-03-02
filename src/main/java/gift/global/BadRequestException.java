package gift.global;

import org.springframework.http.HttpStatus;

public class BadRequestException extends CustomException {

    public BadRequestException(String responseMessage) {
        super(HttpStatus.BAD_REQUEST, responseMessage);
    }
}
