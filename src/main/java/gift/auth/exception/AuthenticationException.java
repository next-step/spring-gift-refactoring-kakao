package gift.auth.exception;

import gift.common.BaseException;

public class AuthenticationException extends BaseException {
    public AuthenticationException() {
        super(AuthErrorCode.AUTHENTICATION_FAILED);
    }
}
