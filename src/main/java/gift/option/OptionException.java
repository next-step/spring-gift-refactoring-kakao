package gift.option;

import gift.common.BaseException;

public class OptionException extends BaseException {
    public OptionException(OptionErrorCode errorCode) {
        super(errorCode);
    }
}
