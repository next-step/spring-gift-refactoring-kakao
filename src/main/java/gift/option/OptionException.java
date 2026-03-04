package gift.option;

import gift.error.BusinessException;

public class OptionException extends BusinessException {
    public OptionException(final OptionErrorCode errorCode) {
        super(errorCode);
    }
}
