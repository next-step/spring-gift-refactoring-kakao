package gift.wish;

import gift.common.BaseException;

public class WishException extends BaseException {
    public WishException(WishErrorCode errorCode) {
        super(errorCode);
    }
}
