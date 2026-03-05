package gift.auth;

import gift.common.BaseException;

public class InvalidOAuthProviderException extends BaseException {
    public InvalidOAuthProviderException() {
        super(AuthErrorCode.INVALID_OAUTH_PROVIDER);
    }
}
