package gift.wish;

import gift.error.BusinessException;

public class WishException extends BusinessException {
    public WishException(final WishErrorCode errorCode) {
        super(errorCode);
    }
}
