package gift.member.internal;

import gift.global.BadRequestException;

public class RegisterFailedException extends BadRequestException {

    public RegisterFailedException(String responseMessage) {
        super(responseMessage);
    }

    public static RegisterFailedException byRegisteredEmail() {
        return new RegisterFailedException(
                "Email is already registered."
        );
    }
}
