package gift.auth.exception;

import gift.common.BaseException;

public class ForbiddenException extends BaseException {
    public ForbiddenException() {
        super(AuthErrorCode.ACCESS_DENIED);
    }
}
