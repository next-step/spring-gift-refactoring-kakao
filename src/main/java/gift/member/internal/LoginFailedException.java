package gift.member.internal;

import gift.global.BadRequestException;

public class LoginFailedException extends BadRequestException {

    public LoginFailedException(String responseMessage) {
        super(responseMessage);
    }

    public static LoginFailedException byInvalidEmailOrPassword() {
        return new LoginFailedException(
                "Invalid email or password."
        );
    }
}
